package org.example.spring.rules;

public class JPARules {

    // --- 实体类标识 (javax.persistence.*) ---
    // Spring Boot 3+
    /**
     * 作用在类上
     */
    public static final String JAKARTA_ENTITY = "javax.persistence.Entity";
    public static final String JAKARTA_TABLE = "javax.persistence.Table";
    public static final String JAKARTA_MAPPED_SUPERCLASS = "javax.persistence.MappedSuperclass";
    public static final String JAKARTA_EMBEDDABLE = "javax.persistence.Embeddable";

    public static final String JAKARTA_ID = "javax.persistence.Id";
    public static final String JAKARTA_COLUMN = "javax.persistence.Column";

    /**
     * 作用在类上
     */
    public static final String JAVAX_ENTITY = "javax.persistence.Entity";
    public static final String JAVAX_TABLE = "javax.persistence.Table";
    public static final String JAVAX_MAPPED_SUPERCLASS = "javax.persistence.MappedSuperclass";
    public static final String JAVAX_EMBEDDABLE = "javax.persistence.Embeddable";

    public static final String JAVAX_ID = "javax.persistence.Id";
    public static final String JAVAX_COLUMN = "javax.persistence.Column";

    // --- 接口继承类型 (Spring Data JPA) ---
    // 如果你在扫描接口，这些是必须处理的父接口

    public static final String JPA_REPOSITORY = "org.springframework.data.jpa.repository.JpaRepository";
    public static final String CRUD_REPOSITORY = "org.springframework.data.repository.CrudRepository";
    public static final String PAGING_AND_SORTING_REPOSITORY = "org.springframework.data.repository.PagingAndSortingRepository";

    // 通用 Repository 注解
    public static final String REPOSITORY_ANNOTATION = "org.springframework.stereotype.Repository";
}