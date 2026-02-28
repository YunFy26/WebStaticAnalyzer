//// File: SourceMetadataExtractor.java
//package org.example.taint;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.example.spring.analysis.router.ControllerClass;
//import org.example.spring.analysis.router.RouterMethod;
//import pascal.taie.World;
//import pascal.taie.language.annotation.Annotation;
//import pascal.taie.language.classes.JClass;
//import pascal.taie.language.classes.JField;
//import pascal.taie.language.classes.JMethod;
//import pascal.taie.language.type.Type;
//
//import java.util.*;
//
///**
// * Implements Algorithm 4-1: Access-path-based Source metadata extraction.
// */
//public class SourceMetadataExtractor {
//
//    private static final Logger logger = LogManager.getLogger(SourceMetadataExtractor.class);
//
//    public List<SourceMetadata> extract(List<ControllerClass> controllerClasses) {
//        List<SourceMetadata> result = new ArrayList<>();
//
//        for (ControllerClass controllerClass : controllerClasses) {
//            for (RouterMethod routerMethod : controllerClass.getRouterMethods()) {
//                JMethod jMethod = routerMethod.getJMethod();
//                String sig = jMethod.getSignature();
//
//                List<Type> paramTypes = jMethod.getParamTypes();
//                for (int idx = 0; idx < paramTypes.size(); idx++) {
//                    Type paramType = paramTypes.get(idx);
//                    Set<String> paramAnnotations = getParamAnnotations(jMethod, idx);
//                    String typeName = paramType.getName();
//
//                    if (SpringAnnotationRules.isBasicType(typeName)
//                        || SpringAnnotationRules.isFrameworkType(typeName)) {
//                        SourceMetadata meta = new SourceMetadata(
//                            sig, idx,
//                            Collections.emptyList(),
//                            typeName,
//                            paramAnnotations,
//                            getParamName(jMethod, idx)
//                        );
//                        result.add(meta);
//                    } else {
//                        List<FieldInfo> nestedFields =
//                            resolveBindableFields(typeName, new HashSet<>());
//                        if (nestedFields.isEmpty()) {
//                            // Fallback: treat as object-level source
//                            result.add(new SourceMetadata(
//                                sig, idx,
//                                Collections.emptyList(),
//                                typeName,
//                                paramAnnotations,
//                                getParamName(jMethod, idx)
//                            ));
//                        } else {
//                            for (FieldInfo fieldInfo : nestedFields) {
//                                Set<String> combined = new HashSet<>(paramAnnotations);
//                                combined.addAll(fieldInfo.annotations);
//                                result.add(new SourceMetadata(
//                                    sig, idx,
//                                    fieldInfo.path,
//                                    fieldInfo.type,
//                                    combined,
//                                    fieldInfo.fieldName
//                                ));
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        logger.info("Extracted {} source metadata entries (before filtering)", result.size());
//        return result;
//    }
//
//    private List<FieldInfo> resolveBindableFields(String typeName, Set<String> visited) {
//        List<FieldInfo> result = new ArrayList<>();
//        if (visited.contains(typeName)) return result;
//        visited.add(typeName);
//
//        JClass jClass = resolveClass(typeName);
//        if (jClass == null) return result;
//
//        for (JField field : jClass.getDeclaredFields()) {
//            if (field.isStatic()) continue;
//
//            String fieldTypeName = field.getType().getName();
//            String fieldName = field.getName();
//            Set<String> fieldAnnotations = getFieldAnnotations(field);
//
//            if (SpringAnnotationRules.isBasicType(fieldTypeName)
//                || SpringAnnotationRules.isFrameworkType(fieldTypeName)) {
//                result.add(new FieldInfo(
//                    List.of(fieldName), fieldTypeName, fieldAnnotations, fieldName));
//            } else {
//                List<FieldInfo> nested =
//                    resolveBindableFields(fieldTypeName, new HashSet<>(visited));
//                for (FieldInfo nestedInfo : nested) {
//                    List<String> newPath = new ArrayList<>();
//                    newPath.add(fieldName);
//                    newPath.addAll(nestedInfo.path);
//                    Set<String> combined = new HashSet<>(fieldAnnotations);
//                    combined.addAll(nestedInfo.annotations);
//                    result.add(new FieldInfo(
//                        newPath, nestedInfo.type, combined, nestedInfo.fieldName));
//                }
//            }
//        }
//        return result;
//    }
//
//    /**
//     * Resolve class by trying multiple name variants to handle
//     * Tai-e's "classes." prefix that appears when classpath points to a directory.
//     */
//    private JClass resolveClass(String typeName) {
//        JClass cls = World.get().getClassHierarchy().getClass(typeName);
//        if (cls != null) return cls;
//
//        cls = World.get().getClassHierarchy().getClass("classes." + typeName);
//        if (cls != null) return cls;
//
//        if (typeName.startsWith("classes.")) {
//            cls = World.get().getClassHierarchy()
//                .getClass(typeName.substring("classes.".length()));
//            if (cls != null) return cls;
//        }
//        return null;
//    }
//
//    private Set<String> getParamAnnotations(JMethod jMethod, int paramIdx) {
//        Set<String> annotations = new HashSet<>();
//        Collection<Annotation> list = jMethod.getParamAnnotations(paramIdx);
//        if (list != null) {
//            for (Annotation a : list) {
//                annotations.add(a.getType());
//            }
//        }
//        return annotations;
//    }
//
//    private Set<String> getFieldAnnotations(JField field) {
//        Set<String> annotations = new HashSet<>();
//        for (Annotation a : field.getAnnotations()) {
//            annotations.add(a.getType());
//        }
//        return annotations;
//    }
//
//    private String getParamName(JMethod jMethod, int idx) {
//        try {
//            return jMethod.getIR().getParam(idx).getName();
//        } catch (Exception e) {
//            return "param" + idx;
//        }
//    }
//
//    private static class FieldInfo {
//        final List<String> path;
//        final String type;
//        final Set<String> annotations;
//        final String fieldName;
//
//        FieldInfo(List<String> path, String type,
//                  Set<String> annotations, String fieldName) {
//            this.path = path;
//            this.type = type;
//            this.annotations = annotations;
//            this.fieldName = fieldName;
//        }
//    }
//}

