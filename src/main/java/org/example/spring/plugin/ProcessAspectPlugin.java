package org.example.spring.plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.printer.aop.AOPEdgesToFile;
import org.example.spring.analysis.AspectAnalysis;
import org.example.spring.analysis.aop.AspectClass;
import org.example.spring.analysis.aop.AspectMethod;
import org.example.spring.analysis.aop.Pointcut;
import org.example.spring.analysis.di.MockObjDescriptor;
import org.example.spring.analysis.di.bean.BeanClass;
import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * AOP 切面织入插件
 * 在方法被添加到调用图时,检查是否匹配切点表达式,如果匹配则织入通知方法
 */
public class ProcessAspectPlugin implements Plugin {

    private static final Logger logger = LogManager.getLogger(ProcessAspectPlugin.class);

    private Solver solver;

    private HeapModel heapModel;

    private ClassHierarchy hierarchy;

    // 从 AspectAnalysis 中获取所有已解析的切面类
    private final List<AspectClass> aspects = World.get().getResult(AspectAnalysis.ID);

    // 避免重复处理同一个方法
    private final Map<JMethod, Boolean> processedMethods = new HashMap<>();

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        this.heapModel = solver.getHeapModel();
        this.hierarchy = solver.getHierarchy();
    }

    @Override
    public void onStart() {

        initGlobalAspectObjects();

    }

    private void initGlobalAspectObjects() {
        int count = 0;
        logger.info("Initializing global aspect mock objects...");
        for (AspectClass aspect : aspects) {
            heapModel.getMockObj(
                MockObjDescriptor.ASPECT_OBJ,
                aspect.getjClass().getName(),
                aspect.getjClass().getType()
            );
            count++;
        }
        logger.info("Initialized {} global aspect objects.", count);
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        JMethod targetMethod = csMethod.getMethod();

        if (processedMethods.containsKey(targetMethod)) {
            return;
        }
        processedMethods.put(targetMethod, true);

        // 遍历所有切面类
        for (AspectClass aspect : aspects) {
            weaveAspectForMethod(csMethod, aspect);
        }
    }

    /**
     * 为指定方法织入切面
     * 遍历切面的所有切点,检查方法是否匹配,如果匹配则织入通知方法
     */
    private void weaveAspectForMethod(CSMethod csMethod, AspectClass aspect) {
        JMethod currMethod = csMethod.getMethod();
        Context context = csMethod.getContext();
        CSManager csManager = solver.getCSManager();

        // 遍历切面中的所有切点
        for (var entry : aspect.getPointcutMethodMap().entrySet()) {
            Pointcut pointcut = entry.getKey();
            List<AspectMethod> aspectMethods = entry.getValue();

            // 使用 PointcutMatcher 检查当前方法是否匹配切点
            if (!PointcutMatcher.matches(pointcut, currMethod, aspect)) {
                continue;
            }

//            logger.info("方法 {} 匹配切点: {}",
//                currMethod.getSignature(), pointcut.getExpression());

            // 为匹配的切点织入所有通知方法
            for (AspectMethod aspectMethod : aspectMethods) {
                weaveAdvice(csMethod, aspectMethod, aspect, context, csManager);
            }
        }
    }
    /**
     * 织入通知方法
     * 创建虚拟调用边,将通知方法加入调用图
     */
    private void weaveAdvice(CSMethod csTargetMethod, AspectMethod aspectMethod,
                             AspectClass aspect, Context context, CSManager csManager) {
        JMethod adviceMethod = aspectMethod.getMethod();

//        logger.info("织入切面: {} -> 通知类型: {} -> 目标方法: {}",
//            aspect.getjClass().getName(),
//            aspectMethod.getAdviceType(),
//            csTargetMethod.getMethod().getSignature());

        // 创建虚拟调用点
        CSCallSite virtualCallSite = createVirtualCallSite(csTargetMethod, aspectMethod, context, csManager);

        // 选择上下文并创建 CSMethod
        Context adviceContext = solver.getContextSelector().selectContext(virtualCallSite, adviceMethod);
        CSMethod csAdviceMethod = csManager.getCSMethod(adviceContext, adviceMethod);

        // 添加 AOP 调用边
        AOPEdge aopEdge = new AOPEdge(virtualCallSite, csAdviceMethod, aspectMethod.getAdviceType());
        provideEntryObjectsForAdvice(csAdviceMethod, aopEdge);
        solver.addCallEdge(aopEdge);
//        Set<CSCallSite> callersOf = solver.getCallGraph().getCallersOf(csTargetMethod);
//
//        solver.addCSMethod(csAdviceMethod);
    }


    /**
     * 创建虚拟调用点
     * 用于表示从目标方法到通知方法的调用关系
     */
    private CSCallSite createVirtualCallSite(CSMethod csTargetMethod, AspectMethod aspectMethod,
                                             Context context, CSManager csManager) {
        Invoke virtualInvoke = AOPInvokeFactory.createVirtualInvoke(
            csTargetMethod.getMethod(), aspectMethod);
        return csManager.getCSCallSite(context, virtualInvoke);
    }

