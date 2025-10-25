package org.example.spring.analysis.router;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.enums.AnnotationElementKeys;
import org.example.spring.rules.EntryMappingRules;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.annotation.Element;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 路由工具类
 */
public class RouterUtils {

    private static final Logger logger = LogManager.getLogger(RouterUtils.class);

    /**
     * 提取Mapping注解中的URL
     * @param mappingAnnotation Mapping注解
     * @return URL列表（不可变集合）
     */
    public static List<String> extractUrls(Annotation mappingAnnotation){
        Element element = mappingAnnotation.getElement(AnnotationElementKeys.VALUE);
        if (element == null) {
            return List.of("");
        }
        // @RequestMapping(value={"/api/{id}","/api/v2"})
        // {"/api/{id}","/api/v2"}
        // 只移除最外层的大括号,保留路径变量中的大括号
        String stringUrls = element.toString().replaceAll("^\\{|\\}$", "");
        String[] urlList = stringUrls.split(",");


        return Arrays.stream(urlList)
            .map(String::trim)
            .map(s -> s.replaceAll("^\"|\"$", ""))
            .toList();
    }

    /**
     * 解析Mapping注解中的HTTP方法类型
     * @param mappingAnnotation Mapping注解
     * @return HTTP方法类型
     */
    public static EntryMappingRules.HttpMethod parseHttpMethodType(Annotation mappingAnnotation) {
        Element element = mappingAnnotation.getElement(AnnotationElementKeys.METHOD);
        if (element == null) {
            return EntryMappingRules.HttpMethod.ANY;
        }

        try {
            // 从 {org.springframework.web.bind.annotation.RequestMethod.POST} 提取 POST
            String elementStr = element.toString();

            // 使用正则提取方法名
            Pattern pattern = Pattern.compile("RequestMethod\\.(\\w+)");
            Matcher matcher = pattern.matcher(elementStr);

            if (matcher.find()) {
                String methodType = matcher.group(1);
                return EntryMappingRules.HttpMethod.valueOf(methodType);
            }

            logger.warn("Cannot extract HTTP-Request-Method Type from: {}", elementStr);
            return EntryMappingRules.HttpMethod.ANY;

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid HTTP method in annotation, using ANY as default", e);
            return EntryMappingRules.HttpMethod.ANY;
        } catch (Exception e) {
            logger.error("Unexpected error while parsing HTTP method", e);
            return EntryMappingRules.HttpMethod.ANY;
        }
    }
}