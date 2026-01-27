package org.example.spring.plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.printer.aop.AOPEdgesToFile;
import org.example.spring.analysis.AspectAnalysis;
import org.example.spring.analysis.aop.AspectClass;
import org.example.spring.analysis.aop.AspectMethod;
import org.example.spring.analysis.aop.Pointcut;
import org.example.spring.analysis.di.MockObjDescriptor;
import pascal.taie.World;
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

/**
 * AOP 切面织入插件
 */
public class ProcessAspectPlugin implements Plugin {

    private static final Logger logger = LogManager.getLogger(ProcessAspectPlugin.class);

    private Solver solver;
    private HeapModel heapModel;
    private ClassHierarchy hierarchy;

    private final List<AspectClass> aspects = World.get().getResult(AspectAnalysis.ID);
    private final Map<JMethod, Boolean> processedMethods = new HashMap<>();

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        this.heapModel = solver.getHeapModel();
        this.hierarchy = solver.getHierarchy();
    }

    @Override
    public void onStart() {
        logger.info("Initializing global aspect mock objects...");
        for (AspectClass aspect : aspects) {
            heapModel.getMockObj(
                MockObjDescriptor.ASPECT_OBJ,
                aspect.getjClass().getName(),
                aspect.getjClass().getType()
            );
        }
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        JMethod targetMethod = csMethod.getMethod();

        if (processedMethods.containsKey(targetMethod)) {
            return;
        }
        processedMethods.put(targetMethod, true);

        for (AspectClass aspect : aspects) {
            weaveAspectForMethod(csMethod, aspect);
        }
    }

    private void weaveAspectForMethod(CSMethod csMethod, AspectClass aspect) {
        JMethod currMethod = csMethod.getMethod();
        Context context = csMethod.getContext();
        CSManager csManager = solver.getCSManager();

        for (var entry : aspect.getPointcutMethodMap().entrySet()) {
            Pointcut pointcut = entry.getKey();
            List<AspectMethod> aspectMethods = entry.getValue();

            // [调试日志] 如果是特定类的方法，打印匹配尝试
            // if (currMethod.getDeclaringClass().getName().contains("CategoryAction")) {
            //     logger.info("Checking method: {} against pointcut: {}", currMethod.getName(), pointcut.getExpression());
            // }

            if (!PointcutMatcher.matches(pointcut, currMethod, aspect)) {
                continue;
            }

            // [调试日志] 匹配成功
            logger.info(">>> AOP Match Found! Method: [{}], Aspect: [{}]", currMethod.toString(), aspect.getjClass().getName());

            for (AspectMethod aspectMethod : aspectMethods) {
                weaveAdvice(csMethod, aspectMethod, aspect, context, csManager);
            }
        }
    }

    private void weaveAdvice(CSMethod csTargetMethod, AspectMethod aspectMethod,
                             AspectClass aspect, Context context, CSManager csManager) {
        JMethod adviceMethod = aspectMethod.getMethod();

        CSCallSite virtualCallSite = createVirtualCallSite(csTargetMethod, aspectMethod, context, csManager);

        Context adviceContext = solver.getContextSelector().selectContext(virtualCallSite, adviceMethod);
        CSMethod csAdviceMethod = csManager.getCSMethod(adviceContext, adviceMethod);

        AOPEdge aopEdge = new AOPEdge(virtualCallSite, csAdviceMethod, aspectMethod.getAdviceType());

        provideEntryObjectsForAdvice(csAdviceMethod, aopEdge);

        solver.addCallEdge(aopEdge);
    }

    private CSCallSite createVirtualCallSite(CSMethod csTargetMethod, AspectMethod aspectMethod,
                                             Context context, CSManager csManager) {
        Invoke virtualInvoke = AOPInvokeFactory.createVirtualInvoke(
            csTargetMethod.getMethod(), aspectMethod);
        return csManager.getCSCallSite(context, virtualInvoke);
    }

    private void provideEntryObjectsForAdvice(CSMethod csAdviceMethod, AOPEdge aopEdge) {
        JMethod adviceMethod = csAdviceMethod.getMethod();
        Context context = csAdviceMethod.getContext();
        IR ir = adviceMethod.getIR();

        if (!adviceMethod.isStatic()) {
            Obj aspectObj = solver.getHeapModel().getMockObj(
                MockObjDescriptor.ASPECT_OBJ,
                adviceMethod.getDeclaringClass().getName(),
                adviceMethod.getDeclaringClass().getType()
            );
            solver.addVarPointsTo(context, ir.getThis(), context, aspectObj);
        }

        if (adviceMethod.getParamCount() > 0) {
            for (int i = 0; i < adviceMethod.getParamCount(); i++) {
                Var param = ir.getParam(i);
                Type paramType = param.getType();

                if (isJoinPointType(paramType)) {
                    String concreteType = RuntimeImplementationMapper.getConcreteType(paramType.getName());
                    JClass aClass = hierarchy.getClass(concreteType);
                    Type typeToUse = (aClass != null) ? aClass.getType() : paramType;

                    String callSiteStr = aopEdge.getCallSite().getCallSite().toString();
                    String joinPointAllocKey = "JoinPoint:" + callSiteStr;

                    Obj joinPointObj = solver.getHeapModel().getMockObj(
                        () -> "JoinPointObj:" + callSiteStr,
                        joinPointAllocKey,
                        typeToUse,
                        adviceMethod
                    );

                    solver.addVarPointsTo(context, param, context, joinPointObj);
                }
            }
        }
    }

    private boolean isJoinPointType(Type type) {
        if (!(type instanceof ClassType classType)) return false;
        String name = classType.getName();
        return name.equals("org.aspectj.lang.JoinPoint") ||
            name.equals("org.aspectj.lang.ProceedingJoinPoint");
    }

    @Override
    public void onFinish() {
        AOPEdgesToFile.writeAOPEdgesToFile(solver.getCallGraph());
    }
}