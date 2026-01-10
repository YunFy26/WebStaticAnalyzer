package org.example.spring.rules;

/**
 * TODO
 */
public class ShiroRules {

    public static final String REALM = "org.apache.shiro.realm.Realm";

    // --- 权限/认证注解 ---
    // 只有具备这些注解的方法或类才会被 Shiro 拦截检查
    public static final String REQUIRES_PERMISSIONS = "org.apache.shiro.authz.annotation.RequiresPermissions";
    public static final String REQUIRES_ROLES = "org.apache.shiro.authz.annotation.RequiresRoles";
    public static final String REQUIRES_AUTHENTICATION = "org.apache.shiro.authz.annotation.RequiresAuthentication";
    public static final String REQUIRES_USER = "org.apache.shiro.authz.annotation.RequiresUser";
    public static final String REQUIRES_GUEST = "org.apache.shiro.authz.annotation.RequiresGuest";
}
