// File: SinkMetadataExtractor.java
package org.example.taint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

import java.util.*;

public class SinkMetadataExtractor {

    private static final Logger logger = LogManager.getLogger(SinkMetadataExtractor.class);

    private static final String PARAM_ANNOTATION =
        "org.apache.ibatis.annotations.Param";
    private static final String VULNERABILITY_TYPE_SQL_INJECTION = "SQL_INJECTION";

    public List<SinkMetadata> extract(List<MyBatisXMLParser.MapperParseResult> mapperResults) {
        List<SinkMetadata> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // 诊断日志：打印所有namespace的解析结果
        logger.info("Resolving {} mapper namespaces...", mapperResults.size());
        for (MyBatisXMLParser.MapperParseResult mapperResult : mapperResults) {
            JClass resolved = resolveClass(mapperResult.namespace);
            logger.info("  Namespace resolve: {} -> {}",
                mapperResult.namespace,
                resolved != null ? resolved.getName() : "NOT FOUND");
        }

        for (MyBatisXMLParser.MapperParseResult mapperResult : mapperResults) {
            JClass mapperClass = resolveClass(mapperResult.namespace);
            if (mapperClass == null) {
                logger.warn("Could not resolve mapper class for namespace: {}",
                    mapperResult.namespace);
                continue;
            }

            for (MyBatisXMLParser.SqlOperation operation : mapperResult.operations) {
                JMethod mapperMethod = findMethod(mapperClass, operation.id);
                if (mapperMethod == null) {
                    logger.debug("Method not found: {}.{}", mapperClass.getName(), operation.id);
                    continue;
                }

                Set<String> processedVarNames = new HashSet<>();
                for (MyBatisXMLParser.VarReference varRef : operation.variables) {
                    String rootVarName = varRef.varName;
                    if (processedVarNames.contains(rootVarName)) continue;
                    processedVarNames.add(rootVarName);

                    int paramIdx = matchParam(mapperMethod, rootVarName);
                    if (paramIdx >= 0) {
                        String dedupeKey = mapperMethod.getSignature() + "#" + paramIdx;
                        if (!seen.contains(dedupeKey)) {
                            seen.add(dedupeKey);
                            SinkMetadata sinkMeta = new SinkMetadata(
                                mapperMethod.getSignature(),
                                paramIdx,
                                operation.sqlFragment,
                                VULNERABILITY_TYPE_SQL_INJECTION
                            );
                            result.add(sinkMeta);
                            logger.debug("Sink candidate: {}, paramIdx={}, risky={}",
                                mapperMethod.getSignature(), paramIdx, sinkMeta.isRisky());
                        } else {
                            logger.debug("Duplicate sink skipped: {}", dedupeKey);
                        }
                    } else {
                        logger.debug("No param match for var '{}' in '{}.{}'",
                            rootVarName, mapperClass.getName(), operation.id);
                    }
                }
            }
        }

        logger.info("Extracted {} sink metadata entries (deduplicated, before risk filtering)",
            result.size());
        return result;
    }

    /**
     * Resolve class by trying multiple name variants to handle Tai-e's "classes." prefix.
     *
     * Tries in order:
     * 1. Exact name
     * 2. With "classes." prefix
     * 3. Strip existing "classes." prefix
     * 4. Suffix match among all application classes (fallback)
     */
    private JClass resolveClass(String namespace) {
        // Try 1: exact name
        JClass cls = World.get().getClassHierarchy().getClass(namespace);
        if (cls != null) return cls;

        // Try 2: with "classes." prefix
        cls = World.get().getClassHierarchy().getClass("classes." + namespace);
        if (cls != null) return cls;

        // Try 3: strip existing "classes." prefix
        if (namespace.startsWith("classes.")) {
            cls = World.get().getClassHierarchy()
                .getClass(namespace.substring("classes.".length()));
            if (cls != null) return cls;
        }

        // Try 4: suffix match among all application classes
        String target = namespace.replace("classes.", "");
        for (JClass appClass :
            World.get().getClassHierarchy().applicationClasses().toList()) {
            String className = appClass.getName();
            // Strip "classes." prefix from className for comparison
            String normalizedClassName = className.startsWith("classes.")
                ? className.substring("classes.".length())
                : className;
            if (normalizedClassName.equals(target)) {
                logger.debug("Resolved via suffix search: {} -> {}", namespace, className);
                return appClass;
            }
        }

        return null;
    }

    private JMethod findMethod(JClass mapperClass, String methodId) {
        for (JMethod method : mapperClass.getDeclaredMethods()) {
            if (method.getName().equals(methodId)) return method;
        }
        return null;
    }

    /**
     * Match variable name to parameter index.
     * Strategy: @Param annotation → IR param name → single param → MyBatis defaults
     */
    private int matchParam(JMethod method, String varName) {
        List<Type> paramTypes = method.getParamTypes();
        int paramCount = paramTypes.size();

        // Strategy 1: @Param annotation value
        for (int i = 0; i < paramCount; i++) {
            Collection<Annotation> paramAnnotations = method.getParamAnnotations(i);
            if (paramAnnotations != null) {
                for (Annotation annotation : paramAnnotations) {
                    if (PARAM_ANNOTATION.equals(annotation.getType())) {
                        Object value = annotation.getElement("value");
                        if (value != null && varName.equals(value.toString())) {
                            return i;
                        }
                    }
                }
            }
        }

        // Strategy 2: IR parameter name (may fail for interface methods)
        for (int i = 0; i < paramCount; i++) {
            try {
                String paramName = method.getIR().getParam(i).getName();
                if (varName.equals(paramName)) return i;
            } catch (Exception ignored) {
                // Expected for interface methods
            }
        }

        // Strategy 3: Single parameter → must be index 0
        if (paramCount == 1) {
            logger.debug("Single-param '{}': mapping '{}' to index 0",
                method.getName(), varName);
            return 0;
        }

        // Strategy 4: MyBatis default names (param1/param2/... or arg0/arg1/...)
        if (varName.matches("param\\d+")) {
            int idx = Integer.parseInt(varName.substring(5)) - 1;
            if (idx >= 0 && idx < paramCount) return idx;
        }
        if (varName.matches("arg\\d+")) {
            int idx = Integer.parseInt(varName.substring(3));
            if (idx >= 0 && idx < paramCount) return idx;
        }

        logger.debug("No match for '{}' in method '{}' ({} params)",
            varName, method.getName(), paramCount);
        return -1;
    }
}