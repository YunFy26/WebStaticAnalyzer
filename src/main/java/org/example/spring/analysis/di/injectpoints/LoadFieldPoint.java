package org.example.spring.analysis.di.injectpoints;

import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 记录Field加载到变量的点
 * 例如:
 * UserController (BeanClass)
 *   └─ getUser() (Method)
 *       └─ userService (Field) -> [$r1] (Var)
 */
public class LoadFieldPoint {

    private final Stmt stmt;

    private final JField jField;

    private List<Var> vars;

    public LoadFieldPoint(Stmt stmt, JField jField) {
        this.stmt = stmt;
        this.jField = jField;
        this.vars = new ArrayList<>();
    }

    public Stmt getStmt() {
        return stmt;
    }

    public JField getjField() {
        return jField;
    }

    public List<Var> getVars() {
        return vars;
    }
}
