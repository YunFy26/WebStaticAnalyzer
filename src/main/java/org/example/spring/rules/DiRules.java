package org.example.spring.rules;

/**
 * 依赖注入相关规则
 */
public class DiRules {

    // 自动注入注解
    public static final String AUTOWIRED = "org.springframework.beans.factory.annotation.Autowired";

    public static final String JSR250_RESOURCE = "javax.annotation.Resource";

    public static final String JSR330_INJECT = "javax.inject.Inject";

    // 限定符注解
    public static final String QUALIFIER = "org.springframework.beans.factory.annotation.Qualifier";

    public static final String JSR330_NAMED = "javax.inject.Named";

    // 值注入注解
    public static final String VALUE = "org.springframework.beans.factory.annotation.Value";

    // 生命周期注解
    public static final String POST_CONSTRUCT = "javax.annotation.PostConstruct";

    public static final String PRE_DESTROY = "javax.annotation.PreDestroy";
}