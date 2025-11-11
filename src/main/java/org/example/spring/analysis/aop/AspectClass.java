package org.example.spring.analysis.aop;

import pascal.taie.language.classes.JClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表示一个 Aspect 类
 */
public class AspectClass {

    private final JClass jClass;

    private final Map<Pointcut, List<AspectMethod>> pointcutMethodMap;

    public AspectClass(JClass jClass) {
        this.jClass = jClass;
        this.pointcutMethodMap = new HashMap<>();
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AspectClass{\n");
        sb.append("  class=").append(jClass.getName()).append("\n");
        sb.append("  pointcut-method mappings=[\n");

        if (!pointcutMethodMap.isEmpty()) {
            pointcutMethodMap.forEach((pointcut, methods) -> {
                sb.append("    Pointcut: ").append(pointcut.toString()).append("\n");
                sb.append("      -> AspectMethods: [\n");
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

