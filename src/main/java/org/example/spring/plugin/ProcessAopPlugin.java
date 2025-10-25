package org.example.spring.plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;

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
