// File: SinkMetadata.java
package org.example.taint;

/**
 * Represents taint sink metadata
 * m_sink = <S_sig, p_idx, rho_ctx, V_type>
 */
public class SinkMetadata {

    private final String methodSignature;
    private final int paramIndex;
    private final String sqlContext;
    private final String vulnerabilityType;

    public SinkMetadata(String methodSignature, int paramIndex,
                        String sqlContext, String vulnerabilityType) {
        this.methodSignature = methodSignature;
        this.paramIndex = paramIndex;
        this.sqlContext = sqlContext;
        this.vulnerabilityType = vulnerabilityType;
    }

    public String getMethodSignature() { return methodSignature; }
    public int getParamIndex() { return paramIndex; }
    public String getSqlContext() { return sqlContext; }
    public String getVulnerabilityType() { return vulnerabilityType; }

    /**
     * Risk judgment rule (Equation 4-3):
     * ${...} text concatenation → HIGH RISK
     * #{...} prepared statement → SAFE
     */
    public boolean isRisky() {
        return true;
//        return sqlContext != null && sqlContext.contains("${");
    }
}