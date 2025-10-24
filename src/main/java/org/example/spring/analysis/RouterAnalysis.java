package org.example.spring.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.spring.router.ControllerClass;
import org.example.spring.router.ControllerParser;
import org.example.spring.router.RouterMethod;
import org.example.spring.router.RouterMethodParser;
import org.example.spring.rules.EntryControllerRules;
import org.example.spring.rules.EntryMappingRules;
import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Router analysis to extract urls from controller classes
 */
public class RouterAnalysis extends ProgramAnalysis {

    private static final Logger logger = LogManager.getLogger(RouterAnalysis.class);

    public static final String ID = "routerAnalysis";

    private static final List<String> CONTROLLER_ANNOTATIONS = List.of(
        EntryControllerRules.CONTROLLER,
        EntryControllerRules.REST_CONTROLLER
    );

    private static final List<String> MAPPING_ANNOTATIONS = List.of(
        EntryMappingRules.REQUEST_MAPPING,
        EntryMappingRules.GET_MAPPING,
        EntryMappingRules.POST_MAPPING,
        EntryMappingRules.PUT_MAPPING,
        EntryMappingRules.DELETE_MAPPING,
        EntryMappingRules.PATCH_MAPPING
    );

    private final List<JClass> applicationClasses;

    private final List<ControllerClass> controllerClasses;

    public RouterAnalysis(AnalysisConfig config) {
        super(config);
        this.applicationClasses = World.get()
            .getClassHierarchy()
            .applicationClasses()
            .toList();
        this.controllerClasses = new ArrayList<>();
    }

    @Override
    public Object analyze() {
        LocalDateTime startTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        logger.info("Router analysis started at: {}", startTime.format(formatter));

        extractUrls();

        LocalDateTime endTime = LocalDateTime.now();
        long duration = java.time.Duration.between(startTime, endTime).toMillis();

        logger.info("Router analysis completed at: {} (Duration: {}ms)",
            endTime.format(formatter), duration);

        return List.copyOf(controllerClasses);
    }


    private void extractUrls() {
        applicationClasses.forEach(jClass -> {
            if (isController(jClass)) {
                ControllerClass controller = new ControllerClass(jClass);
                List<String> baseUrls = ControllerParser.parseBaseUrl(controller);
                controller.setBaseUrls(baseUrls);

                Collection<JMethod> declaredMethods = jClass.getDeclaredMethods();
                declaredMethods.forEach(jMethod -> {
                    if (isRouterMethod(jMethod)) {
                        EntryMappingRules.HttpMethod httpMethod = RouterMethodParser.parseHttpMethodType(jMethod);
                        RouterMethod routerMethod = new RouterMethod(jMethod, httpMethod);
                        List<String> urls = RouterMethodParser.parseUrls(jMethod);
                        routerMethod.setUrls(urls);
                        controller.addRouterMethod(routerMethod);
                    }
                });

                controllerClasses.add(controller);
            }
        });
    }

    private boolean isController(JClass jClass) {
        return CONTROLLER_ANNOTATIONS.stream()
            .anyMatch(jClass::hasAnnotation);
    }

    private boolean isRouterMethod(JMethod jMethod) {
        return MAPPING_ANNOTATIONS.stream()
            .anyMatch(jMethod::hasAnnotation);
    }
}
