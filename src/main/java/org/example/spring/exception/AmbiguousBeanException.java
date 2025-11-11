package org.example.spring.exception;

import org.example.spring.analysis.di.bean.BeanClass;
import pascal.taie.language.classes.JClass;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 当依赖注入存在多个候选 Bean 但无法确定使用哪个时抛出的异常
 */
public class AmbiguousBeanException extends RuntimeException {

  private final String interfaceOrClassName;
  private final String fieldName;
  private final List<String> candidateBeanNames;
  private final int candidateCount;

  /**
   * 为字段注入找到多个候选 Bean 但无法确定时构造异常
   */
  public AmbiguousBeanException(JClass interfaceOrAbstractClass,
                                String fieldName,
                                List<BeanClass> candidates) {
    super(buildMessage(interfaceOrAbstractClass, fieldName, candidates));
    this.interfaceOrClassName = interfaceOrAbstractClass.getName();
    this.fieldName = fieldName;
    this.candidateCount = candidates.size();
    this.candidateBeanNames = candidates.stream()
        .map(bean -> bean.getDefaultBeanName() != null ?
            bean.getDefaultBeanName() : bean.getjClass().getName())
        .collect(Collectors.toList());
  }

  private static String buildMessage(JClass type, String fieldName, List<BeanClass> candidates) {
    String candidateInfo = candidates.stream()
        .map(bean -> {
          String beanName = bean.getDefaultBeanName() != null ?
              bean.getDefaultBeanName() : bean.getjClass().getName();
          return String.format("  - %s (type: %s, primary: %s)",
              beanName,
              bean.getjClass().getName(),
              bean.isPrimary());
        })
        .collect(Collectors.joining("\n"));

    return String.format(
        "Multiple implementations (%d) found for field '%s' of type: %s.\n" +
            "None is marked as @Primary and field name doesn't match any bean name.\n" +
            "Candidates:\n%s",
        candidates.size(), fieldName, type.getName(), candidateInfo
    );
  }

  public String getInterfaceOrClassName() {
    return interfaceOrClassName;
  }

  public String getFieldName() {
    return fieldName;
  }

  public List<String> getCandidateBeanNames() {
    return candidateBeanNames;
  }

  public int getCandidateCount() {
    return candidateCount;
  }
}
