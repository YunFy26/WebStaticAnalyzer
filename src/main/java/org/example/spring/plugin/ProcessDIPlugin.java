package org.example.spring.plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.spring.analysis.BeanAnalysis;
import org.example.spring.analysis.InjectPointsAnalysis;
import org.example.spring.analysis.RouterAnalysis;
import org.example.spring.analysis.di.MockObjDescriptor;
import org.example.spring.analysis.di.bean.BeanClass;
import org.example.spring.analysis.di.injectpoints.LoadFieldPoint;
import org.example.spring.analysis.router.ControllerClass;
import pascal.taie.World;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeInstanceExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ProcessDIPlugin implements Plugin {

    private final Logger logger = LogManager.getLogger(ProcessDIPlugin.class);

    private Solver solver;

    private HeapModel heapModel;

    private ClassHierarchy hierarchy;

    private CSManager csManager;

    private ContextSelector contextSelector;

    private final List<ControllerClass> routerAnalysis = World.get().getResult(RouterAnalysis.ID);

    private final Collection<BeanClass> beans = World.get().getResult(BeanAnalysis.ID);

    private final Map<BeanClass, Map<JMethod, List<LoadFieldPoint>>> injectPoints =
        World.get().getResult(InjectPointsAnalysis.ID);

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        this.heapModel = solver.getHeapModel();
        this.hierarchy = solver.getHierarchy();
        this.csManager = solver.getCSManager();
        this.contextSelector = solver.getContextSelector();
    }

    @Override
    public void onStart() {

        initGlobalBeanObjects();

    }

    private void initGlobalBeanObjects() {
        AtomicInteger count = new AtomicInteger();
        logger.info("Initializing global bean mock objects for beans...");
        for (BeanClass bean : beans) {
            if (!bean.isInterface()) {
                heapModel.getMockObj(
                    MockObjDescriptor.DI_OBJ,
                    bean.getjClass().getName(),
                    bean.getjClass().getType()
                );
                count.getAndIncrement();
            }else {
                bean.getImplementations().forEach(jClass -> {
                    heapModel.getMockObj(
                        MockObjDescriptor.DI_OBJ,
                        jClass.getName(),
                        jClass.getType()
                    );
                    count.getAndIncrement();
                });
            }
        }
        logger.info("Initialized {} global bean objects.", count);
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        JMethod jMethod = csMethod.getMethod();
        JClass jClass = jMethod.getDeclaringClass();
        Context context = csMethod.getContext();
        if (isJdkCalls(jMethod)) {
            solver.addIgnoredMethod(csMethod.getMethod());
            return;
        }
        // 查找当前方法所在的 BeanClass
        Optional<BeanClass> beanClassOpt = injectPoints.keySet().stream()
            .filter(beanClass -> beanClass.getjClass().equals(jClass))
            .findFirst();

        if (beanClassOpt.isEmpty()) {
            return;
        }

        BeanClass beanClass = beanClassOpt.get();
        Map<JMethod, List<LoadFieldPoint>> methodFieldPoints = injectPoints.get(beanClass);

        if (!methodFieldPoints.containsKey(jMethod)) {
            return;
        }

        // 处理当前方法中局部变量LoadField的情况
        List<LoadFieldPoint> fieldPoints = methodFieldPoints.get(jMethod);
        fieldPoints.forEach(loadFieldPoint -> {
            JField jField = loadFieldPoint.getjField();
            String typeName = jField.getType().getName();
            JClass fieldType = hierarchy.getClass(typeName);

            if (fieldType == null) {
                logger.warn("Cannot find class for type: {}", typeName);
                return;
            }
            if (isJdkClass(fieldType)) {
                return;
            }
            if (fieldType.isInterface()) {
                DIHelper.processInterface(solver, csMethod, context, loadFieldPoint, jField, beans, fieldType);
            }else if (fieldType.isAbstract()) {
                DIHelper.processAbstract(solver, csMethod, context, loadFieldPoint, jField, beans, fieldType);
            }else {
                DIHelper.processInstance(solver, csMethod, context, loadFieldPoint, jField, beans, fieldType);
            }
        });

        // 处理 this 变量：只为实例方法中的 this 调用添加指向
        // 注意：这里不创建新的 this 对象，而是使用已存在的对象
        Var thisVar = findThisVar(jMethod);
        if (thisVar != null) {
            // 检查是否已经有 this 对象的指向
            PointsToSet pointsToSet = csManager.getCSVar(context, thisVar).getPointsToSet();
            if (pointsToSet == null || pointsToSet.isEmpty()) {
                // 只有当 this 还没有指向任何对象时，才处理
                DIHelper.processThis(solver, csMethod, context, thisVar);
            }
        }
    }

    /**
     * 查找方法中的this变量
     */
    private Var findThisVar(JMethod method) {
        for (Stmt stmt : method.getIR().getStmts()) {
            if (stmt instanceof Invoke invoke &&
                (invoke.isInterface() || invoke.isVirtual())) {
                InvokeExp invokeExp = invoke.getRValue();
                if (invokeExp instanceof InvokeInstanceExp invokeInstanceExp) {
                    Var base = invokeInstanceExp.getBase();
                    if ("%this".equals(base.getName())) {
                        return base;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void onUnresolvedCall(CSObj recv, Context context, Invoke invoke) {
//        logger.info("Recv is : {}, Unresolved call: {}", recv, invoke);

    }

    private boolean isJdkCalls(JMethod jMethod) {
        String packageName = jMethod.getDeclaringClass().getName();
        return packageName.startsWith("java.") ||
                packageName.startsWith("javax.") ||
                packageName.startsWith("sun.") ||
                packageName.startsWith("com.sun.") ||
                packageName.startsWith("jdk.") ||
                packageName.startsWith("org.w3c.dom");
    }

    private boolean isJdkClass(JClass jClass) {
        String packageName = jClass.getName();
        return packageName.startsWith("java.") ||
            packageName.startsWith("javax.") ||
            packageName.startsWith("sun.") ||
            packageName.startsWith("com.sun.") ||
            packageName.startsWith("jdk.") ||
            packageName.startsWith("org.w3c.dom");
    }
}
