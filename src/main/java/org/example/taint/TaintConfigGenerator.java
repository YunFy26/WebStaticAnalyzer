// File: TaintConfigGenerator.java
package org.example.taint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class TaintConfigGenerator {

    private static final Logger logger = LogManager.getLogger(TaintConfigGenerator.class);

    public TaintAnalysisConfig generateConfig(
        List<SourceMetadata> sourceMetas,
        List<SinkMetadata> sinkMetas,
        Set<String> writeFields) {

        TaintAnalysisConfig config = new TaintAnalysisConfig();

        // --- Source generation with deduplication ---
        Set<String> seenSources = new HashSet<>();
        int sourceCount = 0, sourceFiltered = 0, sourceDuplicated = 0;

        for (SourceMetadata meta : sourceMetas) {
            if (shouldGenerateSourceConfig(meta, writeFields)) {
                String dedupeKey = meta.getMethodSignature() + "#"
                    + meta.getParamIndex() + "#" + meta.getFieldType();
                if (!seenSources.contains(dedupeKey)) {
                    seenSources.add(dedupeKey);
                    config.addSource(new TaintAnalysisConfig.SourceConfig(
                        meta.getMethodSignature(),
                        meta.getParamIndex(),
                        meta.getFieldType(),
                        "param"
                    ));
                    sourceCount++;
                } else {
                    sourceDuplicated++;
                }
            } else {
                sourceFiltered++;
                logger.debug("Filtered source: method={}, idx={}, field={}, annotations={}",
                    meta.getMethodSignature(), meta.getParamIndex(),
                    meta.getFieldName(), meta.getAnnotations());
            }
        }
        logger.info("Sources: {} generated, {} filtered, {} deduplicated",
            sourceCount, sourceFiltered, sourceDuplicated);

        // --- Sink generation with deduplication ---
        Set<String> seenSinks = new HashSet<>();
        int sinkCount = 0, sinkSafe = 0, sinkDuplicated = 0;

        for (SinkMetadata meta : sinkMetas) {
            if (meta.isRisky()) {
                String dedupeKey = meta.getMethodSignature() + "#" + meta.getParamIndex();
                if (!seenSinks.contains(dedupeKey)) {
                    seenSinks.add(dedupeKey);
                    config.addSink(new TaintAnalysisConfig.SinkConfig(
                        meta.getMethodSignature(),
                        meta.getParamIndex()
                    ));
                    sinkCount++;
                } else {
                    sinkDuplicated++;
                }
            } else {
                sinkSafe++;
                logger.debug("Filtered safe sink (uses #{{}}): method={}, idx={}",
                    meta.getMethodSignature(), meta.getParamIndex());
            }
        }
        // 修复：避免Log4j2将 ${} 内容误解析为占位符，拆分日志消息
        logger.info("Sinks: {} generated (risky), {} filtered (safe), {} deduplicated",
            sinkCount, sinkSafe, sinkDuplicated);
        logger.info("  Note: 'risky' means uses string concatenation ($), "
            + "'safe' means uses prepared statement (#)");

        return config;
    }

    /**
     * Dual filtering strategy (Section 4.3.2):
     * - Simple params: filter by annotation risk level (HIGH or MEDIUM)
     * - POJO-expanded fields: filter by @RequestBody + F_write membership
     */
//    private boolean shouldGenerateSourceConfig(SourceMetadata meta, Set<String> writeFields) {
//        return true;
//    }
    private boolean shouldGenerateSourceConfig(SourceMetadata meta, Set<String> writeFields) {
        if (meta.isSimpleParam()) {
            // 简单类型：按注解风险等级过滤
            return SpringAnnotationRules.hasHighOrMediumRisk(meta.getAnnotations());
        } else {
            // 对象类型：
            // 1. 有 @RequestBody → 原逻辑（结合 writeFields）
            if (SpringAnnotationRules.isRequestBody(meta.getAnnotations())) {
                return writeFields.contains(meta.getFieldName());
            }
            // 2. 无注解 / @ModelAttribute → Spring MVC 表单绑定，直接放行
            //    这覆盖了 list(SysUser user) 这类 Query 对象参数
            return true;
        }
    }
}