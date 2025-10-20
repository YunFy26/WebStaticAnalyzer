package org.example.spring.plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.spring.analysis.InjectPointsAnalysis;
import org.example.spring.di.injectpoints.InjectPoint;
import org.example.spring.router.ControllerClass;
import org.example.spring.router.RouterMethod;
import pascal.taie.World;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.EmptyParamProvider;
import pascal.taie.analysis.pta.core.solver.EntryPoint;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.TypeSystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ProcessAopPlugin implements Plugin {

    private Solver solver;

    private final Logger logger = LogManager.getLogger(ProcessAopPlugin.class);

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
    }

    @Override
    public void onStart() {

    }


}
