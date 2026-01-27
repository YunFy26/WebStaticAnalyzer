package org.example.spring.analysis.aop;

import org.example.enums.AnnotationElementKeys;
import org.example.spring.rules.AspectRules;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.annotation.Element;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.util.HashMap;
import java.util.Map;

/**
 * 切面类初始化器
 * 负责解析 @Pointcut 和 @Before/@After 等注解，构建 AspectClass 模型
 */
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

        // 临时存储当前类的命名切点，用于在本类 Advice 解析时快速查找
        // 虽然 AspectClass 里也存了一份，但这里用局部变量更清晰，且用于构建过程
        Map<String, Pointcut> tempNamedPointcuts = new HashMap<>();

        // =========================================================================
        // 第一步: 收集所有通过 @Pointcut 定义的命名切点
        // =========================================================================
        for (JMethod jMethod : jClass.getDeclaredMethods()) {
            Annotation pointcutAnnotation = jMethod.getAnnotation(AspectRules.POINTCUT);
            if (pointcutAnnotation != null) {
                Element expression = pointcutAnnotation.getElement(AnnotationElementKeys.VALUE);
                if (expression != null) {
                    String expr = cleanExpression(expression.toString());
                    Pointcut pointcut = new Pointcut(jMethod);
                    pointcut.setExpression(expr);

                    // 解析类型：命名切点本身也可能是组合切点
                    if (isCombinedExpression(expr)) {
                        pointcut.setType(Pointcut.PointcutType.COMBINED);
                    } else {
                        pointcut.setType(parsePointcutType(expr));
                    }

                    // 1. 存入局部 Map
                    tempNamedPointcuts.put(jMethod.getName(), pointcut);
                    // 2. [关键] 存入 AspectClass 全局注册表，供 Matcher 递归查找引用
                    aspect.addNamedPointcut(jMethod.getName(), pointcut);
                }
            }
        }

        // 缓存池，避免重复创建相同的 Pointcut 对象，节省内存
        Map<Pointcut, Pointcut> pointcutCache = new HashMap<>();

        // =========================================================================
        // 第二步: 解析所有通知方法 (Advice) 上的切点定义
        // =========================================================================
        for (JMethod jMethod : jClass.getDeclaredMethods()) {
            ADVICE_TYPE_MAP.entrySet().stream()
                .filter(entry -> jMethod.getAnnotation(entry.getKey()) != null)
                .findFirst()
                .ifPresent(entry -> {
                    Annotation adviceAnno = jMethod.getAnnotation(entry.getKey());
                    if (adviceAnno == null) return;

                    // 获取切点表达式：可能在 value 属性，也可能在 pointcut 属性
                    Element element = adviceAnno.getElement(AnnotationElementKeys.VALUE);
                    if (element == null) {
                        element = adviceAnno.getElement(AnnotationElementKeys.POINTCUT);
                    }

                    if (element != null) {
                        String expr = cleanExpression(element.toString());

                        // 核心解析逻辑：处理组合、引用或内联
                        Pointcut targetPointcut = resolvePointcut(expr, tempNamedPointcuts, pointcutCache);

                        if (targetPointcut != null) {
                            AspectMethod aspectMethod = new AspectMethod(jMethod);
                            aspectMethod.setAdviceType(entry.getValue());
                            aspectMethod.setPointcut(targetPointcut);

                            // 将 "解析后的最终切点" 与 "通知方法" 绑定
                            aspect.addAspectMethod(targetPointcut, aspectMethod);
                        } else {
                            System.err.println("[AOP Warning] 无法解析切点表达式: '" + expr + "' in " + jClass.getName() + "." + jMethod.getName());
                        }
                    }
                });
        }
    }

    /**
     * 辅助方法：清理字符串引号和首尾空白
     */
    private static String cleanExpression(String raw) {
        if (raw == null) return "";
        return raw.trim().replaceAll("^\"|\"$", "");
    }

    /**
     * 核心解析逻辑：
     * 1. 优先检查是否包含逻辑运算符 (Combined)
     * 2. 其次检查是否为命名引用 (Reference)
     * 3. 最后作为内联基础切点解析 (Inline)
     */
    private static Pointcut resolvePointcut(String expr,
                                            Map<String, Pointcut> namedPointcuts,
                                            Map<Pointcut, Pointcut> pointcutCache) {
        // 1. 组合切点判定 (包含逻辑运算符)
        if (isCombinedExpression(expr)) {
            Pointcut combinedPointcut = new Pointcut(null); // 组合切点没有单一的 Method 对应
            combinedPointcut.setExpression(expr);
            combinedPointcut.setType(Pointcut.PointcutType.COMBINED);
            // 使用缓存
            return pointcutCache.computeIfAbsent(combinedPointcut, k -> k);
        }

        // 2. 命名引用判定 (格式如 "myPointcut()")
        // 必须不包含 execution 等关键字，且看起来像方法调用
        if (isNamedReference(expr)) {
            String refName = extractMethodName(expr);
            Pointcut ref = namedPointcuts.get(refName);
            if (ref != null) {
                return ref;
            }
            // 注意：这里如果返回 null，外部会打印 Warning。
            // 真实场景可能涉及跨类引用 (e.g. "com.Config.pc()")，当前版本暂不支持跨类解析。
            return null;
        }

        // 3. 内联基础切点 (execution, within, @annotation 等)
        Pointcut inlinePointcut = new Pointcut(null);
        inlinePointcut.setExpression(expr);
        inlinePointcut.setType(parsePointcutType(expr));
        return pointcutCache.computeIfAbsent(inlinePointcut, k -> k);
    }

    /**
     * 判断是否包含逻辑运算符
     */
    private static boolean isCombinedExpression(String expr) {
        return expr.contains("&&") || expr.contains("||") ||
            expr.contains("!") ||
            expr.contains(" and ") || expr.contains(" or ") || expr.contains(" not ");
    }

    /**
     * 判断是否为命名引用
     * 规则：包含括号，且前缀不是 execution/within/args 等保留关键字
     */
    private static boolean isNamedReference(String expr) {
        if (!expr.contains("(")) return false; // 必须是方法调用形式
        String prefix = expr.substring(0, expr.indexOf('(')).trim();

        // 如果前缀在保留字 Map 中，则不是引用
        boolean isReserved = POINTCUT_PREFIX_MAP.containsKey(prefix + "(") ||
            prefix.equals("execution") ||
            prefix.equals("within") ||
            prefix.equals("target") ||
            prefix.equals("args") ||
            prefix.equals("this") ||
            prefix.equals("bean");

        return !isReserved;
    }

    /**
     * 提取方法名 "myPointcut()" -> "myPointcut"
     */
    private static String extractMethodName(String expr) {
        int idx = expr.indexOf('(');
        return idx > 0 ? expr.substring(0, idx).trim() : expr;
    }

    /**
     * 解析基础切点类型
     */
    private static Pointcut.PointcutType parsePointcutType(String expression) {
        // 双重检查：如果是组合切点，优先返回 COMBINED
        if (isCombinedExpression(expression)) {
            return Pointcut.PointcutType.COMBINED;
        }

        for (Map.Entry<String, Pointcut.PointcutType> entry : POINTCUT_PREFIX_MAP.entrySet()) {
            if (expression.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        // 默认返回 EXECUTION
        return Pointcut.PointcutType.EXECUTION;
    }
}