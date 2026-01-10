package org.example.spring.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.spring.analysis.di.bean.BeanClass;
import org.example.spring.analysis.di.injectpoints.*;
import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;

import java.util.*;

public class InjectPointsAnalysis extends ProgramAnalysis {

    public static final String ID = "injectPointsAnalysis";

    private static final Logger logger = LogManager.getLogger(InjectPointsAnalysis.class);

    private final Collection<BeanClass> beans = World.get().getResult(BeanAnalysis.ID);

    private final Map<BeanClass, Map<JMethod, List<LoadFieldPoint>>> injectPoints = new HashMap<>();

    public InjectPointsAnalysis(AnalysisConfig config) {
        super(config);
    }

    @Override
    public Object analyze() {
        analyzeInjectPoints();
        return injectPoints;
    }

    /**
     * 分析注入点（LoadField点）
     */
    private void analyzeInjectPoints(){
        beans.forEach(bean -> {
            JClass jClass = bean.getjClass();
            Map<JMethod, List<LoadFieldPoint>> loadFields = new HashMap<>();
            jClass.getDeclaredMethods().forEach(jMethod -> {
                if (jMethod.isAbstract() || jMethod.isNative()) {
                    return;
                }
                List<Stmt> stmts = jMethod.getIR().getStmts();
                // 存储当前method中的LoadField点
                // 有可能一个Field被加载多次，用一个Map记录
                Map<JField, LoadFieldPoint> fieldPoints = new HashMap<>();
                stmts.forEach(stmt -> {
                    if (stmt instanceof LoadField loadField) {
                        JField jField = loadField.getFieldRef().resolve();
                        // 只分析当前Bean类声明的字段（过滤静态字段和外部类字段）
                        if (!jClass.equals(jField.getDeclaringClass())) {
                            return;
                        }
                        // 只分析实例字段（过滤静态字段如System.out）
                        if (jField.isStatic()) {
                            return;
                        }
                        // 只分析有调用语句的变量
                        Var var = loadField.getLValue();
                        if (var.getInvokes().isEmpty()) {
                            return;
                        }
                        if (!fieldPoints.containsKey(jField)) {
                            // 如果是第一次加载该字段，创建LoadFieldPoint
                            LoadFieldPoint newPoint = InjectPointInitializer.createLoadFieldPoint(loadField);
                            fieldPoints.put(jField, newPoint);
                            if (!loadFields.containsKey(jMethod)){
                                loadFields.put(jMethod, new ArrayList<>());
                            }
                            loadFields.get(jMethod).add(newPoint);
                        }else {
                            // 如果已经加载过该字段，直接添加变量到已有的LoadFieldPoint
                            LoadFieldPoint loadFieldPoint = fieldPoints.get(jField);
                            loadFieldPoint.getVars().add(loadField.getLValue());
                        }
                    }
                });
            });
            injectPoints.computeIfAbsent(bean, k -> loadFields);
        });
    }
}