//    @Override
//    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
//        if (edge instanceof AOPEdge aopEdge) {
//            logger.info("✓ 处理 AOP 调用边: {} -> {}, 通知类型: {}",
//                edge.getCallSite().getCallSite().getContainer().getSignature(),
//                edge.getCallee().getMethod().getSignature(),
//                aopEdge.getAdviceType());
//        }
//    }

    /**
     * 为通知方法提供入口对象
     */
    private void provideEntryObjectsForAdvice(CSMethod csAdviceMethod, AOPEdge aopEdge) {
        JMethod adviceMethod = csAdviceMethod.getMethod();
        Context context = csAdviceMethod.getContext();
        IR ir = adviceMethod.getIR();

        // --- 修复 1: Aspect 对象应该是单例的 ---
        if (!adviceMethod.isStatic()) {
            Obj aspectObj = solver.getHeapModel().getMockObj(
                MockObjDescriptor.ASPECT_OBJ,
                adviceMethod.getDeclaringClass().getName(),
                adviceMethod.getDeclaringClass().getType()
            );
            solver.addVarPointsTo(context, ir.getThis(), context, aspectObj);
        }

        // --- 修复 2: JoinPoint 对象应该随拦截点(CallSite)变化 ---
        if (adviceMethod.getParamCount() > 0) {
            for (int i = 0; i < adviceMethod.getParamCount(); i++) {
                Var param = ir.getParam(i);
                Type paramType = param.getType();

                if (isJoinPointType(paramType)) {
                    String concreteType = RuntimeImplementationMapper.getConcreteType(paramType.getName());
                    JClass aClass = hierarchy.getClass(concreteType);
                    Type typeToUse = (aClass != null) ? aClass.getType() : paramType;

                    // 使用调用点（CallSite）的信息作为 Key
                    // 这样不同的业务方法调用同一个切面时，JoinPoint 对象是不同的
                    String callSiteStr = aopEdge.getCallSite().getCallSite().toString();
                    String joinPointAllocKey = "JoinPoint:" + callSiteStr;

                    Obj joinPointObj = solver.getHeapModel().getMockObj(
                        () -> "JoinPointObj:" + callSiteStr,
                        joinPointAllocKey, // [修改] key 包含 CallSite 信息，区分不同拦截
                        typeToUse,
                        adviceMethod
                    );

                    solver.addVarPointsTo(context, param, context, joinPointObj);
                }
            }
        }
    }

    /**
     * 检查类型是否为 JoinPoint 相关类型
     */
    private boolean isJoinPointType(Type type) {
        if (!(type instanceof ClassType classType)) {
            return false;
        }
        String typeName = classType.getName();
        return typeName.equals("org.aspectj.lang.JoinPoint") ||
            typeName.equals("org.aspectj.lang.ProceedingJoinPoint");
    }

    @Override
    public void onFinish() {
        CallGraph<CSCallSite, CSMethod> callGraph = solver.getCallGraph();
        AOPEdgesToFile.writeAOPEdgesToFile(callGraph);
    }
}
