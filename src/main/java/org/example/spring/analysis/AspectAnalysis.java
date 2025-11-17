package org.example.spring.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.enums.AnnotationElementKeys;
import org.example.spring.analysis.aop.*;
import org.example.spring.rules.AspectRules;
import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.annotation.Element;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.util.*;

/**
 * AOP 切面分析
 * 识别项目中的所有切面并建立切点匹配关系
 * 为指针分析提供切面调用信息
 */
public class AspectAnalysis extends ProgramAnalysis {

    public static final String ID = "aspectAnalysis";

    private static final Logger logger = LogManager.getLogger(AspectAnalysis.class);

    private final ClassHierarchy hierarchy;

    private final List<AspectClass> aspectClasses = new ArrayList<>();

    public AspectAnalysis(AnalysisConfig config) {
        super(config);
        this.hierarchy = World.get().getClassHierarchy();
    }

    @Override
    public Object analyze() {
        logger.info("Starting AOP aspect analysis...");
        scanAspectClasses();
        return aspectClasses;
    }

    /**
     * 扫描所有标注了 @Aspect 的类
     */
    private void scanAspectClasses() {
        List<JClass> appClasses = hierarchy.applicationClasses().toList();
        appClasses.forEach(app -> {
            if (hasAspectAnnotation(app)) {
                AspectClass aspectClass = AspectClassInitializer.createAspectClass(app);
                aspectClasses.add(aspectClass);
            }
        });
    }

    /**
     * 判断是否为 Aspect 类
     */
    public boolean hasAspectAnnotation(JClass jClass) {
        return jClass.getAnnotation(AspectRules.ASPECT) != null;
    }

}
