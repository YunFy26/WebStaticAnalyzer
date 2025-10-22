package org.example.spring.router;

import org.example.enums.AnnotationElementKeys;
import org.example.spring.rules.EntryMappingRules;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.annotation.Element;
import pascal.taie.language.classes.JClass;

import java.util.ArrayList;
import java.util.List;

public class ControllerParser {

    /**
     * 提取Controller上的base url
     * @param controller 控制类
     * @return base url 列表
     */
    public static List<String> parseBaseUrl(ControllerClass controller){
        JClass jClass = controller.getJClass();
        if (jClass.hasAnnotation(EntryMappingRules.REQUEST_MAPPING)) {
            Annotation annotation = jClass.getAnnotation(EntryMappingRules.REQUEST_MAPPING);
            assert annotation != null;
            return RouterUtils.extractUrls(annotation);
        }
        return List.of();
    }

}
