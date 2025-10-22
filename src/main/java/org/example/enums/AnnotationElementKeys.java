package org.example.enums;

/**
 * Annotation Element Keys
 * TODO : java doc注解格式修正
 * such as:
 * @RequestMapping(value = "/api")
 * Here, "value" is the annotation element key.
 */
public class AnnotationElementKeys {

    // value 与 path 互为别名 @RequestMapping(path = "/api")
    public static final String VALUE = "value";

    public static final String METHOD = "method";

}
