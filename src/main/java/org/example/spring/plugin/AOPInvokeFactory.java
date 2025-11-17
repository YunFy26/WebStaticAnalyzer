package org.example.spring.plugin;

import org.example.spring.analysis.aop.AspectMethod;
import pascal.taie.language.classes.JMethod;

/**
 * 创建虚拟 AOP 调用的工厂类
 */
public class AOPInvokeFactory {

    /**
     * 为切面方法创建虚拟的 Invoke 语句
     */
    public static VirtualAOPInvoke createVirtualInvoke(JMethod container,
                                                       AspectMethod aspectMethod) {
        return new VirtualAOPInvoke(container, aspectMethod.getMethod(), aspectMethod.getAdviceType());
    }
}

