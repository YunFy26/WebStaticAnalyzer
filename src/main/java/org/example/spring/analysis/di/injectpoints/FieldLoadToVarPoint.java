package org.example.spring.analysis.di.injectpoints;

import org.example.spring.analysis.di.bean.BeanClass;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;

import java.util.List;
import java.util.Map;

/**
 * 记录DI注入点
 * 例如:
 * UserController (BeanClass)
 *   └─ getUser() (Method)
 *       └─ userService (Field) -> [$r1] (Var)
 *   └─ getUser2() (Method)
 *       └─ userServiceImpl (Field) -> [$r1] (Var)
 */
public class FieldLoadToVarPoint {

//    private final JClass jClass;
//
//    private final JField jField;
//
//    private final LoadFieldPoint loadFieldPoint;


}
