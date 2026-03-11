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

        Map<JMethod, NodeInfo> nodeInfoMap = new HashMap<>();
        Map<String, EdgeInfo> edgeInfoMap = new HashMap<>();

        collectGraphInfo(entryMethod, visited, nodeInfoMap, edgeInfoMap);

        Map<JMethod, Integer> depthMap = calculateDepth(entryMethod, edgeInfoMap);

        Map<Integer, List<JMethod>> depthGroups = new TreeMap<>();
        Map<Integer, List<JMethod>> aopDepthGroups = new TreeMap<>();

        for (Map.Entry<JMethod, NodeInfo> entry : nodeInfoMap.entrySet()) {
            JMethod method = entry.getKey();
            NodeInfo info = entry.getValue();

            if (info.isAOPMethod) {
                int targetDepth = depthMap.getOrDefault(method, 0);
                aopDepthGroups.computeIfAbsent(targetDepth, k -> new ArrayList<>()).add(method);
            } else {
                int depth = depthMap.getOrDefault(method, 0);
                depthGroups.computeIfAbsent(depth, k -> new ArrayList<>()).add(method);
            }
        }

        StringBuilder dotContent = new StringBuilder("digraph G {\n");
        dotContent.append("  rankdir=TB;\n");
        dotContent.append("  ranksep=1.0;\n");
        dotContent.append("  nodesep=0.5;\n");
        dotContent.append("  node [shape=box, style=filled, fillcolor=\"lightblue\"];\n");
        dotContent.append("  edge [color=\"black\"];\n\n");

        // 改进的图例 - 放在顶部
        dotContent.append("  // Legend\n");
        dotContent.append("  subgraph cluster_legend {\n");
        dotContent.append("    label=\"Legend\";\n");
        dotContent.append("    style=dashed;\n");
        dotContent.append("    fontsize=10;\n");
        dotContent.append("    rank=source;\n");  // 改为顶部
        dotContent.append("    node [shape=plaintext];\n");
        dotContent.append("    legend [label=<\n");
        dotContent.append("      <TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"4\">\n");
        dotContent.append("        <TR><TD BGCOLOR=\"lightblue\"><B>Business Logic</B></TD><TD>Black Solid Line</TD></TR>\n");
        dotContent.append("        <TR><TD BGCOLOR=\"lightyellow\"><B>AOP Advice</B></TD><TD><FONT COLOR=\"red\">Red Dashed Line (Weaving)</FONT></TD></TR>\n");
        dotContent.append("        <TR><TD COLSPAN=\"2\"><FONT COLOR=\"#FF8C00\">Orange Solid Line (AOP Internal)</FONT></TD></TR>\n");
        dotContent.append("      </TABLE>\n");
        dotContent.append("    >];\n");
        dotContent.append("  }\n\n");

        // AOP方法子图
        if (!aopDepthGroups.isEmpty()) {
            dotContent.append("  subgraph cluster_aop {\n");
            dotContent.append("    style=invis;\n");
            for (List<JMethod> methods : aopDepthGroups.values()) {
                for (JMethod method : methods) {
                    String label = getMethodLabel(method);
                    dotContent.append("    ").append(label)
                        .append(" [label=\"").append(escapeLabel(method.getSignature()))
                        .append("\", fillcolor=\"lightyellow\"];\n");
                }
            }
            dotContent.append("  }\n\n");
        }

        // 主调用链节点
        for (Map.Entry<Integer, List<JMethod>> entry : depthGroups.entrySet()) {
            int depth = entry.getKey();
            List<JMethod> methods = entry.getValue();

            dotContent.append("  // Depth ").append(depth).append("\n");
            dotContent.append("  { rank=same; ");
            for (JMethod method : methods) {
                String label = getMethodLabel(method);
                dotContent.append(label).append("; ");
            }
            dotContent.append("}\n");

            for (JMethod method : methods) {
                String label = getMethodLabel(method);
                dotContent.append("  ").append(label)
                    .append(" [label=\"").append(escapeLabel(method.getSignature())).append("\"];\n");
            }
            dotContent.append("\n");
        }

        // 强制层级顺序
        dotContent.append("  // Force ranking order\n");
        List<Integer> depths = new ArrayList<>(depthGroups.keySet());
        for (int i = 0; i < depths.size() - 1; i++) {
            List<JMethod> currentLevel = depthGroups.get(depths.get(i));
            List<JMethod> nextLevel = depthGroups.get(depths.get(i + 1));
            if (!currentLevel.isEmpty() && !nextLevel.isEmpty()) {
                dotContent.append("  ").append(getMethodLabel(currentLevel.get(0)))
                    .append(" -> ").append(getMethodLabel(nextLevel.get(0)))
                    .append(" [style=invis, weight=100];\n");
            }
        }
        dotContent.append("\n");

        // 生成边
        dotContent.append("  // Edges\n");
        Set<String> addedEdges = new HashSet<>();

        for (EdgeInfo edgeInfo : edgeInfoMap.values()) {
            String callerLabel = getMethodLabel(edgeInfo.caller);
            String calleeLabel = getMethodLabel(edgeInfo.callee);

            String displayEdgeKey = callerLabel + " -> " + calleeLabel;
            if (edgeInfo.isAOP) {
                displayEdgeKey += "_AOP_" + edgeInfo.adviceType;
            } else {
                displayEdgeKey += "_" + getCallSiteLabel(edgeInfo.callSite);
            }

            if (addedEdges.contains(displayEdgeKey)) {
                continue;
            }
            addedEdges.add(displayEdgeKey);

            dotContent.append("  ").append(callerLabel)
                .append(" -> ")
                .append(calleeLabel);

            if (edgeInfo.isAOP) {
                dotContent.append(" [label=\"[AOP-")
                    .append(edgeInfo.adviceType)
                    .append("]\", color=red, style=dashed, fontcolor=red, penwidth=2, constraint=false];\n");
            } else {
                boolean callerIsAOP = isAOPMethodByName(edgeInfo.caller);

                if (callerIsAOP) {
                    dotContent.append(" [label=\"")
                        .append(escapeLabel(getCallSiteLabel(edgeInfo.callSite)))
                        .append("\", color=\"#FF8C00\", fontcolor=\"#FF8C00\", penwidth=1.5, weight=5];\n");
                } else {
                    dotContent.append(" [label=\"")
                        .append(escapeLabel(getCallSiteLabel(edgeInfo.callSite)))
                        .append("\", color=\"black\", weight=10];\n");
                }
            }
        }

        dotContent.append("}\n");
        return dotContent.toString();
    }

    private void collectGraphInfo(JMethod currentMethod, Set<String> visited,
                                  Map<JMethod, NodeInfo> nodeInfoMap,
                                  Map<String, EdgeInfo> edgeInfoMap) {
        String currentMethodSignature = currentMethod.getSignature();
        if (visited.contains(currentMethodSignature)) {
            return;
        }
        visited.add(currentMethodSignature);

        boolean isAOPMethod = isAOPMethodByName(currentMethod);
        nodeInfoMap.put(currentMethod, new NodeInfo(isAOPMethod));

        Set<CSMethod> csMethods = ptaResult.getCSCallGraph().reachableMethods()
            .filter(csMethod -> csMethod.getMethod().equals(currentMethod))
            .collect(Collectors.toSet());

        for (CSMethod csMethod : csMethods) {
            Set<CSCallSite> callSites = callGraph.getCallSitesIn(csMethod);

            for (CSCallSite callSite : callSites) {
                callGraph.edgesOutOf(callSite).forEach(edge -> {
                    JMethod callee = edge.getCallee().getMethod();

                    if (shouldSkipMethod(callee)) {
                        return;
                    }

                    boolean isAOPEdge = isAOPEdge(edge);
                    String adviceType = isAOPEdge ? getAOPAdviceType(edge) : null;

                    String edgeKey = createEdgeKey(currentMethod, callee, callSite, isAOPEdge, adviceType);

                    if (!edgeInfoMap.containsKey(edgeKey)) {
                        edgeInfoMap.put(edgeKey, new EdgeInfo(currentMethod, callee, isAOPEdge, adviceType, callSite));
                    }

                    collectGraphInfo(callee, visited, nodeInfoMap, edgeInfoMap);
                });
            }

            callGraph.reachableMethods().forEach(targetCSMethod -> {
                callGraph.edgesInTo(targetCSMethod).forEach(edge -> {
                    CSMethod caller = callGraph.getContainerOf(edge.getCallSite());
                    if (caller != null && caller.getMethod().equals(currentMethod)) {
                        boolean isAOPEdge = isAOPEdge(edge);
                        if (!isAOPEdge) {
                            return;
                        }

                        JMethod callee = edge.getCallee().getMethod();

                        if (shouldSkipMethod(callee)) {
                            return;
                        }

                        String adviceType = getAOPAdviceType(edge);

                        String edgeKey = createEdgeKey(currentMethod, callee, edge.getCallSite(), true, adviceType);

                        if (!edgeInfoMap.containsKey(edgeKey)) {
                            edgeInfoMap.put(edgeKey, new EdgeInfo(currentMethod, callee, true, adviceType, edge.getCallSite()));
                        }

                        collectGraphInfo(callee, visited, nodeInfoMap, edgeInfoMap);
                    }
                });
            });
        }
    }

    private boolean shouldSkipMethod(JMethod method) {
        String className = method.getDeclaringClass().getName();
        String methodName = method.getName();

        if (className.startsWith("java.lang.StringBuilder") ||
            className.startsWith("java.lang.StringBuffer")) {
            return true;
        }

        if (className.equals("java.lang.String") &&
            (methodName.equals("toString") || methodName.equals("valueOf"))) {
            return true;
        }

        return false;
    }

    private String createEdgeKey(JMethod caller, JMethod callee, CSCallSite callSite,
                                 boolean isAOP, String adviceType) {
        StringBuilder key = new StringBuilder();
        key.append(caller.getSignature())
            .append("->")
            .append(callee.getSignature());

        if (isAOP) {
            key.append("_AOP_").append(adviceType);
        } else {
            key.append("_").append(callSite.getCallSite().toString());
        }

        return key.toString();
    }

    private Map<JMethod, Integer> calculateDepth(JMethod entryMethod, Map<String, EdgeInfo> edgeInfoMap) {
        Map<JMethod, Integer> depthMap = new HashMap<>();
        Queue<JMethod> queue = new LinkedList<>();

        depthMap.put(entryMethod, 0);
        queue.offer(entryMethod);

        while (!queue.isEmpty()) {
            JMethod current = queue.poll();
            int currentDepth = depthMap.get(current);

            for (EdgeInfo edgeInfo : edgeInfoMap.values()) {
                if (edgeInfo.caller.equals(current)) {
                    JMethod callee = edgeInfo.callee;

                    int newDepth;
                    if (edgeInfo.isAOP) {
                        boolean calleeIsAOP = isAOPMethodByName(callee);
                        if (calleeIsAOP) {
                            newDepth = currentDepth;
                        } else {
                            boolean callerIsAOP = isAOPMethodByName(current);
                            newDepth = callerIsAOP ? currentDepth + 2 : currentDepth + 1;
                        }
                    } else {
                        newDepth = currentDepth + 1;
                    }

                    if (!depthMap.containsKey(callee) || depthMap.get(callee) > newDepth) {
                        depthMap.put(callee, newDepth);
                        queue.offer(callee);
                    }
                }
            }
        }

        return depthMap;
    }

    private boolean isAOPMethodByName(JMethod method) {
        String methodName = method.getName().toLowerCase();
        String className = method.getDeclaringClass().getName().toLowerCase();

        return methodName.contains("before") || methodName.contains("after") ||
            methodName.contains("around") || className.contains("aspect");
    }

    private String getMethodLabel(JMethod method) {
        int methodIndex = methodIndexer.getIndex(method);
        return "\"" + methodIndex + "\"";
    }

    /**
     * 转义DOT label中的特殊字符
     * 注意：在普通字符串label中，< 和 > 不需要转义
     */
    private String escapeLabel(String label) {
        return label.replace("\"", "\\\"");
    }

    public void generateDotFile(JMethod entryMethod) throws IOException {
        String dotContent = dotContent(entryMethod);

        String directoryPath = "output/callFlows";
        Files.createDirectories(Paths.get(directoryPath));
//        String fileName = String.valueOf(entryMethod.getDeclaringClass()) + '.' +
//            entryMethod.getName() + '(' +
//            entryMethod.getParamTypes()
//                .stream()
//                .map(Type::toString)
//                .collect(Collectors.joining(",")) +
//            ')' + ".dot";
        String fileName = entryMethod.getDeclaringClass() + "." +
            entryMethod.getName() + ".dot";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(directoryPath + '/' + fileName))) {
            writer.write(dotContent);
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
                if (kindString.contains("BEFORE")) return "BEFORE";
                if (kindString.contains("AFTER_RETURNING")) return "AFTER_RETURNING";
                if (kindString.contains("AFTER_THROWING")) return "AFTER_THROWING";
                if (kindString.contains("AFTER")) return "AFTER";
                if (kindString.contains("AROUND")) return "AROUND";
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

    private static class NodeInfo {
        boolean isAOPMethod;

        NodeInfo(boolean isAOPMethod) {
            this.isAOPMethod = isAOPMethod;
        }
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
    }
}