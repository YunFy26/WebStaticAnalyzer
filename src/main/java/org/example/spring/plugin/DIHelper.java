package org.example.spring.plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.spring.analysis.di.bean.BeanClass;
import org.example.spring.analysis.di.bean.BeanScope;
import org.example.spring.analysis.di.injectpoints.LoadFieldPoint;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class DIHelper {

    private static final Logger logger = LogManager.getLogger(DIHelper.class);

    private DIHelper() {}

    /**
     * 处理接口类型注入
     * TODO: 抛出异常
     * @param beans 所有Bean集合
     * @param interfaceType 接口类型
     */
    public static void processInterface(Solver solver,
                                        CSMethod csMethod,
                                        Context context,
                                        LoadFieldPoint loadFieldPoint,
                                        JField jField,
                                        Collection<BeanClass> beans,
                                        JClass interfaceType) {
        List<BeanClass> implBeans = getInterfaceImpl(beans, interfaceType);
        if (implBeans.isEmpty()) {
            logger.warn("No implementation found for interface: {}", interfaceType.getName());
            return;
        }
        if (implBeans.size() == 1) {
            BeanClass bean = implBeans.get(0);
            processSingleBean(solver, csMethod, context, loadFieldPoint, jField, bean);
        } else {
            processMutiBeans(solver, csMethod, context, loadFieldPoint, jField, implBeans);
        }
    }

    public static void processAbstract(Solver solver,
                                       CSMethod csMethod,
                                       Context context,
                                       LoadFieldPoint loadFieldPoint,
                                       JField jField,
                                       Collection<BeanClass> beans,
                                       JClass abstractClass) {
        List<BeanClass> subBeans = getAbstractSub(beans, abstractClass);
        if (subBeans.isEmpty()) {
            logger.warn("No subclass found for abstract class: {}", abstractClass.getName());
            return;
        }
        if (subBeans.size() == 1) {
            BeanClass bean = subBeans.get(0);
            processSingleBean(solver, csMethod, context, loadFieldPoint, jField, bean);
        } else {
            processMutiBeans(solver, csMethod, context, loadFieldPoint, jField, subBeans);
        }
    }

    public static void processInstance(Solver solver,
                                       CSMethod csMethod,
                                       Context context,
                                       LoadFieldPoint loadFieldPoint,
                                       JField jField,
                                       Collection<BeanClass> beans,
                                       JClass concreteClass) {
        BeanClass bean = getInstance(beans, concreteClass);
        if (bean == null) {
            logger.warn("No bean found for concrete class: {}", concreteClass.getName());
        }else {
            processSingleBean(solver, csMethod, context, loadFieldPoint, jField, bean);
        }
    }

    /**
     * 处理接口类型注入
     */
    private static List<BeanClass> getInterfaceImpl(Collection<BeanClass> beans,
                                                      JClass interfaceType) {
        // 查找接口实现类
        return beans.stream()
            .filter(bean -> bean.getInterfaces().contains(interfaceType))
            .toList();
    }

    /**
     * 处理抽象类类型注入
     */
    private static List<BeanClass> getAbstractSub(Collection<BeanClass> beans,
                                                          JClass abstractClass) {
        // 查找抽象类的子类
        return beans.stream()
            .filter(bean -> abstractClass.equals(bean.getSuperClass()))
            .toList();
    }

    /**
     * 处理具体类类型注入
     */
    private static BeanClass getInstance(Collection<BeanClass> beans,
                                                          JClass concreteClass) {
        return beans.stream()
            .filter(bean -> bean.getjClass().equals(concreteClass))
            .findFirst()
            .orElse(null);
    }

    private static void processSingleBean(Solver solver,
                                          CSMethod csMethod,
                                          Context context,
                                          LoadFieldPoint loadFieldPoint,
                                          JField jField,
                                          BeanClass beanClass) {
        addBeanToVarPointSet(solver, csMethod, context, loadFieldPoint, jField, beanClass);
    }

    private static void processMutiBeans(Solver solver,
                                         CSMethod csMethod,
                                         Context context,
                                         LoadFieldPoint loadFieldPoint,
                                         JField jField,
                                         List<BeanClass> beans) {
        String fieldName = jField.getName();
        BeanClass targetBean = findTargetBean(beans, fieldName);
        if (targetBean != null) {
            processSingleBean(solver, csMethod, context, loadFieldPoint, jField, targetBean);
        }else {
            beans.forEach(beanClass -> {
                processSingleBean(solver, csMethod, context, loadFieldPoint, jField, beanClass);
            });
        }
    }

    /**
     * 从多个候选 Bean 中找到目标 Bean
     */
    private static BeanClass findTargetBean(List<BeanClass> candidateBeans, String fieldName) {
        // 1. 查找 @Primary 标注的 Bean
        // 2. 查找名称匹配的 Bean
        Optional<BeanClass> primaryBean = candidateBeans.stream()
            .filter(BeanClass::isPrimary)
            .findFirst();
        return primaryBean.orElseGet(() -> candidateBeans.stream()
            .filter(bean -> fieldName.equals(bean.getDeclaredBeanName()))
            .findFirst()
            .orElse(null));
    }

    /**
     * 创建对象并添加指向关系
     * TODO：考虑作用域？
     */
    private static void addBeanToVarPointSet(Solver solver,
                                             CSMethod csMethod,
                                             Context context,
                                             LoadFieldPoint loadFieldPoint,
                                             JField jField,
                                             BeanClass beanClass) {
        HeapModel heapModel = solver.getHeapModel();
        ContextSelector contextSelector = solver.getContextSelector();

        JClass implClass = beanClass.getjClass();
        JMethod jMethod = csMethod.getMethod();

        Obj obj = heapModel.getMockObj(
            () -> "DependencyInjectedBean",
            jMethod + ":" + jField.getName(),
            implClass.getType()
        );

        Context heapContext = contextSelector.selectHeapContext(csMethod, obj);

        // 为注入点的所有变量添加指向
        loadFieldPoint.getVars().forEach(var -> {
            solver.addVarPointsTo(context, var, heapContext, obj);
        });
    }

    /**
     * 处理方法中的this调用
     */
    public static void processThis(Solver solver, CSMethod csMethod, Context context, Var thisVar) {
        HeapModel heapModel = solver.getHeapModel();
        ContextSelector contextSelector = solver.getContextSelector();
        JClass jClass = csMethod.getMethod().getDeclaringClass();

        // 使用与 ProcessEntryPlugin 相同的 allocation site 策略
        String allocSite = "EntryPoint{this}";

        Obj thisObj = heapModel.getMockObj(
            Descriptor.ENTRY_DESC,
            allocSite,
            jClass.getType(),
            csMethod.getMethod()
        );

        Context heapContext = contextSelector.selectHeapContext(csMethod, thisObj);
        solver.addVarPointsTo(context, thisVar, heapContext, thisObj);
    }

}
