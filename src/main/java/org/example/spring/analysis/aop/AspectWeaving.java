package org.example.spring.analysis.aop;

import pascal.taie.language.classes.JMethod;

/**
 * 表示切面方法的织入信息
 * 用于在调用图中插入切面方法调用
 */
public record AspectWeaving(JMethod aspectMethod, AspectMethod.AdviceType adviceType, Pointcut pointcut) {

    /**
     * 判断是否在目标方法执行之前织入
     */
    public boolean isBeforeWeaving() {
        return adviceType == AspectMethod.AdviceType.BEFORE ||
            adviceType == AspectMethod.AdviceType.AROUND;
    }

    /**
     * 判断是否在目标方法执行之后织入
     */
    public boolean isAfterWeaving() {
        return adviceType == AspectMethod.AdviceType.AFTER ||
            adviceType == AspectMethod.AdviceType.AFTER_RETURNING ||
            adviceType == AspectMethod.AdviceType.AFTER_THROWING;
    }
}
