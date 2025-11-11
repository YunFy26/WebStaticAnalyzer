package org.example.printer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.spring.analysis.router.ControllerClass;
import org.example.spring.analysis.RouterAnalysis;
import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.CallGraphBuilder;
import pascal.taie.analysis.graph.icfg.ICFG;
import pascal.taie.analysis.graph.icfg.ICFGBuilder;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;

import java.io.IOException;
import java.util.List;

public class OutputPrinter {

    private static final Logger logger = LogManager.getLogger(OutputPrinter.class);

    public static void outputUrls(){
        logger.info("Output the router to output/urls.txt.");
        List<ControllerClass> routerAnalysis = World.get().getResult(RouterAnalysis.ID);
        UrlPrinter urlPrinter = new UrlPrinter(routerAnalysis);
        urlPrinter.printUrls();
    }

    public static void outputCallFlows(){
        logger.info("Output the call flow of each entry method to output/callFlows.");
        PointerAnalysisResult pointerAnalysisResult = World.get().getResult(PointerAnalysis.ID);
        CallGraph<CSCallSite, CSMethod> csCallGraph = pointerAnalysisResult.getCSCallGraph();
//        CallGraph<Invoke, JMethod> callGraph = World.get().getResult(CallGraphBuilder.ID);
        CallGraphPrinter callGraphPrinter = new CallGraphPrinter(csCallGraph);
        csCallGraph.entryMethods().forEach(csMethod -> {
            try {
                callGraphPrinter.generateDotFile(csMethod.getMethod());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void outputIcfg(){
        logger.info("Output the icfg of each entry method to output/callFlows.");
        ICFG<JMethod, Stmt> icfg = World.get().getResult(ICFGBuilder.ID);
        ICFGPrinter icfgPrinter = new ICFGPrinter(icfg);
        icfgPrinter.dumpICFG();
    }

}
