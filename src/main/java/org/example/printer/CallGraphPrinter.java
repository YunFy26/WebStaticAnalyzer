//package org.example.printer;
//
//import pascal.taie.analysis.graph.callgraph.CallGraph;
//import pascal.taie.ir.stmt.Invoke;
//import pascal.taie.language.classes.JMethod;
//import pascal.taie.language.type.Type;
//import pascal.taie.util.Indexer;
//import pascal.taie.util.SimpleIndexer;
//
//import java.io.BufferedWriter;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.HashSet;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//public class CallGraphPrinter {
//
//    private final CallGraph<Invoke, JMethod> callGraph;
//    private final Indexer<JMethod> methodIndexer;
//
//    public CallGraphPrinter(CallGraph<Invoke, JMethod> callGraph) {
//        this.callGraph = callGraph;
//        this.methodIndexer = new SimpleIndexer<>();
//    }
//
//    public String dotContent(JMethod entryMethod){
//        Set<String> visited = new HashSet<>();
//        Set<String> addedEdges = new HashSet<>();
//        StringBuilder nodesContent = new StringBuilder();
//        StringBuilder edgesContent = new StringBuilder();
//        StringBuilder dotContent = new StringBuilder("digraph G {\n");
//        dotContent.append("node [color=\".3 .2 1.0\",shape=box,style=filled];\n");
//        dotContent.append("edge [];\n");
//        explore(entryMethod, visited, addedEdges, nodesContent, edgesContent);
//
//        dotContent.append(nodesContent);
//        dotContent.append(edgesContent);
//        dotContent.append("}\n");
//        return dotContent.toString();
//    }
//
//    public void generateDotFile(JMethod entryMethod) throws IOException {
//        String dotContent = dotContent(entryMethod);
//
//        String directoryPath = "output/callFlows";
//        Files.createDirectories(Paths.get(directoryPath));
//        String fileName = String.valueOf(entryMethod.getDeclaringClass()) + '.' +
//            entryMethod.getName() + '(' +
//            entryMethod.getParamTypes()
//                .stream()
//                .map(Type::toString)
//                .collect(Collectors.joining(",")) +
//            ')'+ ".dot";
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter(directoryPath + '/' + fileName))) {
//            writer.write(dotContent);
//        }
//    }
//
//    private void explore(JMethod currentMethod, Set<String> visited, Set<String> addedEdges,
//                         StringBuilder nodesContent, StringBuilder edgesContent) {
//        String currentMethodSignature = currentMethod.getSignature();
//        if (visited.contains(currentMethodSignature)) {
//            return;
//        }
//        visited.add(currentMethodSignature);
//        Set<JMethod> callees = callGraph.getCalleesOfM(currentMethod);
//
//        int methodIndex = methodIndexer.getIndex(currentMethod);
//
//        String currentMethodLabel = "\"" + methodIndex + "\"";
//        nodesContent.append(currentMethodLabel)
//            .append(" [label=\"")
//            .append(currentMethod.getSignature())
//            .append("\"];\n");
//
//        for (JMethod callee : callees) {
//            int calleeIndex = methodIndexer.getIndex(callee);
//            String calleeMethodLabel = "\"" + calleeIndex + "\"";
//
//            String edgeKey = currentMethodLabel + " -> " + calleeMethodLabel;
//            if (!addedEdges.contains(edgeKey)) {
//                EdgeInfo edgeInfo = getEdgeInfo(currentMethod, callee);
//
//                if (edgeInfo.isAspectEdge) {
//                    edgesContent.append(currentMethodLabel)
//                        .append(" -> ")
//                        .append(calleeMethodLabel)
//                        .append(" [label=\"")
//                        .append(edgeInfo.label)
//                        .append("\", color=red, style=dashed, fontcolor=red, penwidth=2.0];\n");
//                } else {
//                    edgesContent.append(edgeKey)
//                        .append(" [label=\" ")
//                        .append(edgeInfo.label)
//                        .append("\"];\n");
//                }
//                addedEdges.add(edgeKey);
//            }
//            explore(callee, visited, addedEdges, nodesContent, edgesContent);
//        }
//    }
//
//    private static class EdgeInfo {
//        String label;
//        boolean isAspectEdge;
//
//        EdgeInfo(String label, boolean isAspectEdge) {
//            this.label = label;
//            this.isAspectEdge = isAspectEdge;
//        }
//    }
//
//    private EdgeInfo getEdgeInfo(JMethod caller, JMethod callee) {
//        StringBuilder details = new StringBuilder();
//        boolean foundAspectEdge = false;
//
//        for (Invoke callSite : callGraph.getCallSitesIn(caller)) {
//            if (callGraph.getCalleesOf(callSite).contains(callee)) {
//                callGraph.edgesOutOf(callSite).forEach(edge -> {
//                    if (edge.getCallee().equals(callee)) {
//                        String edgeClassName = edge.getClass().getSimpleName();
//                        if (edgeClassName.equals("AOPEdge") || edgeClassName.equals("AspectEdge")) {
//                            try {
//                                var adviceType = edge.getClass().getMethod("getAdviceType").invoke(edge);
//                                details.insert(0, String.format("[AOP:%s]\\n", adviceType));
//                            } catch (Exception e) {
//                                details.insert(0, "[AOP:UNKNOWN]\\n");
//                            }
//                        } else {
//                            String callSiteStr = callSite.toString();
//                            int index = callSiteStr.indexOf('>');
//                            if (index != -1 && index + 1 < callSiteStr.length()) {
//                                details.append(callSiteStr.substring(index + 1).trim()).append("; ");
//                            }
//                        }
//                    }
//                });
//            }
//        }
//
//        String label = details.toString();
//        boolean isAspectEdge = label.contains("[AOP:");
//
//        if (label.isEmpty()) {
//            label = "invoke " + callee.getName() + "();";
//        }
//
//        return new EdgeInfo(label, isAspectEdge);
//    }
//
//    @Deprecated
//    private String getCallSiteDetails(JMethod caller, JMethod callee) {
//        return getEdgeInfo(caller, callee).label;
//    }
//}

package org.example.printer;

import org.example.spring.plugin.aop.AOPEdge;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.ir.stmt.Invoke;
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

/**
 * 支持上下文敏感调用图的打印器
 */
public class CallGraphPrinter {

    // 使用 CS 调用图
    private final CallGraph<CSCallSite, CSMethod> callGraph;
    private final Indexer<JMethod> methodIndexer;

    public CallGraphPrinter(CallGraph<CSCallSite, CSMethod> callGraph) {
        this.callGraph = callGraph;
        this.methodIndexer = new SimpleIndexer<>();
    }

    public String dotContent(JMethod entryMethod) {
        // 找到对应的 CSMethod
        CSMethod csEntryMethod = findCSMethod(entryMethod);
        if (csEntryMethod == null) {
            return "digraph G {\n  error [label=\"Entry method not found in call graph\"];\n}\n";
        }

        Set<String> visited = new HashSet<>();
        Set<String> addedEdges = new HashSet<>();
        StringBuilder nodesContent = new StringBuilder();
        StringBuilder edgesContent = new StringBuilder();
        StringBuilder dotContent = new StringBuilder("digraph G {\n");
        dotContent.append("node [color=\".3 .2 1.0\",shape=box,style=filled];\n");
        dotContent.append("edge [];\n");
        explore(csEntryMethod, visited, addedEdges, nodesContent, edgesContent);

        dotContent.append(nodesContent);
        dotContent.append(edgesContent);
        dotContent.append("}\n");
        return dotContent.toString();
    }

    public void generateDotFile(JMethod entryMethod) throws IOException {
        String dotContent = dotContent(entryMethod);

        String directoryPath = "output/callFlows";
        Files.createDirectories(Paths.get(directoryPath));
        String fileName = sanitizeFileName(entryMethod) + ".dot";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(directoryPath + '/' + fileName))) {
            writer.write(dotContent);
        }
    }

    /**
     * 生成调用边详细报告
     */
    public void generateCallEdgesReport(JMethod entryMethod) throws IOException {
        CSMethod csEntryMethod = findCSMethod(entryMethod);
        if (csEntryMethod == null) {
            System.err.println("✗ 无法找到入口方法: " + entryMethod.getSignature());
            return;
        }

        String directoryPath = "output/call-edges";
        Files.createDirectories(Paths.get(directoryPath));
        String fileName = sanitizeFileName(entryMethod) + "-edges.txt";
        String filePath = directoryPath + '/' + fileName;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("入口方法: " + entryMethod.getSignature() + "\n");
            writer.write("上下文: " + csEntryMethod.getContext() + "\n");
            writer.write("=".repeat(80) + "\n\n");

            // 收集从该入口可达的所有边
            EdgeCollectionResult result = collectCallEdges(csEntryMethod);

            // 打印统计信息
            writer.write(String.format("可达方法数: %d\n", result.visitedMethods.size()));
            writer.write(String.format("可达边总数: %d\n", result.allEdges.size()));
            writer.write(String.format("  - 普通调用边: %d\n", result.normalEdges.size()));
            writer.write(String.format("  - AOP 调用边: %d\n", result.aopEdges.size()));
            writer.write("\n" + "=".repeat(80) + "\n\n");

            // 按通知类型统计 AOP 边
            if (!result.aopEdges.isEmpty()) {
                Map<String, Long> aopEdgesByType = result.aopEdges.stream()
                    .collect(Collectors.groupingBy(
                        EdgeDetail::getAdviceType,
                        Collectors.counting()
                    ));

                writer.write("AOP 边按通知类型统计:\n");
                for (Map.Entry<String, Long> entry : aopEdgesByType.entrySet()) {
                    writer.write(String.format("  - %s: %d\n", entry.getKey(), entry.getValue()));
                }
                writer.write("\n" + "=".repeat(80) + "\n\n");
            }

            // 打印普通调用边
            if (!result.normalEdges.isEmpty()) {
                writer.write("【普通调用边】\n");
                writer.write("-".repeat(80) + "\n");
                for (EdgeDetail edge : result.normalEdges) {
                    printEdgeDetail(writer, edge);
                }
                writer.write("\n");
            }

            // 打印 AOP 调用边
            if (!result.aopEdges.isEmpty()) {
                writer.write("【AOP 调用边】\n");
                writer.write("-".repeat(80) + "\n");
                for (EdgeDetail edge : result.aopEdges) {
                    printAOPEdgeDetail(writer, edge);
                }
                writer.write("\n");
            }
        }

        System.out.println("✓ 生成调用边报告: " + filePath);
    }

    /**
     * 查找 JMethod 对应的 CSMethod(选择第一个上下文)
     */
    private CSMethod findCSMethod(JMethod method) {
        return callGraph.reachableMethods()
            .filter(csMethod -> csMethod.getMethod().equals(method))
            .findFirst()
            .orElse(null);
    }

    /**
     * 打印普通调用边详细信息
     */
    private void printEdgeDetail(BufferedWriter writer, EdgeDetail edge) throws IOException {
        writer.write(String.format("  [%s] %s\n",
            edge.callKind,
            edge.caller.getSignature()));
        writer.write(String.format("    调用点: %s (行号: %d)\n",
            edge.callSite.toString(),
            edge.callSite.getLineNumber()));
        writer.write(String.format("    目标方法: %s\n",
            edge.callee.getSignature()));
        writer.write(String.format("    调用者上下文: %s\n", edge.callerContext));
        writer.write(String.format("    被调者上下文: %s\n", edge.calleeContext));
        writer.write("\n");
    }

    /**
     * 打印 AOP 调用边详细信息
     */
    private void printAOPEdgeDetail(BufferedWriter writer, EdgeDetail edge) throws IOException {
        writer.write(String.format("  [AOP:%s] %s\n",
            edge.adviceType,
            edge.caller.getSignature()));
        writer.write(String.format("    ★ AOP 通知方法: %s\n",
            edge.callee.getSignature()));
        writer.write(String.format("    虚拟调用点: %s (行号: %d)\n",
            edge.callSite.toString(),
            edge.callSite.getLineNumber()));
        writer.write(String.format("    目标方法上下文: %s\n", edge.callerContext));
        writer.write(String.format("    通知方法上下文: %s\n", edge.calleeContext));
        writer.write("\n");
    }

    /**
     * 收集从入口方法可达的所有调用边
     */
    private EdgeCollectionResult collectCallEdges(CSMethod entryMethod) {
        EdgeCollectionResult result = new EdgeCollectionResult();
        Set<CSMethod> visited = new HashSet<>();
        Queue<CSMethod> worklist = new LinkedList<>();

        worklist.add(entryMethod);
        visited.add(entryMethod);

        while (!worklist.isEmpty()) {
            CSMethod current = worklist.poll();
            result.visitedMethods.add(current.getMethod());

            // 收集该方法的所有出边
            callGraph.callSitesIn(current).forEach(callSite -> {
                callGraph.edgesOutOf(callSite).forEach(edge -> {
                    EdgeDetail edgeDetail = createEdgeDetail(edge);
                    result.allEdges.add(edgeDetail);

                    if (edgeDetail.isAOPEdge) {
                        result.aopEdges.add(edgeDetail);
                    } else {
                        result.normalEdges.add(edgeDetail);
                    }

                    CSMethod callee = edge.getCallee();
                    if (!visited.contains(callee)) {
                        visited.add(callee);
                        worklist.add(callee);
                    }
                });
            });
        }

        return result;
    }

    /**
     * 从 CS 边创建边详情对象
     */
    private EdgeDetail createEdgeDetail(Edge<CSCallSite, CSMethod> edge) {
        EdgeDetail detail = new EdgeDetail();
        CSCallSite csCallSite = edge.getCallSite();
        CSMethod csCallee = edge.getCallee();

        detail.callSite = csCallSite.getCallSite();
        detail.caller = callGraph.getContainerOf(csCallSite).getMethod();
        detail.callee = csCallee.getMethod();
        detail.callerContext = csCallSite.getContext().toString();
        detail.calleeContext = csCallee.getContext().toString();

        // 检查是否为 AOP 边
        if (edge instanceof AOPEdge) {
            detail.isAOPEdge = true;
            detail.adviceType = ((AOPEdge) edge).getAdviceType().toString();
        } else {
            detail.callKind = edge.getKind().toString();
        }

        return detail;
    }

    /**
     * 边详情数据类
     */
    private static class EdgeDetail {
        JMethod caller;
        Invoke callSite;
        JMethod callee;
        String callerContext;
        String calleeContext;
        boolean isAOPEdge = false;
        String adviceType = null;
        String callKind = null;

        String getAdviceType() {
            return adviceType != null ? adviceType : "NONE";
        }
    }

    /**
     * 边收集结果
     */
    private static class EdgeCollectionResult {
        Set<JMethod> visitedMethods = new HashSet<>();
        List<EdgeDetail> allEdges = new ArrayList<>();
        List<EdgeDetail> normalEdges = new ArrayList<>();
        List<EdgeDetail> aopEdges = new ArrayList<>();
    }

    /**
     * 生成所有入口方法的报告
     */
    public void generateAllEntryReports() throws IOException {
        List<CSMethod> entryMethods = callGraph.entryMethods()
            .collect(Collectors.toList());

        System.out.println("=".repeat(80));
        System.out.println("调用图边统计报告");
        System.out.println("=".repeat(80));
        System.out.println("入口方法数量: " + entryMethods.size());
        System.out.println("总边数量: " + callGraph.getNumberOfEdges());
        System.out.println("=".repeat(80));

        for (CSMethod csEntryMethod : entryMethods) {
            generateCallEdgesReport(csEntryMethod.getMethod());
        }

        generateSummaryReport();
    }

    /**
     * 生成汇总报告
     */
    private void generateSummaryReport() throws IOException {
        String directoryPath = "output/call-edges";
        Files.createDirectories(Paths.get(directoryPath));
        String filePath = directoryPath + "/summary.txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("调用图汇总报告\n");
            writer.write("=".repeat(80) + "\n\n");

            // 统计所有边
            List<Edge<CSCallSite, CSMethod>> allEdges = callGraph.edges()
                .collect(Collectors.toList());

            long normalEdgeCount = allEdges.stream()
                .filter(edge -> !(edge instanceof AOPEdge))
                .count();

            long aopEdgeCount = allEdges.stream()
                .filter(edge -> edge instanceof AOPEdge)
                .count();

            writer.write(String.format("总边数: %d\n", allEdges.size()));
            writer.write(String.format("  - 普通调用边: %d (%.2f%%)\n",
                normalEdgeCount,
                allEdges.isEmpty() ? 0 : 100.0 * normalEdgeCount / allEdges.size()));
            writer.write(String.format("  - AOP 调用边: %d (%.2f%%)\n",
                aopEdgeCount,
                allEdges.isEmpty() ? 0 : 100.0 * aopEdgeCount / allEdges.size()));
            writer.write("\n");

            // 按通知类型统计 AOP 边
            Map<String, Long> aopEdgesByType = allEdges.stream()
                .filter(edge -> edge instanceof AOPEdge)
                .map(edge -> (AOPEdge) edge)
                .collect(Collectors.groupingBy(
                    edge -> edge.getAdviceType().toString(),
                    Collectors.counting()
                ));

            if (!aopEdgesByType.isEmpty()) {
                writer.write("AOP 边按通知类型统计:\n");
                for (Map.Entry<String, Long> entry : aopEdgesByType.entrySet()) {
                    writer.write(String.format("  - %s: %d\n",
                        entry.getKey(), entry.getValue()));
                }
                writer.write("\n");
            }

            // 统计入口方法
            List<CSMethod> entries = callGraph.entryMethods()
                .collect(Collectors.toList());
            writer.write(String.format("入口方法数: %d\n", entries.size()));
            writer.write("入口方法列表:\n");
            for (CSMethod entry : entries) {
                writer.write(String.format("  - %s [上下文:%s]\n",
                    entry.getMethod().getSignature(),
                    entry.getContext()));
            }
        }

        System.out.println("✓ 生成汇总报告: " + filePath);
    }

    private void explore(CSMethod currentMethod, Set<String> visited, Set<String> addedEdges,
                         StringBuilder nodesContent, StringBuilder edgesContent) {
        JMethod method = currentMethod.getMethod();
        String methodSignature = method.getSignature() + "@" + currentMethod.getContext();

        if (visited.contains(methodSignature)) {
            return;
        }
        visited.add(methodSignature);

        int methodIndex = methodIndexer.getIndex(method);
        String currentMethodLabel = "\"" + methodIndex + "\"";
        nodesContent.append(currentMethodLabel)
            .append(" [label=\"")
            .append(method.getSignature())
            .append("\\n上下文:")
            .append(currentMethod.getContext())
            .append("\"];\n");

        // 遍历所有调用点的出边
        callGraph.callSitesIn(currentMethod).forEach(callSite -> {
            callGraph.edgesOutOf(callSite).forEach(edge -> {
                CSMethod csCallee = edge.getCallee();
                JMethod callee = csCallee.getMethod();
                int calleeIndex = methodIndexer.getIndex(callee);
                String calleeMethodLabel = "\"" + calleeIndex + "\"";

                String edgeKey = currentMethodLabel + " -> " + calleeMethodLabel;
                if (!addedEdges.contains(edgeKey)) {
                    EdgeInfo edgeInfo = getEdgeInfo(edge);

                    if (edgeInfo.isAspectEdge) {
                        edgesContent.append(currentMethodLabel)
                            .append(" -> ")
                            .append(calleeMethodLabel)
                            .append(" [label=\"")
                            .append(edgeInfo.label)
                            .append("\", color=red, style=dashed, fontcolor=red, penwidth=2.0];\n");
                    } else {
                        edgesContent.append(edgeKey)
                            .append(" [label=\"")
                            .append(edgeInfo.label)
                            .append("\"];\n");
                    }
                    addedEdges.add(edgeKey);
                }
                explore(csCallee, visited, addedEdges, nodesContent, edgesContent);
            });
        });
    }

    private static class EdgeInfo {
        String label;
        boolean isAspectEdge;

        EdgeInfo(String label, boolean isAspectEdge) {
            this.label = label;
            this.isAspectEdge = isAspectEdge;
        }
    }

    /**
     * 从 CS 边获取边信息
     */
    private EdgeInfo getEdgeInfo(Edge<CSCallSite, CSMethod> edge) {
        StringBuilder label = new StringBuilder();
        boolean isAspectEdge = false;

        if (edge instanceof AOPEdge) {
            isAspectEdge = true;
            label.append("[AOP:").append(((AOPEdge) edge).getAdviceType()).append("]\\n");
        } else {
            Invoke callSite = edge.getCallSite().getCallSite();
            String callSiteStr = callSite.toString();
            int index = callSiteStr.indexOf('>');
            if (index != -1 && index + 1 < callSiteStr.length()) {
                label.append(callSiteStr.substring(index + 1).trim());
            } else {
                label.append("invoke ").append(edge.getCallee().getMethod().getName()).append("()");
            }
            label.append("\\n行号:").append(callSite.getLineNumber());
        }

        return new EdgeInfo(label.toString(), isAspectEdge);
    }

    private String sanitizeFileName(JMethod method) {
        String fileName = method.getDeclaringClass().toString() + '.' +
            method.getName() + '(' +
            method.getParamTypes()
                .stream()
                .map(Type::toString)
                .collect(Collectors.joining(",")) +
            ')';

        return fileName
            .replace('<', '[')
            .replace('>', ']')
            .replace(':', '-')
            .replace('/', '_')
            .replace('\\', '_')
            .replace('|', '_')
            .replace('?', '_')
            .replace('*', '_')
            .replace('"', '_')
            .replace(' ', '_');
    }
}

