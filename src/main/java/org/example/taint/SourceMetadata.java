// File: SourceMetadata.java
package org.example.taint;

import java.util.List;
import java.util.Set;

/**
 * Represents fine-grained taint source metadata
 * m_src = <S_sig, p_idx, lambda_path, tau_field, A>
 */
public class SourceMetadata {

    private final String methodSignature;
    private final int paramIndex;
    private final List<String> accessPath;
    private final String fieldType;
    private final Set<String> annotations;
    private final String fieldName;

    public SourceMetadata(String methodSignature, int paramIndex,
                          List<String> accessPath, String fieldType,
                          Set<String> annotations, String fieldName) {
        this.methodSignature = methodSignature;
        this.paramIndex = paramIndex;
        this.accessPath = accessPath;
        this.fieldType = fieldType;
        this.annotations = annotations;
        this.fieldName = fieldName;
    }

    public String getMethodSignature() { return methodSignature; }
    public int getParamIndex() { return paramIndex; }
    public List<String> getAccessPath() { return accessPath; }
    public String getFieldType() { return fieldType; }
    public Set<String> getAnnotations() { return annotations; }
    public String getFieldName() { return fieldName; }

    public boolean isSimpleParam() {
        return accessPath.isEmpty();
    }
}