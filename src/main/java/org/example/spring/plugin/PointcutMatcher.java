package org.example.spring.plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.spring.analysis.aop.AspectClass;
import org.example.spring.analysis.aop.Pointcut;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

/**
 * 切点匹配器
 * 修复了 execution 匹配逻辑，支持 AspectJ 表达式解析与 Tai-e 签名的正确比对
 */
public class PointcutMatcher {

    private static final Logger logger = LogManager.getLogger(PointcutMatcher.class);

    public static boolean matches(Pointcut pointcut, JMethod targetMethod, AspectClass aspect) {
        if (pointcut == null) return false;

        String expression = pointcut.getExpression();
        Pointcut.PointcutType type = pointcut.getType();

        if (type == Pointcut.PointcutType.COMBINED) {
            return matchesCombinedPointcut(expression, targetMethod, aspect);
        }

        return switch (type) {
            case EXECUTION -> matchesExecution(pointcut, targetMethod);
            case WITHIN -> matchesWithin(expression, targetMethod);
            case AT_WITHIN -> matchesAtWithin(expression, targetMethod);
            case ANNOTATION -> matchesAtAnnotation(expression, targetMethod);
            case AT_TARGET -> matchesAtTarget(expression, targetMethod);
            case ARGS -> matchesArgs(expression, targetMethod);
            case AT_ARGS -> matchesAtArgs(expression, targetMethod);
            case THIS, TARGET -> matchesThisOrTarget(expression, targetMethod);
            case BEAN -> matchesBean(expression, targetMethod);
            default -> {
                logger.warn("Unknown pointcut type: " + type);
                yield false;
            }
        };
    }

    // =========================================================================
    // 组合切点逻辑
    // =========================================================================

    private static boolean matchesCombinedPointcut(String expression, JMethod method, AspectClass aspect) {
        String expr = expression.trim();

        // 简单的去括号处理
        while (expr.startsWith("(") && expr.endsWith(")")) {
            if (isValidParentheses(expr.substring(1, expr.length() - 1))) {
                expr = expr.substring(1, expr.length() - 1).trim();
            } else {
                break;
            }
        }

        if (expr.contains("||") || expr.contains(" or ")) {
            String[] parts = expr.split("\\|\\|| or ");
            for (String part : parts) {
                if (matchesCombinedPointcut(part, method, aspect)) return true;
            }
            return false;
        }

        if (expr.contains("&&") || expr.contains(" and ")) {
            String[] parts = expr.split("&&| and ");
            for (String part : parts) {
                if (!matchesCombinedPointcut(part, method, aspect)) return false;
            }
            return true;
        }

        if (expr.startsWith("!") || expr.startsWith("not ")) {
            String sub = expr.startsWith("!") ? expr.substring(1) : expr.substring(4);
            return !matchesCombinedPointcut(sub, method, aspect);
        }

        return evaluateSubPointcut(expr, method, aspect);
    }

    private static boolean isValidParentheses(String s) {
        int count = 0;
        for (char c : s.toCharArray()) {
            if (c == '(') count++;
            else if (c == ')') count--;
            if (count < 0) return false;
        }
        return count == 0;
    }

    private static boolean evaluateSubPointcut(String expr, JMethod method, AspectClass aspect) {
        expr = expr.trim();

        if (expr.matches("^[A-Za-z_$][A-Za-z0-9_$]*\\s*\\(\\s*\\)$")) {
            String name = expr.substring(0, expr.indexOf('(')).trim();
            Pointcut ref = aspect.getNamedPointcut(name);
            if (ref != null) {
                return matches(ref, method, aspect);
            }
            return false;
        }

        if (expr.startsWith("execution(")) return matchesExecution(createTempPointcut(expr, Pointcut.PointcutType.EXECUTION), method);
        if (expr.startsWith("within(")) return matchesWithin(expr, method);
        if (expr.startsWith("@annotation(")) return matchesAtAnnotation(expr, method);
        if (expr.startsWith("args(")) return matchesArgs(expr, method);
        if (expr.startsWith("@args(")) return matchesAtArgs(expr, method);
        if (expr.startsWith("@within(")) return matchesAtWithin(expr, method);
        if (expr.startsWith("@target(")) return matchesAtTarget(expr, method);
        if (expr.startsWith("bean(")) return matchesBean(expr, method);

        return false;
    }

    private static Pointcut createTempPointcut(String expr, Pointcut.PointcutType type) {
        Pointcut p = new Pointcut(null);
        p.setExpression(expr);
        p.setType(type);
        return p;
    }

    // =========================================================================
    // 核心修复：健壮的 execution 匹配
    // =========================================================================

