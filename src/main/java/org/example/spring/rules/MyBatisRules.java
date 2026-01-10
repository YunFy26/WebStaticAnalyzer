package org.example.spring.rules;

public class MyBatisRules {

    // --- 核心标识 ---
    public static final String MAPPER = "org.apache.ibatis.annotations.Mapper";

    // --- CRUD 操作 ---
    public static final String SELECT = "org.apache.ibatis.annotations.Select";
    public static final String INSERT = "org.apache.ibatis.annotations.Insert";
    public static final String UPDATE = "org.apache.ibatis.annotations.Update";
    public static final String DELETE = "org.apache.ibatis.annotations.Delete";

    // --- 动态 SQL 提供者 ---
    public static final String SELECT_PROVIDER = "org.apache.ibatis.annotations.SelectProvider";
    public static final String INSERT_PROVIDER = "org.apache.ibatis.annotations.InsertProvider";
    public static final String UPDATE_PROVIDER = "org.apache.ibatis.annotations.UpdateProvider";
    public static final String DELETE_PROVIDER = "org.apache.ibatis.annotations.DeleteProvider";

    // --- 参数与结果映射 ---
    public static final String PARAM = "org.apache.ibatis.annotations.Param";
    public static final String RESULTS = "org.apache.ibatis.annotations.Results";
    public static final String RESULT = "org.apache.ibatis.annotations.Result";

    // --- MyBatis 自身缓存注解 ---
    public static final String CACHE_NAMESPACE = "org.apache.ibatis.annotations.CacheNamespace";
    public static final String CACHE_NAMESPACE_REF = "org.apache.ibatis.annotations.CacheNamespaceRef";
}
