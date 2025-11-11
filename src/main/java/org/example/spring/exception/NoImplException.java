package org.example.spring.exception;

import pascal.taie.language.classes.JClass;

/**
 * 当依赖注入找不到合适的实现类时抛出的异常
 */
public class NoImplException extends RuntimeException {

    private final String interfaceOrClassName;
    private final String fieldName;

    public NoImplException(String message) {
        super(message);
        this.interfaceOrClassName = null;
        this.fieldName = null;
    }

    public NoImplException(String message, Throwable cause) {
        super(message, cause);
        this.interfaceOrClassName = null;
        this.fieldName = null;
    }

    /**
     * 为接口或抽象类找不到实现类时构造异常
     */
    public NoImplException(JClass interfaceOrAbstractClass) {
        super(String.format("No implementation found for type: %s",
            interfaceOrAbstractClass.getName()));
        this.interfaceOrClassName = interfaceOrAbstractClass.getName();
        this.fieldName = null;
    }

    /**
     * 为字段注入找不到合适的 Bean 时构造异常
     */
    public NoImplException(JClass interfaceOrAbstractClass, String fieldName) {
        super(String.format("No implementation found for field '%s' of type: %s",
            fieldName, interfaceOrAbstractClass.getName()));
        this.interfaceOrClassName = interfaceOrAbstractClass.getName();
        this.fieldName = fieldName;
    }

    public String getInterfaceOrClassName() {
        return interfaceOrClassName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
