package org.example.spring.plugin;

import org.example.spring.analysis.aop.AspectMethod;
import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;

/**
 * 表示切面调用边,用于区分普通调用和切面织入
 */
public class AspectEdge extends Edge<CSCallSite, CSMethod> {

    private final AspectMethod.AdviceType adviceType;
    private final String aspectName;

    public AspectEdge(CallKind kind, CSCallSite callSite, CSMethod callee,
                      AspectMethod.AdviceType adviceType, String aspectName) {
        super(kind, callSite, callee);
        this.adviceType = adviceType;
        this.aspectName = aspectName;
    }

    @Override
    public String getInfo() {
        return String.format("[ASPECT:%s] %s", adviceType, aspectName);
    }

    public AspectMethod.AdviceType getAdviceType() {
        return adviceType;
    }

    public String getAspectName() {
        return aspectName;
    }
}
