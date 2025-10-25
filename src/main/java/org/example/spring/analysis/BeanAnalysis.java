package org.example.spring.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.spring.analysis.di.bean.BeanClass;
import org.example.spring.analysis.di.bean.BeanClassInitializer;
import org.example.spring.analysis.di.bean.BeanInfo;
import org.example.spring.rules.BeanComponentsRules;
import org.example.spring.rules.BeanConfigRules;
import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Return;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;

import java.util.*;

public class BeanAnalysis extends ProgramAnalysis {

    public static final String ID = "beanAnalysis";

    private static final Logger logger = LogManager.getLogger(BeanAnalysis.class);

    private final Set<BeanInfo> beanInfoSet = new HashSet<>();

    private final Collection<BeanClass> beans = new HashSet<>();

    private final List<JClass> applicationClasses;

    private static final List<String> BEAN_COMPONENTS_ANNOTATIONS = List.of(
        BeanComponentsRules.CONTROLLER,
        BeanComponentsRules.REST_CONTROLLER,
        BeanComponentsRules.COMPONENT,
        BeanComponentsRules.SERVICE,
        BeanComponentsRules.JSR330_NAMED
    );

    public BeanAnalysis(AnalysisConfig config) {
        super(config);
        applicationClasses = World.get()
            .getClassHierarchy()
            .applicationClasses()
            .toList();
    }

    @Override
    public Object analyze() {
//        extractBeans();
//        for(BeanInfo beanInfo : beanInfoSet){
//            System.out.println(beanInfo.toString());
//        }
        extractBean();
        return Set.of(beans);
    }

    /**
     * 提取 Bean
     */
    private void extractBean() {
        applicationClasses.forEach(appClass -> {
            boolean isBeanComponent = BEAN_COMPONENTS_ANNOTATIONS.stream()
                .anyMatch(appClass::hasAnnotation);

            if (isBeanComponent) {
                BeanClass beanClass = BeanClassInitializer.createBeanClass(appClass);
                beans.add(beanClass);
            }

            if (appClass.hasAnnotation(BeanConfigRules.CONFIGURATION)) {
                processConfigurationClass(appClass);
            }
        });
    }

    /**
     * 处理带有 {@code @Configuration} 注解的类中的 {@code @Bean} 方法。
     * <p>
     * 这种情况需要特殊处理，因为 {@code @Bean} 方法可能返回接口或抽象类类型，
     * 而实际注入时需要依赖其具体实现类。例如：
     * <pre>{@code
     * @Configuration
     * public class Config {
     *     @Bean
     *     public ConfigurationBean configurationBean() {
     *         return new ConfigurationBean();  // 返回具体类
     *     }
     *
     *     @Bean
     *     public ServiceBean serviceBean() {
     *         return new ServiceBeanImpl1();  // 返回接口的实现类
     *     }
     * }
     * }</pre>
     * 本处理器通过解析方法体中的实际实例化类型来解决类型不明确问题，
     * 确保依赖注入时能正确关联具体实现。
     *
     * @param appClass 待处理的配置类
     */
    private void processConfigurationClass(JClass appClass) {
        appClass.getDeclaredMethods().forEach(method -> {
            if (method.hasAnnotation(BeanConfigRules.BEAN)) {
                if (method.hasAnnotation(BeanConfigRules.BEAN)) {
                    Collection<JClass> jClasses = processBeanMethod(method);
                    jClasses.forEach(jClass -> {
                        BeanClass beanClass = BeanClassInitializer.createBeanFromMethod(method, jClass);
                        beans.add(beanClass);
                    });
                }
            }
        });
    }

    private Collection<JClass> processBeanMethod(JMethod jMethod) {
        Set<JClass> actualTypes = new HashSet<>();
        Type returnType = jMethod.getReturnType();
        // 方法的返回类型(声明类型)
        String declaredType = returnType.getName();
        JClass aClass = World.get().getClassHierarchy().getClass(declaredType);
        if (aClass == null) {
            logger.error("Return type not found in class hierarchy: {}", declaredType);
            return actualTypes;
        }
        actualTypes.addAll(analyzeActualReturnTypes(jMethod));
        return actualTypes;
    }

