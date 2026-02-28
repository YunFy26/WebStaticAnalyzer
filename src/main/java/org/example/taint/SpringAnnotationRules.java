// File: SpringAnnotationRules.java
package org.example.taint;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Annotation semantic mapping rules for Spring MVC parameter binding annotations.
 * Corresponds to Table 4-2 in the paper.
 */
public class SpringAnnotationRules {

    public static final String REQUEST_PARAM =
        "org.springframework.web.bind.annotation.RequestParam";
    public static final String PATH_VARIABLE =
        "org.springframework.web.bind.annotation.PathVariable";
    public static final String REQUEST_BODY =
        "org.springframework.web.bind.annotation.RequestBody";
    public static final String REQUEST_PART =
        "org.springframework.web.bind.annotation.RequestPart";
    public static final String REQUEST_HEADER =
        "org.springframework.web.bind.annotation.RequestHeader";
    public static final String COOKIE_VALUE =
        "org.springframework.web.bind.annotation.CookieValue";
    public static final String SESSION_ATTRIBUTE =
        "org.springframework.web.bind.annotation.SessionAttribute";
    public static final String MODEL_ATTRIBUTE =
        "org.springframework.web.bind.annotation.ModelAttribute";

    public static final String HTTP_SERVLET_REQUEST =
        "javax.servlet.http.HttpServletRequest";
    public static final String HTTP_SESSION =
        "javax.servlet.http.HttpSession";

    private static final Map<String, AnnotationRiskLevel> ANNOTATION_RISK_MAP = new HashMap<>();

    static {
        ANNOTATION_RISK_MAP.put(REQUEST_PARAM,   AnnotationRiskLevel.HIGH);
        ANNOTATION_RISK_MAP.put(PATH_VARIABLE,   AnnotationRiskLevel.HIGH);
        ANNOTATION_RISK_MAP.put(REQUEST_BODY,    AnnotationRiskLevel.HIGH);
        ANNOTATION_RISK_MAP.put(REQUEST_PART,    AnnotationRiskLevel.HIGH);
        ANNOTATION_RISK_MAP.put(REQUEST_HEADER,  AnnotationRiskLevel.MEDIUM);
        ANNOTATION_RISK_MAP.put(COOKIE_VALUE,    AnnotationRiskLevel.MEDIUM);
        ANNOTATION_RISK_MAP.put(SESSION_ATTRIBUTE, AnnotationRiskLevel.LOW);
        ANNOTATION_RISK_MAP.put(MODEL_ATTRIBUTE,   AnnotationRiskLevel.LOW);
    }

    public static AnnotationRiskLevel getRiskLevel(String annotationName) {
        return ANNOTATION_RISK_MAP.get(annotationName);
    }

    public static boolean hasHighOrMediumRisk(Set<String> annotations) {
        for (String annotation : annotations) {
            AnnotationRiskLevel level = ANNOTATION_RISK_MAP.get(annotation);
            if (level == AnnotationRiskLevel.HIGH || level == AnnotationRiskLevel.MEDIUM) {
                return true;
            }
        }
        return false;
    }

    public static boolean isRequestBody(Set<String> annotations) {
        return annotations.contains(REQUEST_BODY);
    }

    private static final Set<String> BASIC_TYPES = Set.of(
        "java.lang.String",
        "java.lang.Integer", "int",
        "java.lang.Long",    "long",
        "java.lang.Double",  "double",
        "java.lang.Float",   "float",
        "java.lang.Boolean", "boolean",
        "java.lang.Byte",    "byte",
        "java.lang.Short",   "short",
        "java.lang.Character","char"
    );

    private static final Set<String> FRAMEWORK_TYPES = Set.of(
        HTTP_SERVLET_REQUEST,
        HTTP_SESSION,
        "javax.servlet.http.HttpServletResponse",
        "javax.servlet.ServletRequest",
        "javax.servlet.ServletResponse",
        "org.springframework.ui.Model",
        "org.springframework.ui.ModelMap",
        "org.springframework.web.servlet.ModelAndView",
        "org.springframework.web.context.request.WebRequest"
    );

    public static boolean isBasicType(String typeName) {
        return BASIC_TYPES.contains(typeName);
    }

    public static boolean isFrameworkType(String typeName) {
        return FRAMEWORK_TYPES.contains(typeName);
    }
}