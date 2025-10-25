package org.example.spring.analysis.di.bean;

public enum BeanScope {

    SINGLETON("singleton"),

    PROTOTYPE("prototype"),

    REQUEST("request"),

    SESSION("session"),

    APPLICATION("application"),

    WEBSOCKET("websocket");

    private final String value;

    BeanScope(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static BeanScope fromValue(String value) {
        for (BeanScope scope : BeanScope.values()) {
            if (scope.value.equals(value)) {
                return scope;
            }
        }
        return null;
    }

    public boolean isWebScope() {
        return this == REQUEST || this == SESSION || this == APPLICATION || this == WEBSOCKET;
    }

    public boolean isDefault() {
        return this == SINGLETON;
    }

    @Override
    public String toString() {
        return value;
    }

}