    /**
     * 分析方法的实际返回类型
     * @param method 待分析的方法
     * @return 实际返回类型集合
     */
    private Set<JClass> analyzeActualReturnTypes(JMethod method) {
        Set<JClass> actualTypes = new HashSet<>();
        IR ir = method.getIR();

        if (ir == null) {
            return actualTypes;
        }

        // 分析 IR 中的 return 语句
        ir.getStmts().forEach(stmt -> {
            if (stmt instanceof Return returnStmt) {
                Var returnVar = returnStmt.getValue();
                if (returnVar != null) {
                    Type type = returnVar.getType();
                    if (type instanceof ClassType classType) {
                        actualTypes.add(classType.getJClass());
                    }
                }
            }
        });

        return actualTypes;
    }




//    private void extractBeans() {
//        World world = World.get();
//        Stream<JClass> jClassStream = world.getClassHierarchy().allClasses();
//
//        for (JClass jClass : jClassStream.toList()) {
//            // 查找带有 @Component、@Service、@Controller、@Repository 注解的类
//            if (SpringUtils.isSpringBean(jClass)) {
//                String fromAnnotationName = getBeanNameFromAnnotations(jClass);
//                BeanInfo beanInfo = new BeanInfo(jClass, fromAnnotationName);
//                beanInfoSet.add(beanInfo);
//            }
//
//            // 查找带有 @Configuration 注解的类并处理 @Bean 方法
//            if (jClass.hasAnnotation(BeanAnnotationRules.Configuration.getType())) {
//                Collection<JMethod> methods = jClass.getDeclaredMethods();
//                for (JMethod method : methods) {
//                    if (method.hasAnnotation(BeanAnnotationRules.Bean.getType())) {
//                        Annotation annotation = method.getAnnotation(BeanAnnotationRules.Bean.getType());
//                        if (annotation != null) {
//                            String fromAnnotationName = null;
//                            if (annotation.hasElement("value")) {
//                                fromAnnotationName = Objects.requireNonNull(annotation.getElement("value")).toString();
//                            }
//                            JClass beanClass = world.getClassHierarchy().getClass(method.getReturnType().getName());
////                            System.out.println(beanClass);
//                            if (beanClass != null) {
//                                if (beanClass.isInterface()) {
//                                    Collection<JClass> directImplementorsOf = world.getClassHierarchy().getDirectImplementorsOf(beanClass);
//                                    for (JClass implementor : directImplementorsOf) {
//                                        if (!implementor.isAbstract()) {
//                                            BeanInfo beanInfo = new BeanInfo(implementor, fromAnnotationName);
//                                            beanInfoSet.add(beanInfo);
//                                        }
//                                    }
//                                } else {
//                                    Collection<JClass> subclasses = world.getClassHierarchy().getAllSubclassesOf(beanClass);
//                                    for (JClass subclass : subclasses) {
//                                        if (!subclass.isAbstract()) {
//                                            BeanInfo beanInfo = new BeanInfo(subclass, fromAnnotationName);
//                                            beanInfoSet.add(beanInfo);
//                                        }
//                                    }
//                                }
////                                BeanInfo beanInfo = new BeanInfo(beanClass, fromAnnotationName);
////                                beanInfoSet.add(beanInfo);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    /**
//     * 获取类注解上指定的名称，如果没有指定名称，则返回 null
//     */
//    private String getBeanNameFromAnnotations(JClass jClass) {
//        Annotation componentAnnotation = jClass.getAnnotation(BeanAnnotationRules.Component.getType());
//        Annotation serviceAnnotation = jClass.getAnnotation(BeanAnnotationRules.Service.getType());
//        Annotation controllerAnnotation = jClass.getAnnotation(BeanAnnotationRules.Controller.getType());
//        Annotation repositoryAnnotation = jClass.getAnnotation(BeanAnnotationRules.Repository.getType());
//
//        // 检查所有相关注解的 "value" 元素
//        if (componentAnnotation != null && componentAnnotation.hasElement("value")) {
//            return Objects.requireNonNull(componentAnnotation.getElement("value")).toString();
//        }
//        if (serviceAnnotation != null && serviceAnnotation.hasElement("value")) {
//            return Objects.requireNonNull(serviceAnnotation.getElement("value")).toString();
//        }
//        if (controllerAnnotation != null && controllerAnnotation.hasElement("value")) {
//            return Objects.requireNonNull(controllerAnnotation.getElement("value")).toString();
//        }
//        if (repositoryAnnotation != null && repositoryAnnotation.hasElement("value")) {
//            return Objects.requireNonNull(repositoryAnnotation.getElement("value")).toString();
//        }
//
//        return null;
//    }

}