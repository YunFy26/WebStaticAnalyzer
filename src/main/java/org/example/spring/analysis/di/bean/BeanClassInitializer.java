package org.example.spring.analysis.di.bean;

import org.example.enums.AnnotationElementKeys;
import org.example.spring.plugin.RuntimeImplementationMapper;
import org.example.spring.rules.BeanComponentsRules;
import org.example.spring.rules.BeanPropertyRules;
import org.example.utils.StringUtils;
import pascal.taie.World;
import pascal.taie.ir.IR;
import pascal.taie.ir.IRBuildHelper;
import pascal.taie.ir.IRBuilder;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Modifier;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

/**
 * BeanClass初始化器
 */
public class BeanClassInitializer {

    /**
     * 创建BeanClass实例
     * @param jClass 匹配Bean注解规则的类
     * @return BeanClass实例
     */
    public static BeanClass createBeanClass(JClass jClass) {
        BeanClass beanClass = new BeanClass(jClass);
        // 对非接口类型的类，进行初始化
        initialize(beanClass);
        // 对于Mapper接口，要手动模拟其实现类
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
     * TODO: 如果是Mapper接口，由于是动态生成其实现类，要手动模拟
     * @param beanClass beanClass
     */
    private static void processImplementations(BeanClass beanClass) {
        JClass jClass = beanClass.getjClass();
        JClass aClass = World.get().getClassHierarchy().getClass(jClass.getName());

        if (jClass.isInterface()) {
            Collection<JMethod> declaredMethods = jClass.getDeclaredMethods();

            // =======================================================
            // 1. 处理方法 (JMethod)
            // JMethod 继承自 ClassMember，修饰符在 ClassMember 中定义
            // =======================================================
            try {
                // 获取 JMethod 的 ir 字段
                Field irField = JMethod.class.getDeclaredField("ir");
                irField.setAccessible(true);

                // 获取 ClassMember 的 modifiers 字段 (供 JMethod 使用)
                Field memberModifiersField = pascal.taie.language.classes.ClassMember.class.getDeclaredField("modifiers");
                memberModifiersField.setAccessible(true);

                for (JMethod jMethod : declaredMethods) {
                    // A. 注入空 IR
                    IR stmts = new IRBuildHelper(jMethod).buildEmpty();
                    irField.set(jMethod, stmts);

                    // B. 修改方法修饰符 (移除 ABSTRACT)
                    @SuppressWarnings("unchecked")
                    Set<Modifier> currentModifiers = (Set<Modifier>) memberModifiersField.get(jMethod);

                    // 拷贝并修改
                    Set<Modifier> newModifiers = new HashSet<>(currentModifiers);
                    newModifiers.remove(Modifier.ABSTRACT);
                    newModifiers.remove(Modifier.NATIVE); // 同时也建议移除 NATIVE

                    // 回写
                    memberModifiersField.set(jMethod, Collections.unmodifiableSet(newModifiers));
                }
            } catch (Exception e) {
                throw new RuntimeException("修改 JMethod 失败", e);
            }
        }

        if (jClass.isInterface()) {
            try {
                // 1. 获取 JClass 类中的 'modifiers' 字段
                Field modifiersField = JClass.class.getDeclaredField("modifiers");
                // 2. 暴力设置为可访问
                modifiersField.setAccessible(true);

                // 3. 获取当前的修饰符集合
                @SuppressWarnings("unchecked")
                Set<Modifier> currentModifiers = (Set<Modifier>) modifiersField.get(jClass);

                // 4. 创建一个新的可变集合（因为原集合可能是不可变的，如 Collections.unmodifiableSet）
                Set<Modifier> newModifiers = new HashSet<>(currentModifiers);

                // 5. 【核心步骤】移除 INTERFACE 修饰符
                newModifiers.remove(Modifier.INTERFACE);

                // 6. 【建议步骤】移除 ABSTRACT 修饰符
                // 因为你已经为方法构建了 Body（IR），说明它不再是一个抽象的接口，而是一个具体的类
                newModifiers.remove(Modifier.ABSTRACT);

                // 7. 将修改后的集合写回 JClass 实例
                // 为了保持规范，可以再次包裹为不可变集合，也可以直接塞进去
                modifiersField.set(jClass, Collections.unmodifiableSet(newModifiers));

                // 验证一下
                // System.out.println("成功将 " + jClass.getName() + " 修改为普通类，IsInterface: " + jClass.isInterface());

            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException("修改 JClass 修饰符失败", e);
            }
        }
    }
}
