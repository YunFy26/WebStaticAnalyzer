package org.example.spring.analysis.di.bean;

import org.example.enums.AnnotationElementKeys;
import org.example.spring.rules.BeanComponentsRules;
import org.example.spring.rules.BeanPropertyRules;
import org.example.utils.StringUtils;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.util.*;
import java.util.stream.Stream;

/**
 * BeanClass工厂
 */
public class BeanClassInitializer {

    /**
     * 创建BeanClass实例
     * @param jClass 匹配Bean注解规则的类
     * @return BeanClass实例
     */
    public static BeanClass createBeanClass(JClass jClass) {
        BeanClass beanClass = new BeanClass(jClass);
        initialize(beanClass);
        processImplementations(beanClass);
        return beanClass;
    }

    /**
     * 从 @Bean 方法创建 BeanClass
     */
    public static BeanClass createBeanFromMethod(JMethod method, JClass beanClass) {
        BeanClass bean = BeanClassInitializer.createBeanClass(beanClass);

        // 应用方法级别的注解
        applyMethodAnnotations(method, bean);

        return bean;
    }

    /**
     * 应用方法级别的注解到 BeanClass
     */
    private static void applyMethodAnnotations(JMethod method, BeanClass bean) {
        // @Primary
        if (method.hasAnnotation(BeanPropertyRules.PRIMARY)) {
            bean.setPrimary(true);
        }

        // @Lazy
        Annotation lazyAnnotation = method.getAnnotation(BeanPropertyRules.LAZY);
        if (lazyAnnotation != null) {
            if (lazyAnnotation.hasElement(AnnotationElementKeys.VALUE)) {
                String lazyValue = Objects.requireNonNull(lazyAnnotation.getElement(AnnotationElementKeys.VALUE))
                    .toString();
                bean.setLazy(Boolean.parseBoolean(lazyValue));
            } else {
                bean.setLazy(true);
            }
        }

        // @Scope
        Annotation scopeAnnotation = method.getAnnotation(BeanPropertyRules.SCOPE);
        if (scopeAnnotation != null && scopeAnnotation.hasElement(AnnotationElementKeys.VALUE)) {
            String scopeValue = Objects.requireNonNull(scopeAnnotation.getElement(AnnotationElementKeys.VALUE))
                .toString();
            bean.setScope(BeanScope.fromValue(scopeValue));
        }

        // @Bean 注解中的名称
        Annotation beanAnnotation = method.getAnnotation(org.example.spring.rules.BeanConfigRules.BEAN);
        if (beanAnnotation != null && beanAnnotation.hasElement(AnnotationElementKeys.VALUE)) {
            String beanName = Objects.requireNonNull(beanAnnotation.getElement(AnnotationElementKeys.VALUE)).toString();
            bean.setDeclaredBeanName(beanName);
        }
    }

    /**
     * 初始化基础属性
     * @param beanClass beanClass
     */
    private static void initialize(BeanClass beanClass) {
        processDefaultBeanName(beanClass);
        processDeclaredBeanName(beanClass);
        processBeanScope(beanClass);
        processIsPrimary(beanClass);
        processIsLazy(beanClass);
        processInterfaces(beanClass);
        processSuperClass(beanClass);
    }

    /**
     * 初始化defaultBeanName
     */
    private static void processDefaultBeanName(BeanClass beanClass) {
        String className = beanClass.getSimpleClassName();
        String defaultName = StringUtils.decapitalizeClassName(className);
        beanClass.setDefaultBeanName(defaultName);
    }

    /**
     * 初始化declaredBeanName
     */
    private static void processDeclaredBeanName(BeanClass beanClass) {
        JClass jClass = beanClass.getjClass();
        String declaredName = Stream.of(
                jClass.getAnnotation(BeanComponentsRules.COMPONENT),
                jClass.getAnnotation(BeanComponentsRules.CONTROLLER),
                jClass.getAnnotation(BeanComponentsRules.REST_CONTROLLER),
                jClass.getAnnotation(BeanComponentsRules.SERVICE),
                jClass.getAnnotation(BeanComponentsRules.REPOSITORY),
                jClass.getAnnotation(BeanComponentsRules.JSR330_NAMED)
            )
            .filter(Objects::nonNull)
            .filter(annotation -> annotation.hasElement(AnnotationElementKeys.VALUE))
            .map(annotation -> Objects.requireNonNull(annotation.getElement(AnnotationElementKeys.VALUE)).toString())
            .findFirst()
            .orElse(null);
        beanClass.setDeclaredBeanName(declaredName);
    }

    /**
     * 处理Bean作用域
     */
    private static void processBeanScope(BeanClass beanClass) {
        JClass jClass = beanClass.getjClass();
        Annotation scopeAnnotation = jClass.getAnnotation(BeanPropertyRules.SCOPE);

        if (scopeAnnotation != null && scopeAnnotation.hasElement(AnnotationElementKeys.VALUE)) {
            String scopeValue = Objects.requireNonNull(scopeAnnotation.getElement(AnnotationElementKeys.VALUE)).toString();
            beanClass.setScope(BeanScope.fromValue(scopeValue));
        } else {
            beanClass.setScope(BeanScope.SINGLETON); // 默认单例
        }
    }

    /**
     * 处理是否为Primary Bean
     */
    private static void processIsPrimary(BeanClass beanClass) {
        JClass jClass = beanClass.getjClass();
        Annotation primaryAnnotation = jClass.getAnnotation(BeanPropertyRules.PRIMARY);
        beanClass.setPrimary(primaryAnnotation != null);
    }

    /**
     * 处理是否延迟加载
     */
    private static void processIsLazy(BeanClass beanClass) {
        JClass jClass = beanClass.getjClass();
        Annotation lazyAnnotation = jClass.getAnnotation(BeanPropertyRules.LAZY);

        if (lazyAnnotation != null) {
            if (lazyAnnotation.hasElement(AnnotationElementKeys.VALUE)) {
                String lazyValue = Objects.requireNonNull(lazyAnnotation.getElement(AnnotationElementKeys.VALUE)).toString();
                beanClass.setLazy(Boolean.parseBoolean(lazyValue));
            } else {
                beanClass.setLazy(true); // @Lazy 默认为 true
            }
        } else {
            beanClass.setLazy(false); // 默认不延迟加载
        }
    }

    /**
     * 初始化该类实现的接口
     */
    private static void processInterfaces(BeanClass beanClass) {
        JClass jClass = beanClass.getjClass();
        Collection<JClass> interfaces = jClass.getInterfaces();

        Collection<JClass> immutableInterfaces = Set.copyOf(interfaces);

        beanClass.setInterfaces(immutableInterfaces);
    }

    /**
     * 初始化该类的父类
     */
    private static void processSuperClass(BeanClass beanClass) {
        JClass jClass = beanClass.getjClass();
        JClass superClass = jClass.getSuperClass();
        beanClass.setSuperClass(superClass);
    }

    /**
     * TODO: 如果是接口类型，说明是@Mapper需要根据IR手动模拟实现类
     * @param beanClass beanClass
     */
    private static void processImplementations(BeanClass beanClass) {

    }

}
