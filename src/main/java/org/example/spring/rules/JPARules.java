package org.example.spring.rules;

public class JPARules {

    // 实体相关注解
    public static final String ENTITY = "javax.persistence.Entity";

    public static final String TABLE = "javax.persistence.Table";

    public static final String MAPPED_SUPERCLASS = "javax.persistence.MappedSuperclass";

    public static final String EMBEDDABLE = "javax.persistence.Embeddable";

    // 主键相关注解
    public static final String ID = "javax.persistence.Id";

    public static final String EMBEDDED_ID = "javax.persistence.EmbeddedId";

    public static final String GENERATED_VALUE = "javax.persistence.GeneratedValue";

    // 字段映射注解
    public static final String COLUMN = "javax.persistence.Column";

    public static final String TRANSIENT = "javax.persistence.Transient";

    public static final String TEMPORAL = "javax.persistence.Temporal";

    public static final String LOB = "javax.persistence.Lob";

    // 关系映射注解
    public static final String ONE_TO_ONE = "javax.persistence.OneToOne";

    public static final String ONE_TO_MANY = "javax.persistence.OneToMany";

    public static final String MANY_TO_ONE = "javax.persistence.ManyToOne";

    public static final String MANY_TO_MANY = "javax.persistence.ManyToMany";

    public static final String JOIN_COLUMN = "javax.persistence.JoinColumn";

    public static final String JOIN_TABLE = "javax.persistence.JoinTable";

}