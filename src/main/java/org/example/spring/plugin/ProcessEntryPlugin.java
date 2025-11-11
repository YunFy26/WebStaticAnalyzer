package org.example.spring.plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.spring.analysis.RouterAnalysis;
import org.example.spring.analysis.router.ControllerClass;
import org.example.spring.analysis.router.RouterMethod;
import pascal.taie.World;
import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.DeclaredParamProvider;
import pascal.taie.analysis.pta.core.solver.EmptyParamProvider;
import pascal.taie.analysis.pta.core.solver.EntryPoint;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.core.solver.SpecifiedParamProvider;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;

import java.util.List;

/**
 * Spring 应用入口点插件
 * 为 Controller 方法创建入口点
 */
public class ProcessEntryPlugin implements Plugin {

    private static final Logger logger = LogManager.getLogger(ProcessEntryPlugin.class);

    private Solver solver;

    private ClassHierarchy hierarchy;

    private HeapModel heapModel;

    private final List<ControllerClass> controllers = World.get().getResult(RouterAnalysis.ID);

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        this.hierarchy = World.get().getClassHierarchy();
        this.heapModel = solver.getHeapModel();
    }

    @Override
    public void onStart() {
        addControllerEntryPoints();
    }

    /**
     * 为 Controller 路由方法添加入口点
     */
    private void addControllerEntryPoints() {
        for (ControllerClass controller : controllers) {
            for (RouterMethod routerMethod : controller.getRouterMethods()) {
                JMethod jMethod = routerMethod.getJMethod();
                if (jMethod.getParamCount() == 0) {
                    solver.addEntryPoint(new EntryPoint(jMethod, EmptyParamProvider.get()));
                    logger.info("Add entry point (empty params): {}", jMethod);
                } else {
                    addRouterMethodWithParams(routerMethod);
                }
            }
        }
    }

    /**
     * 为有参数的路由方法创建入口点
     */
    private void addRouterMethodWithParams(RouterMethod routerMethod) {
        JMethod jMethod = routerMethod.getJMethod();
        SpecifiedParamProvider.Builder builder = new SpecifiedParamProvider.Builder(jMethod);

        // 创建 this 对象
        JClass controllerClass = jMethod.getDeclaringClass();
        Obj thisObj = heapModel.getMockObj(
            Descriptor.ENTRY_DESC,
            "EntryPoint{this}",
            controllerClass.getType(),
            jMethod
        );
        builder.addThisObj(thisObj);

        // 为每个参数创建模拟对象
        for (int i = 0; i < jMethod.getParamCount(); i++) {
            Type paramType = jMethod.getParamType(i);

            // 使用 TypeMapper 获取具体类型
            String concreteType = EntryParaInterfaceMapper.getConcreteType(paramType.getName());
            JClass aClass = hierarchy.getClass(concreteType);
            Obj paramObj;
            if (aClass != null) {
                paramObj = heapModel.getMockObj(
                    Descriptor.ENTRY_DESC,
                    "EntryPoint{param" + i + "}",
                    aClass.getType(),
                    jMethod
                );
            }else {
                paramObj = heapModel.getMockObj(
                    Descriptor.ENTRY_DESC,
                    "EntryPoint{param" + i + "}",
                    paramType,
                    jMethod
                );
            }
            builder.addParamObj(i, paramObj);
        }

        // 使用 DeclaredParamProvider 自动处理那些没有被显式指定的参数
        builder.setDelegate(new DeclaredParamProvider(jMethod, heapModel, 1));

        solver.addEntryPoint(new EntryPoint(jMethod, builder.build()));
        logger.info("Added entry point (with params): {}", jMethod);
    }
}
