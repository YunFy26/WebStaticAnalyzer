package org.example.spring.plugin.aop;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.spring.analysis.aop.AspectClass;
import org.example.spring.analysis.aop.Pointcut;
import org.example.spring.plugin.AspectHelper;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

/**
 * 切点匹配器 - 负责判断方法是否匹配切点表达式
 */
public class PointcutMatcher {

    private static final Logger logger = LogManager.getLogger(PointcutMatcher.class);

    /**
     * 判断目标方法是否匹配切点表达式
     */
    public static boolean matches(Pointcut pointcut, JMethod targetMethod, AspectClass aspect) {
        String expression = pointcut.getExpression();
        Pointcut.PointcutType type = pointcut.getType();

        // 处理组合切点 (如 "executionPointcut() && withinPointcut()")
        if (type == Pointcut.PointcutType.COMBINED) {
            return matchesCombinedPointcut(expression, targetMethod, aspect);
        }

        // 根据切点类型进行匹配
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
                logger.warn("未知的切点类型: {}", type);
                yield false;
            }
        };
    }

    /**
     * 匹配 execution 切点
     */
    private static boolean matchesExecution(Pointcut pointcut, JMethod method) {
        String expression = pointcut.getExpression();
        // 提取 execution() 内的模式
        String pattern = extractPattern(expression, "execution");

        return matchesExecutionPattern(pattern, method);
    }

    /**
     * 匹配 execution 模式
     * 支持格式: [修饰符] 返回值类型 [类名.]方法名(参数) [throws 异常]
     */
    private static boolean matchesExecutionPattern(String pattern, JMethod method) {
        // 移除多余空格
        pattern = pattern.trim().replaceAll("\\s+", " ");

        // 解析模式各部分
        ExecutionPattern execPattern = parseExecutionPattern(pattern);

        // 匹配修饰符 (如果指定)
        if (execPattern.modifier != null && !matchesModifier(execPattern.modifier, method)) {
            return false;
        }

        // 匹配返回值类型
        if (!matchesReturnType(execPattern.returnType, method)) {
            return false;
        }

        // 匹配类名
        if (!matchesClassName(execPattern.className, method)) {
            return false;
        }

        // 匹配方法名
        if (!matchesMethodName(execPattern.methodName, method)) {
            return false;
        }

        // 匹配参数
        return matchesParameters(execPattern.parameters, method);
    }

    /**
     * 解析 execution 模式
     */
    private static ExecutionPattern parseExecutionPattern(String pattern) {
        ExecutionPattern result = new ExecutionPattern();

        // 查找参数部分
        int paramsStart = pattern.indexOf('(');
        int paramsEnd = pattern.lastIndexOf(')');
        result.parameters = pattern.substring(paramsStart + 1, paramsEnd).trim();

        // 剩余部分: [修饰符] 返回值 类名.方法名
        String beforeParams = pattern.substring(0, paramsStart).trim();
        String[] parts = beforeParams.split("\\s+");

        // 解析: 修饰符 返回值 类名.方法名
        if (parts.length == 1) {
            // 只有 类名.方法名
            parseClassAndMethod(parts[0], result);
        } else if (parts.length == 2) {
            // 返回值 类名.方法名
            result.returnType = parts[0];
            parseClassAndMethod(parts[1], result);
        } else if (parts.length >= 3) {
            // 修饰符 返回值 类名.方法名
            result.modifier = parts[0];
            result.returnType = parts[1];
            parseClassAndMethod(parts[2], result);
        }

        return result;
    }

    /**
     * 解析类名和方法名
     */
    private static void parseClassAndMethod(String qualifiedName, ExecutionPattern result) {
        int lastDot = qualifiedName.lastIndexOf('.');
        if (lastDot > 0) {
            result.className = qualifiedName.substring(0, lastDot);
            result.methodName = qualifiedName.substring(lastDot + 1);
        } else {
            result.methodName = qualifiedName;
        }
    }

    /**
     * 匹配修饰符
     */
    private static boolean matchesModifier(String modifierPattern, JMethod method) {
        if (modifierPattern.equals("*")) {
            return true;
        }

        String modifiers = method.getModifiers().toString().toLowerCase();
        return modifiers.contains(modifierPattern.toLowerCase());
    }

    /**
     * 匹配返回值类型
     */
    private static boolean matchesReturnType(String returnTypePattern, JMethod method) {
        if (returnTypePattern == null || returnTypePattern.equals("*")) {
            return true;
        }

        String returnType = method.getReturnType().getName();

        // 精确匹配
        if (returnType.equals(returnTypePattern)) {
            return true;
        }

        // 通配符匹配
        if (returnTypePattern.contains("*")) {
            String regex = returnTypePattern.replace(".", "\\.").replace("*", ".*");
            return returnType.matches(regex);
        }

        // 包通配符 (如 java.lang..*)
        if (returnTypePattern.contains("..")) {
            String packagePrefix = returnTypePattern.replace("..*", "");
            return returnType.startsWith(packagePrefix);
        }

        return false;
    }

    /**
     * 匹配类名
     */
    private static boolean matchesClassName(String classNamePattern, JMethod method) {
        if (classNamePattern == null || classNamePattern.equals("*")) {
            return true;
        }

        String className = method.getDeclaringClass().getName();

        // 精确匹配
        if (className.equals(classNamePattern)) {
            return true;
        }

        // 包通配符 (如 org.example..*)
        if (classNamePattern.contains("..")) {
            String packagePrefix = classNamePattern.replace("..*", "");
            return className.startsWith(packagePrefix);
        }

        // 通配符匹配 (如 org.example.*.Service)
        if (classNamePattern.contains("*")) {
            String regex = classNamePattern.replace(".", "\\.").replace("*", ".*");
            return className.matches(regex);
        }

        return false;
    }

    /**
     * 匹配方法名
     */
    private static boolean matchesMethodName(String methodNamePattern, JMethod method) {
        if (methodNamePattern == null || methodNamePattern.equals("*")) {
            return true;
        }

        String methodName = method.getName();

        // 精确匹配
        if (methodName.equals(methodNamePattern)) {
            return true;
        }

        // 通配符匹配 (如 get*, *Service)
        if (methodNamePattern.contains("*")) {
            String regex = methodNamePattern.replace("*", ".*");
            return methodName.matches(regex);
        }

        return false;
    }

    /**
     * 匹配参数列表
     */
    private static boolean matchesParameters(String paramPattern, JMethod method) {
        // 匹配任意参数
        if (paramPattern.equals("..")) {
            return true;
        }

        // 无参数
        if (paramPattern.isEmpty()) {
            return method.getParamCount() == 0;
        }

        String[] patterns = paramPattern.split(",");
        int paramCount = method.getParamCount();

        // 处理 (Type,..) 模式 - 至少有一个指定类型参数
        if (patterns.length > 0 && patterns[patterns.length - 1].trim().equals("..")) {
            if (paramCount < patterns.length - 1) {
                return false;
            }
            // 匹配前面的固定参数
            for (int i = 0; i < patterns.length - 1; i++) {
                if (!matchesParameterType(patterns[i].trim(), method.getParamType(i).getName())) {
                    return false;
                }
            }
            return true;
        }

        // 精确匹配所有参数类型
        if (paramCount != patterns.length) {
            return false;
        }

        for (int i = 0; i < paramCount; i++) {
            String paramType = method.getParamType(i).getName();
            String pattern = patterns[i].trim();
            if (!matchesParameterType(pattern, paramType)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 匹配参数类型
     */
    private static boolean matchesParameterType(String pattern, String paramType) {
        if (pattern.equals("*")) {
            return true;
        }

        // 精确匹配
        if (paramType.equals(pattern)) {
            return true;
        }

        // 通配符匹配
        if (pattern.contains("*")) {
            String regex = pattern.replace(".", "\\.").replace("*", ".*");
            return paramType.matches(regex);
        }

        return false;
    }

    /**
     * Execution 模式数据结构
     */
    private static class ExecutionPattern {
        String modifier;      // 修饰符 (如 public, private)
        String returnType;    // 返回值类型
        String className;     // 类名
        String methodName;    // 方法名
        String parameters;    // 参数列表
    }


    /**
     * 匹配 within 切点 - 类型内的所有方法
     */
    private static boolean matchesWithin(String expression, JMethod method) {
        String pattern = extractPattern(expression, "within");
        String className = method.getDeclaringClass().getName();

        // 包通配符 (如 org.example..*)
        if (pattern.contains("..")) {
            String packagePrefix = pattern.replace("..*", "");
            return className.startsWith(packagePrefix);
        }

        // 类名匹配 (支持通配符 *)
        return className.equals(pattern) || className.matches(pattern.replace("*", ".*"));
    }

    /**
     * 匹配 @within 切点 - 类上有指定注解
     */
    private static boolean matchesAtWithin(String expression, JMethod method) {
        String annotation = extractPattern(expression, "@within");
        return method.getDeclaringClass().hasAnnotation(annotation);
    }

    /**
     * 匹配 @annotation 切点 - 方法上有指定注解
     */
    private static boolean matchesAtAnnotation(String expression, JMethod method) {
        String annotation = extractPattern(expression, "@annotation");
        return method.hasAnnotation(annotation);
    }

    /**
     * 匹配 @target 切点 - 目标对象类上有指定注解
     */
    private static boolean matchesAtTarget(String expression, JMethod method) {
        String annotation = extractPattern(expression, "@target");
        return method.getDeclaringClass().hasAnnotation(annotation);
    }

    /**
     * 匹配 args 切点 - 参数类型匹配
     */
    private static boolean matchesArgs(String expression, JMethod method) {
        String argsPattern = extractPattern(expression, "args");

        // 匹配任意参数
        if (argsPattern.equals("..")) {
            return true;
        }

        String[] patterns = argsPattern.split(",");
        int paramCount = method.getParamCount();

        // 处理 (Type,..) 模式 - 第一个参数匹配即可
        if (patterns.length > 0 && patterns[patterns.length - 1].trim().equals("..")) {
            if (paramCount == 0) {
                return false;
            }
            String firstType = patterns[0].trim();
            String paramType = method.getParamType(0).getName();
            return paramType.equals(firstType);
        }

        // 精确匹配所有参数类型
        if (paramCount != patterns.length) {
            return false;
        }

        for (int i = 0; i < paramCount; i++) {
            String paramType = method.getParamType(i).getName();
            String pattern = patterns[i].trim();
            if (!paramType.equals(pattern)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 匹配 @args 切点 - 参数上有指定注解
     */
    private static boolean matchesAtArgs(String expression, JMethod method) {
        String annotation = extractPattern(expression, "@args");
        // 检查是否有参数的类型上标注了指定注解
        for (int i = 0; i < method.getParamCount(); i++) {
            JClass paramType = (JClass) method.getParamType(i);
            if (paramType.hasAnnotation(annotation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 匹配 this/target 切点 - 代理对象/目标对象类型匹配
     */
    private static boolean matchesThisOrTarget(String expression, JMethod method) {
        String prefix = expression.startsWith("this(") ? "this" : "target";
        String typePattern = extractPattern(expression, prefix);
        return matchesType(typePattern, method.getDeclaringClass());
    }

    /**
     * 匹配 bean 切点 - Bean 名称匹配
     */
    private static boolean matchesBean(String expression, JMethod method) {
        String beanPattern = extractPattern(expression, "bean");

        // 从类名推断 Bean 名称 (首字母小写)
        String className = method.getDeclaringClass().getSimpleName();
        String beanName = Character.toLowerCase(className.charAt(0)) + className.substring(1);

        // 支持通配符 (如 *Service)
        return beanName.matches(beanPattern.replace("*", ".*"));
    }

    /**
     * 匹配组合切点 (如 "executionPointcut() && withinPointcut()")
     */
    private static boolean matchesCombinedPointcut(String expression, JMethod method, AspectClass aspect) {
        // AND 逻辑
        if (expression.contains("&&") || expression.contains(" and ")) {
            String[] parts = expression.split("(&&| and )");
            for (String part : parts) {
                if (!evaluateSubPointcut(part.trim(), method, aspect)) {
                    return false;
                }
            }
            return true;
        }

        // OR 逻辑
        if (expression.contains("||") || expression.contains(" or ")) {
            String[] parts = expression.split("(\\|\\|| or )");
            for (String part : parts) {
                if (evaluateSubPointcut(part.trim(), method, aspect)) {
                    return true;
                }
            }
            return false;
        }

        // NOT 逻辑
        if (expression.trim().startsWith("!") || expression.contains(" not ")) {
            String negated = expression.replace("!", "").replace(" not ", "").trim();
            return !evaluateSubPointcut(negated, method, aspect);
        }

        // 单个命名切点引用
        return evaluateSubPointcut(expression, method, aspect);
    }

    /**
     * 评估子切点表达式 - 查找已解析的切点
     */
    private static boolean evaluateSubPointcut(String expr, JMethod method, AspectClass aspect) {
        // 如果是命名切点引用 (如 "executionPointcut()")
        if (expr.matches("^[A-Za-z_$][A-Za-z0-9_$]*\\s*\\(\\s*\\)$")) {
            String pointcutName = expr.substring(0, expr.indexOf('(')).trim();
            Pointcut namedPointcut = findNamedPointcut(aspect, pointcutName);
            if (namedPointcut != null) {
                return matches(namedPointcut, method, aspect);
            }
            logger.warn("未找到命名切点: {}", pointcutName);
            return false;
        }

        // 内联表达式 - 根据前缀判断类型并匹配
        if (expr.startsWith("execution(")) {
            return matchesExecution(createPointcut(expr, Pointcut.PointcutType.EXECUTION), method);
        } else if (expr.startsWith("within(")) {
            return matchesWithin(expr, method);
        } else if (expr.startsWith("@within(")) {
            return matchesAtWithin(expr, method);
        } else if (expr.startsWith("@annotation(")) {
            return matchesAtAnnotation(expr, method);
        } else if (expr.startsWith("@target(")) {
            return matchesAtTarget(expr, method);
        } else if (expr.startsWith("args(")) {
            return matchesArgs(expr, method);
        } else if (expr.startsWith("@args(")) {
            return matchesAtArgs(expr, method);
        } else if (expr.startsWith("this(") || expr.startsWith("target(")) {
            return matchesThisOrTarget(expr, method);
        } else if (expr.startsWith("bean(")) {
            return matchesBean(expr, method);
        }

        logger.warn("无法识别的切点表达式: {}", expr);
        return false;
    }

    /**
     * 创建临时切点对象 (仅用于 execution 表达式匹配)
     */
    private static Pointcut createPointcut(String expression, Pointcut.PointcutType type) {
        Pointcut pointcut = new Pointcut(null);
        pointcut.setExpression(expression);
        pointcut.setType(type);
        return pointcut;
    }

    /**
     * 查找命名切点 - 从已解析的切点中查找
     */
    private static Pointcut findNamedPointcut(AspectClass aspect, String pointcutName) {
        for (var entry : aspect.getPointcutMethodMap().entrySet()) {
            Pointcut pointcut = entry.getKey();
            JMethod pointcutMethod = pointcut.getjMethod();
            if (pointcutMethod != null && pointcutMethod.getName().equals(pointcutName)) {
                return pointcut;
            }
        }
        return null;
    }

    /**
     * 类型匹配 (支持继承关系)
     */
    private static boolean matchesType(String typePattern, JClass jClass) {
        String className = jClass.getName();

        // 直接匹配
        if (className.equals(typePattern)) {
            return true;
        }

        // 检查父类
        if (jClass.getSuperClass() != null) {
            if (matchesType(typePattern, jClass.getSuperClass())) {
                return true;
            }
        }

        // 检查实现的接口
        for (JClass iface : jClass.getInterfaces()) {
            if (matchesType(typePattern, iface)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 从切点表达式中提取模式
     */
    private static String extractPattern(String expression, String prefix) {
        int start = expression.indexOf('(') + 1;
        int end = expression.lastIndexOf(')');
        if (start < end && start > 0) {
            return expression.substring(start, end).trim();
        }
        return "";
    }
}
