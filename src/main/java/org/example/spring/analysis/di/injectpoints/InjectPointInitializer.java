package org.example.spring.analysis.di.injectpoints;

import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.language.classes.JMethod;

/**
 * 注入点初始化器
 */
public class InjectPointInitializer {

    public static LoadFieldPoint createLoadFieldPoint(LoadField loadField){
        LoadFieldPoint loadFieldPoint = new LoadFieldPoint(
                loadField,
                loadField.getFieldRef().resolve()
        );
        Var var = loadField.getLValue();
        loadFieldPoint.getVars().add(var);
        return loadFieldPoint;
    }

}
