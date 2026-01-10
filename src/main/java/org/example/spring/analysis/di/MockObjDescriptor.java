package org.example.spring.analysis.di;

import pascal.taie.analysis.pta.core.heap.Descriptor;

public class MockObjDescriptor {

    // 私有构造，防止实例化
    private MockObjDescriptor() {}

    /**
     * 用于标记依赖注入的 Bean 对象
     * TODO: 区分Controller、Service、Repository等不同类型的Bean？
     */
    public static final Descriptor DI_OBJ = () -> "DiObj";

    /**
     * 用于标记切面相关的对象
     */
    public static final Descriptor ASPECT_OBJ = () -> "AspectObj";

    /**
     * MyBatis 相关的对象
     */
    public static final Descriptor MYBATIS_OBJ = () -> "MyBatisObj";

    /**
     * 用于标记普通共享参数对象，如 String, Array 等
     * TODO: HttpServletRequest 算吗
     */
    public static final Descriptor COMMON_SHARED_PARAM_OBJ = () -> "CommonSharedParamObj";
}
