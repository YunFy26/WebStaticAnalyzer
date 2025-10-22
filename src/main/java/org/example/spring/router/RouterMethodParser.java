package org.example.spring.router;

import org.example.enums.AnnotationElementKeys;
import org.example.spring.rules.EntryMappingRules;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.annotation.Element;
import pascal.taie.language.classes.JMethod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 路由方法解析类
 */
public class RouterMethodParser {

    record Mapping(String annotation, EntryMappingRules.HttpMethod method) {}

    private static final List<Mapping> mappings = List.of(
        new Mapping(EntryMappingRules.REQUEST_MAPPING, null),
        new Mapping(EntryMappingRules.GET_MAPPING, EntryMappingRules.HttpMethod.GET),
        new Mapping(EntryMappingRules.POST_MAPPING, EntryMappingRules.HttpMethod.POST),
        new Mapping(EntryMappingRules.PUT_MAPPING, EntryMappingRules.HttpMethod.PUT),
        new Mapping(EntryMappingRules.DELETE_MAPPING, EntryMappingRules.HttpMethod.DELETE),
        new Mapping(EntryMappingRules.PATCH_MAPPING, EntryMappingRules.HttpMethod.PATCH)
    );

    /**
     * 解析路由方法的HTTP方法类型
     * @param jMethod 待解析的方法
     * @return HTTP方法类型
     */
    public static EntryMappingRules.HttpMethod parseHttpMethodType(JMethod jMethod) {
        for (Mapping mapping : mappings) {
            if (jMethod.hasAnnotation(mapping.annotation())) {
                if (mapping.annotation().equals(EntryMappingRules.REQUEST_MAPPING)) {
                    Annotation annotation = jMethod.getAnnotation(EntryMappingRules.REQUEST_MAPPING);
                    assert annotation != null;
                    return RouterUtils.parseHttpMethodType(annotation);
                }
                return mapping.method();
            }
        }
        return null;
    }

    /**
     * 解析路由方法的URL列表
     * @param jMethod 待解析的路由方法
     * @return URL列表
     */
    public static List<String> parseUrls(JMethod jMethod) {
        Collection<Annotation> annotations = jMethod.getAnnotations();

        for (Annotation annotation : annotations) {
            if (mappings.stream().anyMatch(m -> annotation.getType().equals(m.annotation()))) {
                return RouterUtils.extractUrls(annotation);
            }
        }
        return List.of();
    }

}
