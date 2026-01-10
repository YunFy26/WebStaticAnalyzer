package org.example.spring.plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * 运行时具体实现类映射器
 * <p>
 * 用于维护【静态接口类型】到【运行时常见具体实现类型】的映射关系。
 * 在静态分析或构造测试上下文时，通过此映射查找接口的默认或最常用的实现类。
 * </p>
 */
public class RuntimeImplementationMapper {

    private static final Map<String, String> INTERFACE_MAPPINGS = new HashMap<>();

    static {
        // ==========================================
        // 1. Java EE / Jakarta EE Servlet (Web 基础)
        // ==========================================

        // Javax (Java EE <= 8)
        INTERFACE_MAPPINGS.put("javax.servlet.http.HttpServletRequest",
            "javax.servlet.http.HttpServletRequestWrapper");
        INTERFACE_MAPPINGS.put("javax.servlet.ServletRequest",
            "javax.servlet.http.HttpServletRequestWrapper");
        INTERFACE_MAPPINGS.put("javax.servlet.http.HttpServletResponse",
            "javax.servlet.http.HttpServletResponseWrapper");
        INTERFACE_MAPPINGS.put("javax.servlet.ServletResponse",
            "javax.servlet.http.HttpServletResponseWrapper");
        INTERFACE_MAPPINGS.put("javax.servlet.http.HttpSession",
            "org.apache.catalina.session.StandardSession"); // Tomcat 默认实现

        // Jakarta (Jakarta EE 9+)
        INTERFACE_MAPPINGS.put("jakarta.servlet.http.HttpServletRequest",
            "jakarta.servlet.http.HttpServletRequestWrapper");
        INTERFACE_MAPPINGS.put("jakarta.servlet.ServletRequest",
            "jakarta.servlet.http.HttpServletRequestWrapper");
        INTERFACE_MAPPINGS.put("jakarta.servlet.http.HttpServletResponse",
            "jakarta.servlet.http.HttpServletResponseWrapper");
        INTERFACE_MAPPINGS.put("jakarta.servlet.ServletResponse",
            "jakarta.servlet.http.HttpServletResponseWrapper");
        INTERFACE_MAPPINGS.put("jakarta.servlet.http.HttpSession",
            "org.apache.catalina.session.StandardSession");

        // ==========================================
        // 2. Spring Framework (Web & AOP)
        // ==========================================

        // Spring MVC 请求对象
        INTERFACE_MAPPINGS.put("org.springframework.web.context.request.WebRequest",
            "org.springframework.web.context.request.ServletWebRequest");
        INTERFACE_MAPPINGS.put("org.springframework.web.context.request.NativeWebRequest",
            "org.springframework.web.context.request.ServletWebRequest");

        // 文件上传
        INTERFACE_MAPPINGS.put("org.springframework.web.multipart.MultipartFile",
            "org.springframework.web.multipart.support.StandardMultipartFile");

        // AOP 切点 (AspectJ)
        INTERFACE_MAPPINGS.put("org.aspectj.lang.JoinPoint",
            "org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint");
        INTERFACE_MAPPINGS.put("org.aspectj.lang.ProceedingJoinPoint",
            "org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint");

        // Spring Cache
        INTERFACE_MAPPINGS.put("org.springframework.cache.CacheManager",
            "org.springframework.cache.concurrent.ConcurrentMapCacheManager"); // 默认内存实现
        INTERFACE_MAPPINGS.put("org.springframework.cache.Cache",
            "org.springframework.cache.concurrent.ConcurrentMapCache");

        // ==========================================
        // 3. JPA (Spring Data JPA / Hibernate)
        // ==========================================

        // Spring Data JPA 的 Repository 接口，运行时实际上是由 SimpleJpaRepository 支持的代理
        INTERFACE_MAPPINGS.put("org.springframework.data.jpa.repository.JpaRepository",
            "org.springframework.data.jpa.repository.support.SimpleJpaRepository");
        INTERFACE_MAPPINGS.put("org.springframework.data.repository.CrudRepository",
            "org.springframework.data.jpa.repository.support.SimpleJpaRepository");
        INTERFACE_MAPPINGS.put("org.springframework.data.repository.PagingAndSortingRepository",
            "org.springframework.data.jpa.repository.support.SimpleJpaRepository");

        // JPA 核心管理器 (通常映射到 Hibernate 的实现)
        INTERFACE_MAPPINGS.put("javax.persistence.EntityManager",
            "org.hibernate.internal.SessionImpl");
        INTERFACE_MAPPINGS.put("javax.persistence.EntityManagerFactory",
            "org.hibernate.internal.SessionFactoryImpl");

        // ==========================================
        // 4. MyBatis
        // ==========================================

        // MyBatis 的核心会话
        INTERFACE_MAPPINGS.put("org.apache.ibatis.session.SqlSession",
            "org.apache.ibatis.session.defaults.DefaultSqlSession");

        // 注意：MyBatis 的 Mapper 接口 (如 UserMapper) 运行时是 JDK 动态代理 ($ProxyN)
        // 这里的映射较为困难，因为没有固定的静态类。但其核心执行代理逻辑位于 MapperProxy
        INTERFACE_MAPPINGS.put("org.apache.ibatis.binding.MapperProxy",
            "org.apache.ibatis.binding.MapperProxy");

        // ==========================================
        // 5. Apache Shiro
        // ==========================================

        // 当前用户主体 (Web 环境下最常用实现)
        INTERFACE_MAPPINGS.put("org.apache.shiro.subject.Subject",
            "org.apache.shiro.web.subject.support.WebDelegatingSubject");

        // 安全管理器
        INTERFACE_MAPPINGS.put("org.apache.shiro.mgt.SecurityManager",
            "org.apache.shiro.web.mgt.DefaultWebSecurityManager");

        // 会话
        INTERFACE_MAPPINGS.put("org.apache.shiro.session.Session",
            "org.apache.shiro.web.session.mgt.WebSession"); // 或 SimpleSession

        // 缓存 (Shiro 默认无缓存实现，通常接 EhCache 或 MapCache)
        INTERFACE_MAPPINGS.put("org.apache.shiro.cache.Cache",
            "org.apache.shiro.cache.MapCache"); // 最基本的内存实现

        // ==========================================
        // 6. Quartz Scheduler
        // ==========================================

        // 调度器
        INTERFACE_MAPPINGS.put("org.quartz.Scheduler",
            "org.quartz.impl.StdScheduler");

        // 任务执行上下文 (Job 运行时传入的参数)
        INTERFACE_MAPPINGS.put("org.quartz.JobExecutionContext",
            "org.quartz.impl.JobExecutionContextImpl");
    }

    /**
     * 根据接口类型获取其运行时可能的具体实现类类型
     *
     * @param interfaceType 接口全限定名 (e.g., "javax.servlet.http.HttpServletRequest")
     * @return 对应的具体实现类全限定名，如果未配置映射则返回原接口名
     */
    public static String getConcreteType(String interfaceType) {
        return INTERFACE_MAPPINGS.getOrDefault(interfaceType, interfaceType);
    }

    /**
     * 判断某个类型是否有已知的具体实现映射
     */
    public static boolean hasConcreteMapping(String interfaceType) {
        return INTERFACE_MAPPINGS.containsKey(interfaceType);
    }
}