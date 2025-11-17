package org.example.printer.aop;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.spring.plugin.AOPEdge;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * AOP 调用边输出工具类
 * 仅输出 AOP 织入的调用边
 */
public class AOPEdgesToFile {

    private static final Logger logger = LogManager.getLogger(AOPEdgesToFile.class);

    // 输出目录
    private static final String OUTPUT_DIR = "output";

    // AOP 边输出文件路径
    private static final String AOP_EDGES_FILE = OUTPUT_DIR + "/aop-call-edges.txt";

    /**
     * 输出 AOP 调用边到文件
     *
     * @param callGraph 调用图
     */
    public static void writeAOPEdgesToFile(CallGraph<CSCallSite, CSMethod> callGraph) {
        try {
            // 确保输出目录存在
            ensureOutputDirectory();

            try (PrintWriter aopWriter = new PrintWriter(new FileWriter(AOP_EDGES_FILE))) {

                int aopEdgeCount = 0;

                // 遍历所有可达方法
                for (CSMethod csMethod : callGraph.reachableMethods().toList()) {
                    // 获取该方法的所有调用边
                    for (Edge<CSCallSite, CSMethod> edge : csMethod.getEdges()) {
                        // 仅处理 AOP 边
                        if (edge instanceof AOPEdge aopEdge) {
                            String edgeStr = formatAOPEdge(aopEdge);
                            aopWriter.println(edgeStr);
                            aopEdgeCount++;
                        }
                    }
                }

//                logger.info("AOP 调用边输出完成: {} 条边 -> {}", aopEdgeCount, AOP_EDGES_FILE);

            }
        } catch (IOException e) {
            logger.error("输出 AOP 调用边失败", e);
        }
    }

    /**
     * 格式化 AOP 调用边
     * 格式: <caller>/<method>/<index>\t<callee>
     */
    private static String formatAOPEdge(AOPEdge edge) {
        CSCallSite callSite = edge.getCallSite();
        CSMethod callee = edge.getCallee();

        return String.format("<%s>/%s/%d\t<%s>",
            callSite.getCallSite().getContainer().getSignature(),
            callSite.getCallSite().getMethodRef().resolve().getSignature(),
            callSite.getCallSite().getIndex(),
            callee.getMethod().getSignature());
    }

    /**
     * 确保输出目录存在
     */
    private static void ensureOutputDirectory() throws IOException {
        Path outputPath = Paths.get(OUTPUT_DIR);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
            logger.info("创建输出目录: {}", OUTPUT_DIR);
        }
    }
}
