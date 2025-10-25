package org.example.spring.analysis.router;

import pascal.taie.language.classes.JClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity class including the JClass, routerMethods and baseUrls
 */

public class ControllerClass {

    private final JClass jClass;

    private final List<String> baseUrls;

    private final List<RouterMethod> routerMethods;

    public ControllerClass(JClass jClass) {
        this.jClass = jClass;
        this.baseUrls = new ArrayList<>();
        this.routerMethods = new ArrayList<>();
    }

    public JClass getJClass() {
        return jClass;
    }

    public void setBaseUrls(List<String> baseUrls) {
        this.baseUrls.addAll(baseUrls);
    }

    public List<String> getBaseUrls() {
        return baseUrls;
    }

    public void addRouterMethod(RouterMethod routerMethod) {
        this.routerMethods.add(routerMethod);
    }

    public List<RouterMethod> getRouterMethods() {
        return routerMethods;
    }

}