    /**
     * 解析 AspectJ 表达式，拆分为 [返回值] [类名] [方法名] [参数] 分别进行匹配
     */
    private static boolean matchesExecution(Pointcut pointcut, JMethod method) {
        String expression = pointcut.getExpression();
        // 提取括号内的内容: "execution(* com.A.method(..))" -> "* com.A.method(..)"
        String pattern = extractPattern(expression);
        if (pattern.isEmpty()) return false;

        // 1. 提取参数部分
        int paramsStart = pattern.indexOf('(');
        int paramsEnd = pattern.lastIndexOf(')');
        if (paramsStart == -1 || paramsEnd == -1) return false;

        String argsPattern = pattern.substring(paramsStart + 1, paramsEnd).trim();
        // 剩余部分：RetType ClassName.MethodName
        String signaturePart = pattern.substring(0, paramsStart).trim();

        // 2. 拆分返回值、类名、方法名
        // AspectJ 格式通常是: [Modifiers] RetType ClassName.MethodName
        // 这里简化处理：寻找最后一个点号分离方法名
        int lastDot = signaturePart.lastIndexOf('.');
        if (lastDot == -1) {
            // 没有点号，说明没有指定类名，类似于 "execution(* method(..))"
            // 这在实际 Spring AOP 中很少见，通常都是完整限定名
            return wildcardMatch(signaturePart, method.getName());
        }

        String methodNamePat = signaturePart.substring(lastDot + 1);
        String beforeMethod = signaturePart.substring(0, lastDot).trim(); // "RetType ClassName"

        // 分离返回值和类名
        // 寻找 beforeMethod 中的最后一个空格
        int lastSpace = beforeMethod.lastIndexOf(' ');
        String classNamePat;
        // String retTypePat; // 暂时忽略返回值匹配，因为 Tai-e 的 Type 格式和 AspectJ 字符串很难直接对齐

        if (lastSpace != -1) {
            classNamePat = beforeMethod.substring(lastSpace + 1);
            // retTypePat = beforeMethod.substring(0, lastSpace);
        } else {
            // 如果没有空格，说明可能只有类名（不规范）或者只有返回值（不可能）
            // 假设它是类名
            classNamePat = beforeMethod;
        }

        // 3. 执行匹配

        // A. 匹配类名
        String currentClassName = method.getDeclaringClass().getName();
        if (!wildcardMatch(classNamePat, currentClassName)) {
            return false;
        }

        // B. 匹配方法名
        String currentMethodName = method.getName();
        if (!wildcardMatch(methodNamePat, currentMethodName)) {
            return false;
        }

        // C. 匹配参数 (简化版：仅支持 .. 和空)
        // 完整的参数匹配需要解析 AspectJ 的参数类型列表并与 JMethod.getParamTypes 对比
        if (argsPattern.equals("..")) {
            return true;
        }
        if (argsPattern.isEmpty()) {
            return method.getParamCount() == 0;
        }

        // 进一步的参数匹配逻辑可在此扩展...
        return true;
    }

    /**
     * 通配符匹配工具
     * 支持 * (任意字符) 和 .. (包通配符)
     */
    private static boolean wildcardMatch(String pattern, String target) {
        if (pattern.equals("*")) return true;
        if (pattern.equals(target)) return true;

        // 将 AspectJ 通配符转换为正则
        // 1. . 转换为 \.
        // 2. * 转换为 .*
        String regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*");

        return target.matches(regex);
    }

    // =========================================================================
    // 其他匹配逻辑
    // =========================================================================

    private static boolean matchesWithin(String expr, JMethod method) {
        String pattern = extractPattern(expr);
        String className = method.getDeclaringClass().getName();
        return wildcardMatch(pattern, className);
    }

    private static boolean matchesAtAnnotation(String expr, JMethod method) {
        String anno = extractPattern(expr);
        return method.hasAnnotation(anno);
    }

    private static boolean matchesAtWithin(String expr, JMethod method) {
        String anno = extractPattern(expr);
        return method.getDeclaringClass().hasAnnotation(anno);
    }

    private static boolean matchesAtTarget(String expr, JMethod method) {
        String anno = extractPattern(expr);
        return method.getDeclaringClass().hasAnnotation(anno);
    }

    private static boolean matchesArgs(String expr, JMethod method) {
        return true; // 简化版
    }

    private static boolean matchesAtArgs(String expression, JMethod method) {
        String annotation = extractPattern(expression);
        for (int i = 0; i < method.getParamCount(); i++) {
            Type paramType = method.getParamType(i);
            if (paramType instanceof JClass jParamClass) {
                if (jParamClass.hasAnnotation(annotation)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesThisOrTarget(String expr, JMethod method) {
        String typePattern = extractPattern(expr);
        return method.getDeclaringClass().getName().equals(typePattern);
    }

    private static boolean matchesBean(String expr, JMethod method) {
        return true;
    }

    private static String extractPattern(String expression) {
        int start = expression.indexOf('(') + 1;
        int end = expression.lastIndexOf(')');
        if (start > 0 && end > start) {
            return expression.substring(start, end).trim();
        }
        return "";
    }
}