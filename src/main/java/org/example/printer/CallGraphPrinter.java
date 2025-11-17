package org.example.printer;

import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.Indexer;
import pascal.taie.util.SimpleIndexer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class CallGraphPrinter {

    private final CallGraph<CSCallSite, CSMethod> callGraph;
    private final PointerAnalysisResult ptaResult;
    private final Indexer<JMethod> methodIndexer;

    public CallGraphPrinter() {
        ptaResult = World.get().getResult(PointerAnalysis.ID);
        callGraph = ptaResult.getCSCallGraph();
        methodIndexer = new SimpleIndexer<>();
    }

    public String dotContent(JMethod entryMethod) {
        Set<String> visited = new HashSet<>();
        Set<String> addedEdges = new HashSet<>();
        StringBuilder nodesContent = new StringBuilder();
        StringBuilder edgesContent = new StringBuilder();
        StringBuilder rankContent = new StringBuilder();

        // 用于收集不同类型的方法，以便分层显示
        Map<String, Set<String>> rankGroups = new HashMap<>();
        rankGroups.put("entry", new HashSet<>());
        rankGroups.put("target", new HashSet<>());
        rankGroups.put("before", new HashSet<>());
        rankGroups.put("around", new HashSet<>());
        rankGroups.put("after", new HashSet<>());
        rankGroups.put("afterReturning", new HashSet<>());
        rankGroups.put("afterThrowing", new HashSet<>());
        rankGroups.put("utility", new HashSet<>());

        StringBuilder dotContent = new StringBuilder("digraph G {\n");
        dotContent.append("rankdir=TB;\n");  // 从上到下布局
        dotContent.append("node [color=\".3 .2 1.0\",shape=box,style=filled];\n");
        dotContent.append("edge [];\n");

        explore(entryMethod, visited, addedEdges, nodesContent, edgesContent, rankGroups);

        // 添加节点
        dotContent.append(nodesContent);

        // 添加排序约束
        dotContent.append(generateRankConstraints(rankGroups));

        // 添加边
        dotContent.append(edgesContent);

        dotContent.append("}\n");
        return dotContent.toString();
    }

    public void generateDotFile(JMethod entryMethod) throws IOException {
        String dotContent = dotContent(entryMethod);

        String directoryPath = "output/callFlows";
        Files.createDirectories(Paths.get(directoryPath));
        String fileName = String.valueOf(entryMethod.getDeclaringClass()) + '.' +
            entryMethod.getName() + '(' +
            entryMethod.getParamTypes()
                .stream()
                .map(Type::toString)
                .collect(Collectors.joining(",")) +
            ')' + ".dot";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(directoryPath + '/' + fileName))) {
            writer.write(dotContent);
        }
    }

    private void explore(JMethod currentMethod, Set<String> visited, Set<String> addedEdges,
                         StringBuilder nodesContent, StringBuilder edgesContent,
                         Map<String, Set<String>> rankGroups) {
        String currentMethodSignature = currentMethod.getSignature();
        if (visited.contains(currentMethodSignature)) {
            return;
        }
        visited.add(currentMethodSignature);

        // 获取当前方法的所有 CSMethod
        Set<CSMethod> csMethods = ptaResult.getCSCallGraph().reachableMethods()
            .filter(csMethod -> csMethod.getMethod().equals(currentMethod))
            .collect(Collectors.toSet());

        // 使用 Map 来避免重复的边，key 为 "caller->callee"
        Map<String, EdgeInfo> edgeInfoMap = new HashMap<>();

        for (CSMethod csMethod : csMethods) {
            // 方法1: 获取该方法内的所有调用点（普通调用边）
            Set<CSCallSite> callSites = callGraph.getCallSitesIn(csMethod);

            for (CSCallSite callSite : callSites) {
                // 获取每个调用点的所有出边
                callGraph.edgesOutOf(callSite).forEach(edge -> {
                    JMethod callee = edge.getCallee().getMethod();

                    // 判断是否为AOP边
                    boolean isAOPEdge = isAOPEdge(edge);
                    String adviceType = isAOPEdge ? getAOPAdviceType(edge) : null;

                    String edgeKey = currentMethod.getSignature() + "->" + callee.getSignature() +
                        (isAOPEdge ? "_AOP_" + adviceType : "");

                    if (!edgeInfoMap.containsKey(edgeKey)) {
                        edgeInfoMap.put(edgeKey, new EdgeInfo(currentMethod, callee, isAOPEdge, adviceType, callSite));
                    }
                });
            }

            // 方法2: 获取所有从当前方法出发的边（包括AOP边）
            callGraph.reachableMethods().forEach(targetCSMethod -> {
                JMethod targetMethod = targetCSMethod.getMethod();

                // 获取所有指向目标方法的入边
                callGraph.edgesInTo(targetCSMethod).forEach(edge -> {
                    // 检查这条边是否从当前方法出发
                    CSMethod caller = callGraph.getContainerOf(edge.getCallSite());
                    if (caller != null && caller.getMethod().equals(currentMethod)) {
                        JMethod callee = edge.getCallee().getMethod();

                        // 判断是否为AOP边
                        boolean isAOPEdge = isAOPEdge(edge);
                        String adviceType = isAOPEdge ? getAOPAdviceType(edge) : null;

                        String edgeKey = currentMethod.getSignature() + "->" + callee.getSignature() +
                            (isAOPEdge ? "_AOP_" + adviceType : "");

                        if (!edgeInfoMap.containsKey(edgeKey)) {
                            edgeInfoMap.put(edgeKey, new EdgeInfo(currentMethod, callee, isAOPEdge, adviceType, edge.getCallSite()));
                        }
                    }
                });
            });
        }

        int methodIndex = methodIndexer.getIndex(currentMethod);
        String currentMethodLabel = "\"" + methodIndex + "\"";

        // 分类当前方法
        classifyMethod(currentMethod, currentMethodLabel, rankGroups, edgeInfoMap);

        nodesContent.append(currentMethodLabel)
            .append(" [label=\"")
            .append(currentMethod.getSignature())
            .append("\"];\n");

        // 按AOP执行顺序排序边
        List<EdgeInfo> sortedEdges = new ArrayList<>(edgeInfoMap.values());
        sortedEdges.sort((e1, e2) -> {
            if (e1.isAOP && e2.isAOP) {
                return getAOPOrder(e1.adviceType) - getAOPOrder(e2.adviceType);
            } else if (e1.isAOP) {
                return -1;
            } else if (e2.isAOP) {
                return 1;
            }
            return 0;
        });

        for (EdgeInfo edgeInfo : sortedEdges) {
            JMethod callee = edgeInfo.callee;
            int calleeIndex = methodIndexer.getIndex(callee);
            String calleeMethodLabel = "\"" + calleeIndex + "\"";

            String edgeKey = currentMethodLabel + " -> " + calleeMethodLabel +
                (edgeInfo.isAOP ? "_AOP_" + edgeInfo.adviceType : "_" + edgeInfo.callSite.hashCode());

            if (!addedEdges.contains(edgeKey)) {
                edgesContent.append(currentMethodLabel)
                    .append(" -> ")
                    .append(calleeMethodLabel);

                // 为AOP边设置不同的样式
                if (edgeInfo.isAOP) {
                    edgesContent.append(" [label=\"[AOP-")
                        .append(edgeInfo.adviceType)
                        .append("]\", color=red, style=dashed, fontcolor=red, penwidth=2");

                    // 根据AOP类型设置不同的权重，影响边的布局
                    edgesContent.append(", weight=").append(getAOPWeight(edgeInfo.adviceType));
                    edgesContent.append("];\n");
                } else {
                    edgesContent.append(" [label=\"")
                        .append(getCallSiteLabel(edgeInfo.callSite))
                        .append("\", weight=5];\n");
                }
                addedEdges.add(edgeKey);
            }
            explore(callee, visited, addedEdges, nodesContent, edgesContent, rankGroups);
        }
    }

    private void classifyMethod(JMethod method, String label, Map<String, Set<String>> rankGroups,
                                Map<String, EdgeInfo> edgeInfoMap) {
        String methodName = method.getName().toLowerCase();
        String className = method.getDeclaringClass().getName().toLowerCase();

        // 检查是否是工具类方法
        if (className.contains("java.lang") || className.contains("java.util")) {
            rankGroups.get("utility").add(label);
            return;
        }

        // 检查是否是AOP advice方法
        if (methodName.contains("before")) {
            rankGroups.get("before").add(label);
        } else if (methodName.contains("around")) {
            rankGroups.get("around").add(label);
        } else if (methodName.contains("afterreturning")) {
            rankGroups.get("afterReturning").add(label);
        } else if (methodName.contains("afterthrowing")) {
            rankGroups.get("afterThrowing").add(label);
        } else if (methodName.contains("after")) {
            rankGroups.get("after").add(label);
        } else {
            // 检查是否有AOP边指向它
            boolean hasAOPEdge = edgeInfoMap.values().stream()
                .anyMatch(e -> e.callee.equals(method) && e.isAOP);

            if (hasAOPEdge) {
                rankGroups.get("target").add(label);
            } else {
                rankGroups.get("entry").add(label);
            }
        }
    }

    private String generateRankConstraints(Map<String, Set<String>> rankGroups) {
        StringBuilder sb = new StringBuilder();

        // 定义层级顺序
        String[] order = {"entry", "before", "around", "target", "afterReturning", "afterThrowing", "after", "utility"};

        for (String rank : order) {
            Set<String> nodes = rankGroups.get(rank);
            if (nodes != null && !nodes.isEmpty()) {
                sb.append("{ rank=same; ");
                for (String node : nodes) {
                    sb.append(node).append("; ");
                }
                sb.append("}\n");
            }
        }

        return sb.toString();
    }

    private int getAOPOrder(String adviceType) {
        if (adviceType == null) return 999;
        switch (adviceType) {
            case "BEFORE":
                return 1;
            case "AROUND":
                return 2;
            case "AFTER_RETURNING":
                return 3;
            case "AFTER_THROWING":
                return 4;
            case "AFTER":
                return 5;
            default:
                return 999;
        }
    }

    private int getAOPWeight(String adviceType) {
        if (adviceType == null) return 1;
        switch (adviceType) {
            case "BEFORE":
                return 10;
            case "AROUND":
                return 9;
            case "AFTER_RETURNING":
                return 7;
            case "AFTER_THROWING":
                return 6;
            case "AFTER":
                return 5;
            default:
                return 1;
        }
    }

    private boolean isAOPEdge(Edge<CSCallSite, CSMethod> edge) {
        try {
            String edgeClassName = edge.getClass().getSimpleName();
            String kindString = edge.getKind().toString();
            return edgeClassName.contains("AOP") || kindString.contains("AOP");
        } catch (Exception e) {
            return false;
        }
    }

    private String getAOPAdviceType(Edge<CSCallSite, CSMethod> edge) {
        try {
            java.lang.reflect.Field adviceTypeField = edge.getClass().getDeclaredField("adviceType");
            adviceTypeField.setAccessible(true);
            Object adviceType = adviceTypeField.get(edge);
            if (adviceType != null) {
                return adviceType.toString();
            }
        } catch (Exception e) {
            try {
                String kindString = edge.getKind().toString();
                if (kindString.contains("BEFORE")) {
                    return "BEFORE";
                } else if (kindString.contains("AFTER_RETURNING")) {
                    return "AFTER_RETURNING";
                } else if (kindString.contains("AFTER_THROWING")) {
                    return "AFTER_THROWING";
                } else if (kindString.contains("AFTER")) {
                    return "AFTER";
                } else if (kindString.contains("AROUND")) {
                    return "AROUND";
                }
            } catch (Exception ex) {
                // ignore
            }
        }
        return "ADVICE";
    }

    private String getCallSiteLabel(CSCallSite callSite) {
        String callSiteStr = callSite.getCallSite().toString();
        int index = callSiteStr.indexOf('>');
        if (index != -1 && index + 1 < callSiteStr.length()) {
            return callSiteStr.substring(index + 1).trim();
        }
        return callSiteStr;
    }

    private static class EdgeInfo {
        JMethod caller;
        JMethod callee;
        boolean isAOP;
        String adviceType;
        CSCallSite callSite;

        EdgeInfo(JMethod caller, JMethod callee, boolean isAOP, String adviceType,
                 CSCallSite callSite) {
            this.caller = caller;
            this.callee = callee;
            this.isAOP = isAOP;
            this.adviceType = adviceType;
            this.callSite = callSite;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EdgeInfo edgeInfo = (EdgeInfo) o;
            return isAOP == edgeInfo.isAOP &&
                caller.equals(edgeInfo.caller) &&
                callee.equals(edgeInfo.callee) &&
                callSite.equals(edgeInfo.callSite) &&
                (adviceType != null ? adviceType.equals(edgeInfo.adviceType) : edgeInfo.adviceType == null);
        }

        @Override
        public int hashCode() {
            int result = caller.hashCode();
            result = 31 * result + callee.hashCode();
            result = 31 * result + (isAOP ? 1 : 0);
            result = 31 * result + (adviceType != null ? adviceType.hashCode() : 0);
            result = 31 * result + callSite.hashCode();
            return result;
        }
    }
}