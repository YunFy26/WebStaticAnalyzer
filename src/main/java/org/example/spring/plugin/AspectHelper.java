package org.example.spring.plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.spring.analysis.aop.AspectClass;
import org.example.spring.plugin.AspectEdge;
import org.example.spring.analysis.aop.AspectMethod;
import org.example.spring.analysis.aop.Pointcut;
import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.ir.IR;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.classes.JMethod;

import java.util.List;

public class AspectHelper {

    private static final Logger logger = LogManager.getLogger(AspectHelper.class);

    /**
     * 检查方法调用是否匹配切点表达式
     */
    public static boolean matchesPointcut(Pointcut pointcut, JMethod callee) {
        if (pointcut == null || pointcut.getExpression() == null) {
            return false;
        }

        return switch (pointcut.getType()) {
            case EXECUTION -> matchesExecution(pointcut.getExpression(), callee);
            case BEAN -> matchesBean(pointcut.getExpression(), callee);
            case AT_WITHIN -> matchesAtWithin(pointcut.getExpression(), callee);
            case WITHIN -> matchesWithin(pointcut.getExpression(), callee);
            case AT_TARGET, ANNOTATION -> matchesAnnotation(pointcut.getExpression(), callee);
            default -> {
                logger.warn("不支持的切点类型: {}", pointcut.getType());
                yield false;
            }
        };
    }

    /**
     * 匹配 execution 表达式
     */
    private static boolean matchesExecution(String expression, JMethod method) {
        String pattern = expression.substring(expression.indexOf('(') + 1, expression.lastIndexOf(')'));
        String[] parts = pattern.trim().split("\\s+", 2);
        if (parts.length < 2) {
            return false;
        }

        String methodPattern = parts[1];
        int lastDot = methodPattern.lastIndexOf('.');
        if (lastDot == -1) {
            return false;
        }

        String classPattern = methodPattern.substring(0, lastDot);
        String methodName = methodPattern.substring(lastDot + 1);

        if (methodName.contains("(")) {
            methodName = methodName.substring(0, methodName.indexOf('('));
        }

        String targetClass = method.getDeclaringClass().getName();
        String targetMethod = method.getName();

        return matchesPattern(targetClass, classPattern) &&
            matchesPattern(targetMethod, methodName);
    }

    /**
     * 匹配 bean 表达式
     */
    private static boolean matchesBean(String expression, JMethod callee) {
        String beanPattern = expression.substring(expression.indexOf('(') + 1, expression.lastIndexOf(')'));
        String targetClass = callee.getDeclaringClass().getSimpleName();
        String defaultBeanName = Character.toLowerCase(targetClass.charAt(0)) + targetClass.substring(1);
        return matchesPattern(defaultBeanName, beanPattern);
    }

    /**
     * 匹配 @within 表达式
     */
    private static boolean matchesAtWithin(String expression, JMethod method) {
        String annotationType = expression.substring(expression.indexOf('(') + 1, expression.lastIndexOf(')'));
        Annotation annotation = method.getDeclaringClass().getAnnotation(annotationType);
        return annotation != null;
    }

    /**
     * 匹配 within 表达式
     */
    private static boolean matchesWithin(String expression, JMethod method) {
        String classPattern = expression.substring(expression.indexOf('(') + 1, expression.lastIndexOf(')'));
        String targetClass = method.getDeclaringClass().getName();
        return matchesPattern(targetClass, classPattern);
    }

    /**
     * 匹配注解表达式
     */
    private static boolean matchesAnnotation(String expression, JMethod method) {
        String annotationType = expression.substring(expression.indexOf('(') + 1, expression.lastIndexOf(')'));
        Annotation annotation = method.getAnnotation(annotationType);
        return annotation != null;
    }

    /**
     * 通配符匹配工具方法
     */
    private static boolean matchesPattern(String target, String pattern) {
        if (target.equals(pattern)) {
            return true;
        }
        if (pattern.equals("*")) {
            return true;
        }
        if (pattern.contains("..")) {
            String regex = pattern.replace("..", ".*").replace(".", "\\.");
            return target.matches(regex);
        }
        if (pattern.contains("*")) {
            String regex = pattern.replace("*", ".*").replace(".", "\\.");
            return target.matches(regex);
        }
        return false;
    }

