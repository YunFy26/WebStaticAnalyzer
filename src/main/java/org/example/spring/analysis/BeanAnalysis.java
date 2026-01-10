package org.example.spring.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.spring.analysis.di.bean.BeanClass;
import org.example.spring.analysis.di.bean.BeanClassInitializer;
import org.example.spring.rules.*;
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

    private final Collection<BeanClass> beans = new HashSet<>();

    private final List<JClass> applicationClasses;

    private static final List<String> BEAN_COMPONENTS_ANNOTATIONS = List.of(
        BeanComponentsRules.CONTROLLER,
        BeanComponentsRules.REST_CONTROLLER,
        BeanComponentsRules.COMPONENT,
        BeanComponentsRules.SERVICE,
        BeanComponentsRules.REPOSITORY,
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
//        for (var bean : beans) {
//            System.out.println(bean.toString());
//        }
        return beans;
    }

    /**
     * 提取 Bean
     * <p>
     *     <li>spring bean</li>
     *     <li>configuration class 中的 @Bean 方法</li>
     *     <li>JPA Entity</li> TODO
     *     <li>Mybatis</li> TODO
     *     <li>Quartz（定时器）</li> TODO
     *     <li>Shiro</li> TODO
     * </p>
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

            // MyBatis Mapper 接口
            if (appClass.hasAnnotation(MyBatisRules.MAPPER) || appClass.getSimpleName().endsWith("Mapper")) {
                BeanClass beanClass = BeanClassInitializer.createBeanClass(appClass);
                beans.add(beanClass);
            }

//            // JPA Entity
//            if (appClass.hasAnnotation(JPARules.JAKARTA_ENTITY) || appClass.hasAnnotation(JPARules.JAVAX_ENTITY)) {
//                BeanClass beanClass = BeanClassInitializer.createBeanClass(appClass);
//                beans.add(beanClass);
//            }

//            // Quartz  TODO
//            JClass quartzJobType = World.get().getClassHierarchy().getClass(QuartzRules.JOB);
//            if (quartzJobType != null && World.get().getClassHierarchy().isSubclass(appClass, quartzJobType)) {
//                beans.add(BeanClassInitializer.createBeanClass(appClass));
//            }
//            // Shiro  TODO
//            JClass shiroRealmType = World.get().getClassHierarchy().getClass(ShiroRules.REALM);
//            if (shiroRealmType != null && World.get().getClassHierarchy().isSubclass(appClass, shiroRealmType)) {
//                beans.add(BeanClassInitializer.createBeanClass(appClass));
//            }
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
                Collection<JClass> jClasses = processBeanMethod(method);
                jClasses.forEach(jClass -> {
                    BeanClass beanClass = BeanClassInitializer.createBeanFromMethod(method, jClass);
                    beans.add(beanClass);
                });
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
}