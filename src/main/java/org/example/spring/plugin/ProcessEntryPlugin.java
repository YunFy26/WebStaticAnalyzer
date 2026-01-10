package org.example.spring.plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.spring.analysis.RouterAnalysis;
import org.example.spring.analysis.di.MockObjDescriptor;
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
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring 应用入口点插件
 * <p>
 * 重构说明：
 * 根据参数类型严格区分处理策略：
 * 1. 无参静态方法 -> EmptyParamProvider
 * 2. 普通参数静态方法 -> DeclaredParamProvider
 * 3. 实例方法(需要this) 或 含特殊映射参数的方法 -> SpecifiedParamProvider
 * </p>
 */
public class ProcessEntryPlugin implements Plugin {

    private static final Logger logger = LogManager.getLogger(ProcessEntryPlugin.class);

    private Solver solver;
    private ClassHierarchy hierarchy;
    private HeapModel heapModel;

    private final List<ControllerClass> controllers = World.get().getResult(RouterAnalysis.ID);

    // 单例缓存：Key = Type, Value = Obj
    private final Map<Type, Obj> singletonObjects = new HashMap<>();

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        this.hierarchy = solver.getHierarchy();
        this.heapModel = solver.getHeapModel();
    }

    @Override
    public void onStart() {
        if (controllers == null || controllers.isEmpty()) {
            logger.warn("No controllers found from RouterAnalysis.");
            return;
        }

        for (ControllerClass controller : controllers) {
            processController(controller);
        }
    }

    private void processController(ControllerClass controller) {
        JClass jClass = controller.getJClass();
        for (RouterMethod routerMethod : controller.getRouterMethods()) {
            JMethod jMethod = routerMethod.getJMethod();
            if (jMethod.getParamCount() == 0) {
                processEmptyParamMethod(jMethod);
            } else {
                int paramCount = jMethod.getParamCount();
                for (int i = 0; i < paramCount; i++) {
                    Type paramType = jMethod.getParamType(i);
                    String paramTypeName = paramType.getName();
                    if (RuntimeImplementationMapper.hasConcreteMapping(paramTypeName)) {
                        SpecifiedParamProvider.Builder paramProviderBuilder =
                            new SpecifiedParamProvider.Builder(jMethod);
                        Obj thisObj = heapModel.getMockObj(
                            MockObjDescriptor.DI_OBJ,
                            "MethodPara{this}",
                            jClass.getType(),
                            jMethod
                        );
                        String concreteType = RuntimeImplementationMapper.getConcreteType(paramTypeName);
                        JClass concreteClass = hierarchy.getClass(concreteType);
                        if (concreteClass != null) {
                            Obj p = heapModel.getMockObj(
                                MockObjDescriptor.DI_OBJ,
                                concreteClass.getName(),
                                concreteClass.getType()
                            );
                            paramProviderBuilder.addThisObj(thisObj)
                                .addParamObj(i, p)
                                .setDelegate(new DeclaredParamProvider(jMethod, heapModel));
                        }else {
                            logger.info("Concrete class not found for type: {}", concreteType);
                        }
                    } else {
                        solver.addEntryPoint(new EntryPoint(jMethod, new DeclaredParamProvider(jMethod, heapModel, 1)));
                    }
                }
            }
        }
    }

    private void processEmptyParamMethod(JMethod jMethod) {
        solver.addEntryPoint(new EntryPoint(jMethod, EmptyParamProvider.get()));
        logger.info("Added entry point (Empty): {}", jMethod);
    }
}