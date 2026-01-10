package org.example.spring.rules;

/**
 * 定时器组件规则 TODO
 */
public class QuartzRules {

    public static final String JOB = "org.quartz.Job";

    // --- 行为控制注解 ---
    // 禁止并发执行（同一个 JobDetail 定义的多个实例不能同时运行）
    public static final String DISALLOW_CONCURRENT_EXECUTION = "org.quartz.DisallowConcurrentExecution";

    // 执行完后持久化 JobDataMap
    public static final String PERSIST_JOB_DATA_AFTER_EXECUTION = "org.quartz.PersistJobDataAfterExecution";

    // 在 JTA 事务中执行
    public static final String EXECUTE_IN_JTA_TRANSACTION = "org.quartz.ExecuteInJTATransaction";
}