    /**
     * 织入切面方法
     * 根据通知类型创建不同的织入策略
     */
    public static void weaveAdvice(Edge<CSCallSite, CSMethod> edge, AspectMethod aspectMethod,
                                   AspectClass aspect, Solver solver) {
        JMethod adviceMethod = aspectMethod.getMethod();
        CSCallSite csCallSite = edge.getCallSite();
        CSMethod csCallee = edge.getCallee();

        logger.info("织入切面: {} -> 通知: {} -> 目标方法: {}",
            aspect.getjClass().getName(),
            aspectMethod.getAdviceType(),
            csCallee.getMethod().getSignature());

        // 创建上下文敏感的切面方法
        CSMethod csAdviceMethod = solver.getCSManager().getCSMethod(
            csCallSite.getContext(), adviceMethod);

        // 根据通知类型创建不同的切面边
        switch (aspectMethod.getAdviceType()) {
            case BEFORE:
                // BEFORE: 在目标方法调用前执行
                addBeforeAdvice(csCallSite, csAdviceMethod, aspect, solver);
                break;

            case AFTER:
            case AFTER_RETURNING:
            case AFTER_THROWING:
                // AFTER: 在目标方法调用后执行
                addAfterAdvice(csCallSite, csCallee, csAdviceMethod, aspectMethod.getAdviceType(),
                    aspect, solver);
                break;

            case AROUND:
                // AROUND: 环绕目标方法执行
                addAroundAdvice(csCallSite, csCallee, csAdviceMethod, aspect, solver);
                break;

            default:
                logger.warn("不支持的通知类型: {}", aspectMethod.getAdviceType());
        }

        // 将切面方法添加到可达方法集合
        addReachableAdviceMethod(csAdviceMethod, solver);
    }

    /**
     * 添加 BEFORE 通知边
     * 在调用点直接添加切面方法调用
     */
    private static void addBeforeAdvice(CSCallSite csCallSite, CSMethod csAdviceMethod,
                                        AspectClass aspect, Solver solver) {
        AspectEdge aspectEdge = new AspectEdge(
            CallKind.OTHER,
            csCallSite,
            csAdviceMethod,
            AspectMethod.AdviceType.BEFORE,
            aspect.getjClass().getName()
        );

        solver.addCallEdge(aspectEdge);
    }

    /**
     * 添加 AFTER 通知边
     * 从目标方法的退出点添加切面方法调用
     */
    private static void addAfterAdvice(CSCallSite csCallSite, CSMethod csTargetMethod,
                                       CSMethod csAdviceMethod, AspectMethod.AdviceType adviceType,
                                       AspectClass aspect, Solver solver) {
        // 创建一个虚拟的调用点,表示在目标方法返回后的位置
        // 注意:这里需要从目标方法的退出点创建边,而不是从调用点
        AspectEdge aspectEdge = new AspectEdge(
            CallKind.OTHER,
            csCallSite,  // 使用原调用点,但标记为 AFTER 类型
            csAdviceMethod,
            adviceType,
            aspect.getjClass().getName()
        );

        solver.addCallEdge(aspectEdge);
    }

    /**
     * 添加 AROUND 通知边
     * 环绕通知需要处理 ProceedingJoinPoint.proceed() 调用
     */
    private static void addAroundAdvice(CSCallSite csCallSite, CSMethod csTargetMethod,
                                        CSMethod csAdviceMethod, AspectClass aspect,
                                        Solver solver) {
        // 1. 添加调用点到环绕通知的边
        AspectEdge aspectEdge = new AspectEdge(
            CallKind.OTHER,
            csCallSite,
            csAdviceMethod,
            AspectMethod.AdviceType.AROUND,
            aspect.getjClass().getName()
        );

        solver.addCallEdge(aspectEdge);

        // 2. 处理环绕通知中的 proceed() 调用
        handleProceedCall(csAdviceMethod, csTargetMethod, solver);
    }

    /**
     * 处理 ProceedingJoinPoint.proceed() 调用
     */
    private static void handleProceedCall(CSMethod csAdviceMethod, CSMethod csTargetMethod,
                                          Solver solver) {
        JMethod adviceMethod = csAdviceMethod.getMethod();
        IR ir = adviceMethod.getIR();

        if (ir == null) {
            return;
        }

        // TODO: 查找 proceed() 调用点,并添加到目标方法的边
        // 这需要遍历 IR 中的所有 Invoke 语句,找到调用 proceed() 的地方
        logger.debug("处理环绕通知的 proceed() 调用: {} -> {}",
            csAdviceMethod, csTargetMethod);
    }

    /**
     * 将切面方法添加到可达方法集合
     */
    private static void addReachableAdviceMethod(CSMethod csAdviceMethod, Solver solver) {
        JMethod adviceMethod = csAdviceMethod.getMethod();

        // 检查方法是否已经被处理过
        if (solver.getCallGraph().contains(csAdviceMethod)) {
            return;
        }

        logger.debug("添加可达切面方法: {}", csAdviceMethod);
        solver.addCSMethod(csAdviceMethod);
    }

    /**
     * 批量处理切面织入
     */
    public static void processAspects(List<AspectClass> aspects,
                                      Edge<CSCallSite, CSMethod> edge,
                                      Solver solver) {
        JMethod callee = edge.getCallee().getMethod();

        for (AspectClass aspect : aspects) {
            for (var entry : aspect.getPointcutMethodMap().entrySet()) {
                Pointcut pointcut = entry.getKey();
                List<AspectMethod> aspectMethods = entry.getValue();

                // 检查当前方法调用是否匹配切点
                if (matchesPointcut(pointcut, callee)) {
                    // 织入所有匹配的切面方法
                    for (AspectMethod aspectMethod : aspectMethods) {
                        weaveAdvice(edge, aspectMethod, aspect, solver);
                    }
                }
            }
        }
    }
}
