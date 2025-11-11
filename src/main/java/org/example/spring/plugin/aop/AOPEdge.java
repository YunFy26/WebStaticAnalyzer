package org.example.spring.plugin.aop;

import org.example.spring.analysis.aop.AspectMethod;
import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;

public class AOPEdge extends Edge<CSCallSite, CSMethod> {

    private final AspectMethod.AdviceType adviceType;

    public AOPEdge(CSCallSite callSite, CSMethod callee, AspectMethod.AdviceType adviceType) {
        super(CallKind.VIRTUAL, callSite, callee);
        this.adviceType = adviceType;
    }

    @Override
    public String getInfo() {
        return "AOP-" + adviceType.name();
    }

    public AspectMethod.AdviceType getAdviceType() {
        return adviceType;
    }
}
