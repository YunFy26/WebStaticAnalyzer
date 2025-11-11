package org.example.spring.plugin.aop;

import org.example.spring.analysis.aop.AspectMethod.AdviceType;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.InvokeStatic;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.exp.ExpVisitor;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 虚拟 AOP 调用点
 * 用于表示从目标方法到切面通知方法的虚拟调用
 */
public class VirtualAOPInvoke extends Invoke {

    private final AdviceType adviceType;

    public VirtualAOPInvoke(JMethod targetMethod, JMethod adviceMethod, AdviceType adviceType) {
        super(targetMethod, new VirtualInvokeExp(adviceMethod.getRef()), null);
        this.adviceType = adviceType;
    }

    public AdviceType getAdviceType() {
        return adviceType;
    }

    @Override
    public String toString() {
        return String.format("[AOP:%s] virtual invoke %s from %s",
            adviceType,
            getMethodRef().getSubsignature(),
            getContainer().getSignature());
    }

    /**
     * 虚拟的 InvokeExp 实现
     */
    private static class VirtualInvokeExp extends InvokeStatic {

        VirtualInvokeExp(MethodRef methodRef) {
            super(methodRef, Collections.emptyList());
        }

        @Override
        public <T> T accept(ExpVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public Set<RValue> getUses() {
            return Collections.emptySet();
        }

        @Override
        public String toString() {
            return "virtual invoke " + getMethodRef().getSubsignature();
        }
    }
}
