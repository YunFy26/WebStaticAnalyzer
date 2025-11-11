package org.example.spring.plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.spring.analysis.AspectAnalysis;
import org.example.spring.analysis.aop.*;
import org.example.spring.plugin.AspectHelper;
import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;

import java.util.List;

/**
 * AOP 织入插件
 * 在指针分析时为目标方法调用织入切面方法
 */
public class ProcessAopPlugin implements Plugin {

    private static final Logger logger = LogManager.getLogger(ProcessAopPlugin.class);

    private Solver solver;

    private final List<AspectClass> aspects = World.get().getResult(AspectAnalysis.ID);

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
    }

    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        AspectHelper.processAspects(aspects, edge, solver);
    }
}
