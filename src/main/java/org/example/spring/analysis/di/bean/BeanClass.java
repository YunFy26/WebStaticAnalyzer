package org.example.spring.analysis.di.bean;

import pascal.taie.language.classes.JClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Bean定义
 */
public class BeanClass {

    /**
     * 匹配Bean注解规则的类
     */
    private final JClass jClass;

    /**
     * 该类实现的接口
     */
    private Collection<JClass> interfaces;

    /**
     * 该类的父类
     */
    private JClass superClass;

    /**
     * 该jClass是否是接口，如果是接口，说明是Mapper等接口类型
     */
    private final boolean isInterface;

    /**
     * 如果jClass是接口，说明实现类是动态生成的
     */
    private Collection<JClass> implementations;

    /**
     * 全限定名
     */
    private final String fullyQualifiedClassName;

    /**
     * 简化类名
     */
    private final String simpleClassName;

    /**
     * 默认名称 ： userService
     * TODO：优化java doc
     */
    private String defaultBeanName;

    /**
     * 通过注解（如 @Bean, @Component）显式声明的名称
     */
    private String declaredBeanName;

    /**
     * bean的作用域
     */
    private BeanScope scope;

    private boolean isPrimary;

    private boolean isLazy;

    public BeanClass(JClass jClass) {
        this.jClass = jClass;
        this.isInterface = jClass.isInterface();
        this.fullyQualifiedClassName = jClass.getName();
        this.simpleClassName = jClass.getSimpleName();
        this.implementations = new ArrayList<>();
    }

    public JClass getjClass() {
        return jClass;
    }

    public Collection<JClass> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(Collection<JClass> interfaces) {
        this.interfaces = interfaces;
    }

    public JClass getSuperClass() {
        return superClass;
    }

    public void setSuperClass(JClass superClass) {
        this.superClass = superClass;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public Collection<JClass> getImplementations() {
        return implementations;
    }

    public void setImplementations(Collection<JClass> implementations) {
        this.implementations = implementations;
    }

    public String getFullyQualifiedClassName() {
        return fullyQualifiedClassName;
    }

    public String getSimpleClassName() {
        return simpleClassName;
    }

    public String getDefaultBeanName() {
        return defaultBeanName;
    }

    public void setDefaultBeanName(String defaultBeanName) {
        this.defaultBeanName = defaultBeanName;
    }

    public String getDeclaredBeanName() {
        return declaredBeanName;
    }

    public void setDeclaredBeanName(String declaredBeanName) {
        this.declaredBeanName = declaredBeanName;
    }

    public BeanScope getScope() {
        return scope;
    }

    public void setScope(BeanScope scope) {
        this.scope = scope;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    public boolean isLazy() {
        return isLazy;
    }

    public void setLazy(boolean lazy) {
        isLazy = lazy;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BeanClass beanClass = (BeanClass) o;
        return Objects.equals(fullyQualifiedClassName, beanClass.fullyQualifiedClassName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fullyQualifiedClassName);
    }

}
