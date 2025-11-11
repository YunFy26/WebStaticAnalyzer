package org.example.spring.analysis.aop;

import org.example.enums.AnnotationElementKeys;
import org.example.spring.rules.AspectRules;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.annotation.Element;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.util.*;

public class AspectClassInitializer {

    private static final Map<String, Pointcut.PointcutType> POINTCUT_PREFIX_MAP = Map.of(
        "@within(", Pointcut.PointcutType.AT_WITHIN,
        "@target(", Pointcut.PointcutType.AT_TARGET,
        "@annotation(", Pointcut.PointcutType.ANNOTATION,
        "@args(", Pointcut.PointcutType.AT_ARGS,
        "execution(", Pointcut.PointcutType.EXECUTION,
        "within(", Pointcut.PointcutType.WITHIN,
        "target(", Pointcut.PointcutType.TARGET,
        "args(", Pointcut.PointcutType.ARGS,
        "this(", Pointcut.PointcutType.THIS,
        "bean(", Pointcut.PointcutType.BEAN
    );

    private static final Map<String, AspectMethod.AdviceType> ADVICE_TYPE_MAP = Map.of(
        AspectRules.BEFORE, AspectMethod.AdviceType.BEFORE,
        AspectRules.AFTER, AspectMethod.AdviceType.AFTER,
        AspectRules.AFTER_RETURNING, AspectMethod.AdviceType.AFTER_RETURNING,
        AspectRules.AFTER_THROWING, AspectMethod.AdviceType.AFTER_THROWING,
        AspectRules.AROUND, AspectMethod.AdviceType.AROUND
    );

    public static AspectClass createAspectClass(JClass jClass) {
        AspectClass aspectClass = new AspectClass(jClass);
        parseAspect(aspectClass);
        return aspectClass;
    }

    public static void parseAspect(AspectClass aspect) {
        JClass jClass = aspect.getjClass();

        // 第一步: 收集所有通过 @Pointcut 定义的命名切点
        Map<String, Pointcut> namedPointcuts = new HashMap<>();
        for (JMethod jMethod : jClass.getDeclaredMethods()) {
            Annotation pointcutAnnotation = jMethod.getAnnotation(AspectRules.POINTCUT);
            if (pointcutAnnotation != null) {
                Element expression = pointcutAnnotation.getElement(AnnotationElementKeys.VALUE);
                if (expression != null) {
                    String expr = expression.toString().trim().replaceAll("^\"|\"$", "");
                    Pointcut pointcut = new Pointcut(jMethod);
                    pointcut.setExpression(expr);
                    pointcut.setType(parsePointcutType(expr));
                    namedPointcuts.put(jMethod.getName(), pointcut);
                }
            }
        }

        // 第二步: 解析所有通知方法上的切点定义
        // 缓存，避免重复创建切点，当advice type和expression相同时复用同一个Pointcut对象
        Map<Pointcut, Pointcut> pointcutCache = new HashMap<>();

        for (JMethod jMethod : jClass.getDeclaredMethods()) {
            ADVICE_TYPE_MAP.entrySet().stream()
                .filter(entry -> jMethod.getAnnotation(entry.getKey()) != null)
                .findFirst()
                .ifPresent(entry -> {
                    AspectMethod aspectMethod = new AspectMethod(jMethod);
                    aspectMethod.setAdviceType(entry.getValue());

                    Annotation adviceAnno = jMethod.getAnnotation(entry.getKey());
                    assert adviceAnno != null;
                    Element element = adviceAnno.getElement(AnnotationElementKeys.VALUE);
                    if (element == null) {
                        element = adviceAnno.getElement(AnnotationElementKeys.POINTCUT);
                    }

                    if (element != null) {
                        String expr = element.toString().trim().replaceAll("^\"|\"$", "");
                        // 统一命名切点与内联切点
                        Pointcut targetPointcut = resolvePointcut(expr, jMethod, namedPointcuts, pointcutCache);

                        // 关联切点与切面方法
                        if (targetPointcut != null) {
                            aspectMethod.setPointcut(targetPointcut);
                            aspect.addAspectMethod(targetPointcut, aspectMethod);
                        }
                    }
                });
        }
    }

    /**
     * 解析切点,统一管理命名切点和内联切点（通知方法上直接定义的切点表达式）
     */
    private static Pointcut resolvePointcut(String expr, JMethod jMethod,
                                            Map<String, Pointcut> namedPointcuts,
                                            Map<Pointcut, Pointcut> pointcutCache) {
        // 判断是否为命名切点引用 (如 "executionPointcut()")
        // 必须在检查组合切点之前执行
        if (expr.matches("^[A-Za-z_$][A-Za-z0-9_$]*\\s*\\(\\s*\\)$")) {
            String refName = expr.substring(0, expr.indexOf('(')).trim();
            return namedPointcuts.get(refName);
        }

        // 处理组合切点表达式 (如 "executionPointcut() && withinPointcut()")
        // 必须包含逻辑运算符和至少两个切点引用
        if (expr.matches(".*\\(\\)\\s*(&&|\\|\\||!|and|or|not)\\s*.*\\(\\).*") ||
            expr.matches("^!\\s*[A-Za-z_$][A-Za-z0-9_$]*\\s*\\(\\s*\\)$")) {
            Pointcut combinedPointcut = new Pointcut(null);
            combinedPointcut.setExpression(expr);
            combinedPointcut.setType(Pointcut.PointcutType.COMBINED);
            return pointcutCache.computeIfAbsent(combinedPointcut, k -> combinedPointcut);
        }

        // 内联切点表达式
        Pointcut inlinePointcut = new Pointcut(null);
        inlinePointcut.setExpression(expr);
        inlinePointcut.setType(parsePointcutType(expr));

        // 使用缓存避免重复创建相同的切点
        return pointcutCache.computeIfAbsent(inlinePointcut, k -> inlinePointcut);
    }

    /**
     * 解析切点类型,支持所有 Spring AOP 切点表达式
     */
    private static Pointcut.PointcutType parsePointcutType(String expression) {
        // 优先检查是否为组合切点
        if (expression.contains("&&") || expression.contains("||") ||
            expression.contains(" and ") || expression.contains(" or ") ||
            (expression.trim().startsWith("!") && expression.contains("()"))) {
            return Pointcut.PointcutType.COMBINED;
        }

        // 遍历前缀映射表
        for (Map.Entry<String, Pointcut.PointcutType> entry : POINTCUT_PREFIX_MAP.entrySet()) {
            if (expression.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }

        // 默认返回 EXECUTION 类型
        return Pointcut.PointcutType.EXECUTION;
    }
}
