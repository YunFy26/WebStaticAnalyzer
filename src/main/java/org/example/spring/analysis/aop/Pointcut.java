package org.example.spring.analysis.aop;

import pascal.taie.language.classes.JMethod;

import java.util.Objects;

/**
 * 表示一个切点定义
 */
public class Pointcut {

    private final JMethod jMethod;

    private String expression;

    private PointcutType type;

    public Pointcut(JMethod jMethod) {
        this.jMethod = jMethod;
    }

    public JMethod getjMethod() {
        return jMethod;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public PointcutType getType() {
        return type;
    }

    public void setType(PointcutType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pointcut pointcut = (Pointcut) o;
        return Objects.equals(expression, pointcut.expression) && type == pointcut.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, type);
    }

    @Override
    public String toString() {
        return String.format("Pointcut{method=%s, type=%s, expression='%s'}",
            jMethod != null ? jMethod.getName() : "inline",
            type != null ? type : "N/A",
            expression != null ? expression : "N/A"
        );
    }


    public enum PointcutType {
        EXECUTION,
        WITHIN,
        AT_WITHIN,
        THIS,
        TARGET,
        AT_TARGET,
        ARGS,
        AT_ARGS,
        ANNOTATION,
        BEAN,
        COMBINED
    }


}
