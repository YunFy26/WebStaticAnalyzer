package org.example.spring.rules;

/**
 * 切面编程相关规则
 */
public class AspectRules {

    // 切面注解
    public static final String ASPECT = "org.aspectj.lang.annotation.Aspect";

    // 通知类型注解
    public static final String BEFORE = "org.aspectj.lang.annotation.Before";

    public static final String AFTER = "org.aspectj.lang.annotation.After";

    public static final String AFTER_RETURNING = "org.aspectj.lang.annotation.AfterReturning";

    public static final String AFTER_THROWING = "org.aspectj.lang.annotation.AfterThrowing";

    public static final String AROUND = "org.aspectj.lang.annotation.Around";

    // 切入点注解
    public static final String POINTCUT = "org.aspectj.lang.annotation.Pointcut";

    // 引入声明注解
    public static final String DECLARE_PARENTS = "org.aspectj.lang.annotation.DeclareParents";

    // 切面排序注解
    public static final String ORDER = "org.springframework.core.annotation.Order";

    // 切面配置注解
    public static final String ENABLE_ASPECTJ_AUTO_PROXY = "org.springframework.context.annotation.EnableAspectJAutoProxy";

    // 切点类型
    public static final String POINTCUT_TYPE_EXECUTE = "@execute";

    public static final String POINTCUT_TYPE_WITHIN = "@within";

    public static final String POINTCUT_TYPE_ANNOTATION = "@annotation";
}