// File: SourceMetadataExtractor.java
package org.example.taint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.spring.analysis.router.ControllerClass;
import org.example.spring.analysis.router.RouterMethod;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.type.Type;

import java.util.*;

/**
 * Implements Algorithm 4-1: Access-path-based Source metadata extraction.
 *
 * Change: Domain object parameters (e.g. SysUser, SysRole) are now emitted
 * as parameter-level sources (kind: param, index: N) just like primitive /
 * String parameters.  Tai-e 0.5.1 does not support field-level sources, so
 * expanding them into field paths produced no usable source entries.
 */
public class SourceMetadataExtractor {

    private static final Logger logger = LogManager.getLogger(SourceMetadataExtractor.class);

    // Parameter annotations that mark a parameter as NOT a user-controlled
    // HTTP input (e.g. injected by Spring framework internals).
    private static final Set<String> EXCLUDED_PARAM_ANNOTATIONS = Set.of(
        "org.springframework.web.bind.annotation.PathVariable",  // keep – path params are tainted
        "org.springframework.web.bind.annotation.RequestHeader"  // usually not interesting
    );

    public List<SourceMetadata> extract(List<ControllerClass> controllerClasses) {
        List<SourceMetadata> result = new ArrayList<>();

        for (ControllerClass controllerClass : controllerClasses) {
            for (RouterMethod routerMethod : controllerClass.getRouterMethods()) {
                JMethod jMethod = routerMethod.getJMethod();
                String sig = jMethod.getSignature();

                List<Type> paramTypes = jMethod.getParamTypes();
                for (int idx = 0; idx < paramTypes.size(); idx++) {
                    Type paramType = paramTypes.get(idx);
                    String typeName = paramType.getName();
                    Set<String> paramAnnotations = getParamAnnotations(jMethod, idx);

                    // Skip framework / infrastructure parameter types that are
                    // never user-controlled HTTP inputs.
                    if (isInfrastructureType(typeName)) {
                        continue;
                    }

                    // Emit a single parameter-level source regardless of whether
                    // the type is a primitive, String, or a domain object.
                    // Tai-e's taint engine will propagate taint through field
                    // accesses automatically once the object itself is tainted.
                    result.add(new SourceMetadata(
                        sig, idx,
                        Collections.emptyList(),
                        typeName,
                        paramAnnotations,
                        getParamName(jMethod, idx)
                    ));
                }
            }
        }

        logger.info("Extracted {} source metadata entries (before filtering)", result.size());
        return result;
    }

    /**
     * Returns true for Spring / Servlet infrastructure types that are injected
     * by the framework and do not carry user-controlled data directly as a
     * parameter (HttpServletRequest itself is a source via transfers, not as a
     * param source).
     */
    private boolean isInfrastructureType(String typeName) {
        return typeName.equals("javax.servlet.http.HttpServletRequest")
            || typeName.equals("javax.servlet.http.HttpServletResponse")
            || typeName.equals("javax.servlet.ServletRequest")
            || typeName.equals("javax.servlet.ServletResponse")
            || typeName.equals("org.springframework.ui.ModelMap")
            || typeName.equals("org.springframework.ui.Model")
            || typeName.equals("org.springframework.web.servlet.ModelAndView")
            || typeName.equals("org.springframework.validation.BindingResult")
            || typeName.equals("org.springframework.validation.Errors")
            || typeName.startsWith("org.springframework.web.context.")
            || typeName.equals("java.security.Principal");
    }

    private Set<String> getParamAnnotations(JMethod jMethod, int paramIdx) {
        Set<String> annotations = new HashSet<>();
        Collection<Annotation> list = jMethod.getParamAnnotations(paramIdx);
        if (list != null) {
            for (Annotation a : list) {
                annotations.add(a.getType());
            }
        }
        return annotations;
    }

    private String getParamName(JMethod jMethod, int idx) {
        try {
            return jMethod.getIR().getParam(idx).getName();
        } catch (Exception e) {
            return "param" + idx;
        }
    }
}