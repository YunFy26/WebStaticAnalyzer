package org.example.spring.analysis.aop;

import pascal.taie.language.classes.JClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表示一个 Aspect 类
 * 存储切面定义、通知方法映射以及所有命名切点定义
 */
public class AspectClass {

    private final JClass jClass;

    // 存储 "切点 -> 通知方法列表" 的映射 (用于织入阶段遍历)
    // 这里的 Key (Pointcut) 通常是跟 Advice 绑定的具体切点
    private final Map<Pointcut, List<AspectMethod>> pointcutMethodMap;

    // [新增] 存储 "切点名称 -> 切点对象" 的映射
    // 用于在解析组合切点 (e.g., "pointcutA() && pointcutB()") 时查找引用的定义
    private final Map<String, Pointcut> globalPointcuts;

    public AspectClass(JClass jClass) {
        this.jClass = jClass;
        this.pointcutMethodMap = new HashMap<>();
        this.globalPointcuts = new HashMap<>();
    }

    public JClass getjClass() {
        return jClass;
    }

    public Map<Pointcut, List<AspectMethod>> getPointcutMethodMap() {
        return pointcutMethodMap;
    }

    public void addAspectMethod(Pointcut pointcut, AspectMethod method) {
        pointcutMethodMap.computeIfAbsent(pointcut, k -> new ArrayList<>()).add(method);
    }

    /**
     * 注册命名切点定义
     * @param name 切点方法名 (e.g., "serviceLayer")
     * @param pointcut 切点对象
     */
    public void addNamedPointcut(String name, Pointcut pointcut) {
        globalPointcuts.put(name, pointcut);
    }

    /**
     * 查找命名切点定义
     */
    public Pointcut getNamedPointcut(String name) {
        return globalPointcuts.get(name);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AspectClass{\n");
        sb.append("  class=").append(jClass.getName()).append("\n");
        sb.append("  defined pointcuts=").append(globalPointcuts.size()).append("\n");
        sb.append("  pointcut-method mappings=[\n");

        if (!pointcutMethodMap.isEmpty()) {
            pointcutMethodMap.forEach((pointcut, methods) -> {
                sb.append("    Pointcut: ").append(pointcut.toString()).append("\n");
                sb.append("      -> Advice: [\n");
                for (AspectMethod method : methods) {
                    sb.append("           ").append(method.toString().replace("\n", "\n           ")).append("\n");
                }
                sb.append("      ]\n");
            });
        } else {
            sb.append("    (none)\n");
        }
        sb.append("  ]\n}");
        return sb.toString();
    }
}