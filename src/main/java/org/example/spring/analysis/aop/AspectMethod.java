package org.example.spring.analysis.aop;

import pascal.taie.language.classes.JMethod;

/**
 * 表示一个切面方法（Before/After/Around 等）
 */
public class AspectMethod {

    private final JMethod method;

    private AdviceType adviceType;

    private Pointcut pointcut;

    public AspectMethod(JMethod method) {
        this.method = method;
    }

    public JMethod getMethod() {
        return method;
    }

    public AdviceType getAdviceType() {
        return adviceType;
    }

    public void setAdviceType(AdviceType adviceType) {
        this.adviceType = adviceType;
    }

    public Pointcut getPointcut() {
        return pointcut;
    }

    public void setPointcut(Pointcut pointcut) {
        this.pointcut = pointcut;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AspectMethod{\n");
        sb.append("    method=").append(method.getName()).append("\n");
        sb.append("    adviceType=").append(adviceType != null ? adviceType : "N/A").append("\n");
        sb.append("    pointcut=");
        if (pointcut != null) {
            sb.append("\n      ").append(pointcut.toString().replace("\n", "\n      "));
        } else {
            sb.append("N/A");
        }
        sb.append("\n  }");
        return sb.toString();
    }


    public enum AdviceType {
        BEFORE,
        AFTER,
        AFTER_RETURNING,
        AFTER_THROWING,
        AROUND
    }
}
