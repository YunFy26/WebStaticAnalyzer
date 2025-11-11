package org.example.spring.plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * 接口到具体实现类的映射配置
 * TODO：做成配置文件
 */
public class EntryParaInterfaceMapper {

    private static final Map<String, String> INTERFACE_MAPPINGS = new HashMap<>();

    static {
        // Javax Servlet 相关映射 (Java EE <= 8)
        INTERFACE_MAPPINGS.put("javax.servlet.http.HttpServletRequest",
            "javax.servlet.http.HttpServletRequestWrapper");
        INTERFACE_MAPPINGS.put("javax.servlet.ServletRequest",
            "javax.servlet.http.HttpServletRequestWrapper");
        INTERFACE_MAPPINGS.put("javax.servlet.http.HttpServletResponse",
            "javax.servlet.http.HttpServletResponseWrapper");
        INTERFACE_MAPPINGS.put("javax.servlet.ServletResponse",
            "javax.servlet.http.HttpServletResponseWrapper");

        // Jakarta Servlet 相关映射 (Jakarta EE 9+)
        INTERFACE_MAPPINGS.put("jakarta.servlet.http.HttpServletRequest",
            "jakarta.servlet.http.HttpServletRequestWrapper");
        INTERFACE_MAPPINGS.put("jakarta.servlet.ServletRequest",
            "jakarta.servlet.http.HttpServletRequestWrapper");
        INTERFACE_MAPPINGS.put("jakarta.servlet.http.HttpServletResponse",
            "jakarta.servlet.http.HttpServletResponseWrapper");
        INTERFACE_MAPPINGS.put("jakarta.servlet.ServletResponse",
            "jakarta.servlet.http.HttpServletResponseWrapper");

        // Spring Web 相关映射
        INTERFACE_MAPPINGS.put("org.springframework.web.context.request.WebRequest",
            "org.springframework.web.context.request.ServletWebRequest");
        INTERFACE_MAPPINGS.put("org.springframework.web.context.request.NativeWebRequest",
            "org.springframework.web.context.request.ServletWebRequest");
    }

    /**
     * 为接口类型返回具体的实现类类型
     */
    public static String getConcreteType(String type) {
        return INTERFACE_MAPPINGS.getOrDefault(type, type);
    }
}
