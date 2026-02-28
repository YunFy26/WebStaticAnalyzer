package org.example.spring.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.spring.analysis.aop.AspectClass;
import org.example.spring.analysis.aop.AspectMethod;
import org.example.spring.analysis.di.bean.BeanClass;
import org.example.spring.analysis.router.ControllerClass;
import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.language.classes.JMethod;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 结果汇总分析
 * 负责收集 Router, Bean, Aspect 的分析结果并输出统计信息
 */
public class ResultAnalysis extends ProgramAnalysis {

    public static final String ID = "resultAnalysis";

    private static final Logger logger = LogManager.getLogger(ResultAnalysis.class);

    public ResultAnalysis(AnalysisConfig config) {
        super(config);
    }

    @Override
    public Object analyze() {
        // 1. 获取各模块的分析结果
        // 注意：ResultAnalysis 需要在 analysis-plan.yml 中配置为依赖于 router, bean, aspect 分析
        List<ControllerClass> routersResult = World.get().getResult(RouterAnalysis.ID);
        Collection<BeanClass> beansResult = World.get().getResult(BeanAnalysis.ID);
        List<AspectClass> aspectsResult = World.get().getResult(AspectAnalysis.ID);

        // 2. 统计 Router 方法数量
        int routerMethodCount = 0;
        if (routersResult != null) {
            routerMethodCount = routersResult.stream()
                .mapToInt(controller -> controller.getRouterMethods().size())
                .sum();
        }

        // 3. 统计 Bean 数量
        int beanCount = 0;
        if (beansResult != null) {
            beanCount = beansResult.size();
        }

        // 4. 统计切面方法 (Advice) 数量
        // 使用 Set<JMethod> 进行去重，防止因同一个通知方法绑定多个切点而被重复统计
        int aspectMethodCount = 0;
        if (aspectsResult != null) {
            Set<JMethod> uniqueAdviceMethods = new HashSet<>();
            for (AspectClass aspectClass : aspectsResult) {
                // 遍历 Map<Pointcut, List<AspectMethod>>
                aspectClass.getPointcutMethodMap().values().stream()
                    .flatMap(List::stream)
                    .map(AspectMethod::getMethod)
                    .forEach(uniqueAdviceMethods::add);
            }
            aspectMethodCount = uniqueAdviceMethods.size();
        }

        // 5. 输出统计日志
        logger.info("============================================================");
        logger.info("                 Spring Analysis Result Summary             ");
        logger.info("============================================================");
        logger.info(String.format("%-30s : %d", "Router Methods (Endpoints)", routerMethodCount));
        logger.info(String.format("%-30s : %d", "Spring Beans", beanCount));
        logger.info(String.format("%-30s : %d", "Aspect Methods (Advice)", aspectMethodCount));
        logger.info("============================================================");

        // 返回一个简单的统计对象或字符串供后续可能的分析使用
        return String.format("Routers: %d, Beans: %d, Aspects: %d",
            routerMethodCount, beanCount, aspectMethodCount);
    }
}