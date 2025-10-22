package org.example.spring.router;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.spring.analysis.RouterAnalysis;
import pascal.taie.World;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Pretty printer for router analysis results
 */
public class RouterPrinter {

    private static final Logger logger = LogManager.getLogger(RouterPrinter.class);

    private static final String SEPARATOR = "═".repeat(120);

    private static final String LINE = "─".repeat(120);

    public static void printRouterInfo() {

        List<ControllerClass> controllerClasses = World.get().getResult(RouterAnalysis.ID);

        if (controllerClasses == null || controllerClasses.isEmpty()) {
            logger.warn("No controller classes found");
            return;
        }

        StringBuilder output = new StringBuilder();

        output.append("\n\n");
        output.append(SEPARATOR).append("\n");
        output.append(String.format("║ %-116s ║\n", "🎯 Spring MVC Router Analysis Report"));
        output.append(String.format("║ %-116s ║\n", "📊 Total Controllers: " + controllerClasses.size()));
        output.append(SEPARATOR).append("\n\n");

        int totalRoutes = 0;
        for (ControllerClass controller : controllerClasses) {
            totalRoutes += printController(output, controller);
        }

        output.append("\n").append(SEPARATOR).append("\n");
        output.append(String.format("║ %-116s ║\n",
            String.format("✅ Summary: %d Controllers, %d Routes", controllerClasses.size(), totalRoutes)));
        output.append(SEPARATOR).append("\n");

        logger.info(output.toString());
    }

    private static int printController(StringBuilder output, ControllerClass controller) {
        String className = controller.getJClass().getName();
        List<String> baseUrls = controller.getBaseUrls();
        List<RouterMethod> routerMethods = controller.getRouterMethods();

        output.append("┌").append(LINE).append("┐\n");
        output.append(String.format("│ 📦 Controller: %-102s │\n", className));
        output.append(String.format("│ 🔗 Base URLs: %-103s │\n",
            baseUrls.isEmpty() ? "/" : String.join(", ", baseUrls)));
        output.append(String.format("│ 📍 Total Routes: %-100d │\n", routerMethods.size()));
        output.append("├").append(LINE).append("┤\n");

        if (routerMethods.isEmpty()) {
            output.append(String.format("│ %-118s │\n", "⚠️  No route methods found"));
        } else {
            for (int i = 0; i < routerMethods.size(); i++) {
                printRouterMethod(output, routerMethods.get(i), baseUrls, i + 1);
                if (i < routerMethods.size() - 1) {
                    output.append("├").append(LINE).append("┤\n");
                }
            }
        }

        output.append("└").append(LINE).append("┘\n\n");
        return routerMethods.size();
    }

    private static void printRouterMethod(StringBuilder output, RouterMethod routerMethod,
                                          List<String> baseUrls, int index) {
        JMethod jMethod = routerMethod.getJMethod();
        String methodName = jMethod.getName();
        List<String> urls = routerMethod.getUrls();

        List<String> fullUrls = buildFullUrls(baseUrls, urls);

        output.append(String.format("│ ⚡ [%d] Method: %-102s │\n", index, methodName));
        output.append(String.format("│     🔸 Signature: %-99s │\n", jMethod.getSignature()));
        output.append(String.format("│     🌐 URL Patterns: %-95s │\n",
            fullUrls.isEmpty() ? "N/A" : fullUrls.get(0)));

        for (int i = 1; i < fullUrls.size(); i++) {
            output.append(String.format("│                       %-95s │\n", fullUrls.get(i)));
        }

        List<Type> paramTypes = jMethod.getParamTypes();
        if (paramTypes.isEmpty()) {
            output.append(String.format("│     📝 Parameters: %-98s │\n", "None"));
        } else {
            output.append(String.format("│     📝 Parameters: %-98s │\n", ""));
            for (int i = 0; i < paramTypes.size(); i++) {
                String paramInfo = formatParameter(jMethod, i);
                output.append(String.format("│         [%d] %-105s │\n", i, paramInfo));
            }
        }
    }

    private static String formatParameter(JMethod jMethod, int paramIndex) {
        Type paramType = jMethod.getParamTypes().get(paramIndex);
        String paramName = String.format("param%d", paramIndex);

        Collection<Annotation> annotations = jMethod.getParamAnnotations(paramIndex);
        String annotationStr = annotations.isEmpty()
            ? ""
            : annotations.stream()
            .map(ann -> {
                String simpleName = ann.getType().substring(ann.getType().lastIndexOf('.') + 1);
                return "@" + simpleName;
            })
            .collect(Collectors.joining(" ")) + " ";

        return String.format("%s%s %s", annotationStr, paramType, paramName);
    }

    private static List<String> buildFullUrls(List<String> baseUrls, List<String> methodUrls) {
        if (methodUrls.isEmpty()) {
            return baseUrls.isEmpty() ? List.of("/") : baseUrls;
        }

        if (baseUrls.isEmpty()) {
            return methodUrls;
        }

        return baseUrls.stream()
            .flatMap(base -> methodUrls.stream()
                .map(method -> normalizePath(base + method)))
            .collect(Collectors.toList());
    }

    private static String normalizePath(String path) {
        return path.replaceAll("/+", "/");
    }
}
