package org.example.printer.router.console;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.spring.router.ControllerClass;
import org.example.spring.router.RouterMethod;
import org.example.utils.StringUtils;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.example.printer.router.console.ConsoleColors.*;

/**
 * Console printer for router analysis results
 */
public class RouterAnalysisResultToConsole {

    private static final Logger logger = LogManager.getLogger(RouterAnalysisResultToConsole.class);

    // 精确列宽定义 (包括空格)
    private static final int COL_NO = 6;

    private static final int COL_URL = 52;

    private static final int COL_HTTP = 13;

    private static final int COL_METHOD = 40;

    private static final int COL_PARAM = 26;

    // 总宽度 = 各列宽度之和 + 6个分隔符
    private static final int TOTAL_WIDTH = COL_NO + COL_URL + COL_HTTP + COL_METHOD + COL_PARAM + 6;

    // =分隔符
    private static final String SEPARATOR = "═".repeat(TOTAL_WIDTH - 2);

    // -分隔符
    private static final String LINE = "─".repeat(TOTAL_WIDTH - 2);

    public static void printToConsole(List<ControllerClass> controllerClasses) {
        StringBuilder output = new StringBuilder();

        output.append("\n\n");
        output.append(colored("╔" + SEPARATOR + "╗", BRIGHT_CYAN)).append("\n");

        String title = "🎯 Spring MVC Router Analysis Report";
        int titleVisibleWidth = StringUtils.calculateVisibleWidth(title);
        int titlePaddingNeeded = TOTAL_WIDTH - 2 - titleVisibleWidth;
        output.append(colored("║", BRIGHT_CYAN))
            .append(colored(" " + title + " ".repeat(titlePaddingNeeded - 1), BOLD_PURPLE))
            .append(colored("║", BRIGHT_CYAN)).append("\n");

        int totalRoutes = controllerClasses.stream()
            .mapToInt(c -> c.getRouterMethods().size())
            .sum();

        String stats = String.format("📊 Total Controllers: %d | Total Routes: %d",
            controllerClasses.size(), totalRoutes);
        int statsVisibleWidth = StringUtils.calculateVisibleWidth(stats);
        int statsPaddingNeeded = TOTAL_WIDTH - 2 - statsVisibleWidth;
        output.append(colored("║", BRIGHT_CYAN))
            .append(colored(" " + stats + " ".repeat(statsPaddingNeeded - 1), BOLD_CYAN))
            .append(colored("║", BRIGHT_CYAN)).append("\n");
        output.append(colored("╚" + SEPARATOR + "╝", BRIGHT_CYAN)).append("\n\n");

        for (int i = 0; i < controllerClasses.size(); i++) {
            printControllerTable(output, controllerClasses.get(i), i + 1);
            if (i < controllerClasses.size() - 1) {
                output.append("\n");
            }
        }

        output.append("\n").append(colored("╔" + SEPARATOR + "╗", BRIGHT_CYAN)).append("\n");
        String complete = String.format("✅ Analysis Complete: %d Controllers, %d Routes",
            controllerClasses.size(), totalRoutes);
        int completeVisibleWidth = StringUtils.calculateVisibleWidth(complete);
        int completePaddingNeeded = TOTAL_WIDTH - 2 - completeVisibleWidth;
        output.append(colored("║", BRIGHT_CYAN))
            .append(colored(" " + complete + " ".repeat(completePaddingNeeded - 1), BOLD_GREEN))
            .append(colored("║", BRIGHT_CYAN)).append("\n");
        output.append(colored("╚" + SEPARATOR + "╝", BRIGHT_CYAN)).append("\n");

        logger.info(output.toString());
    }

    private static void printControllerTable(StringBuilder output, ControllerClass controller, int index) {
        String className = controller.getJClass().getName();
        List<String> baseUrls = controller.getBaseUrls();
        List<RouterMethod> routerMethods = controller.getRouterMethods();

        output.append(colored("┌" + LINE + "┐", BRIGHT_BLUE)).append("\n");
        output.append(colored("│", BRIGHT_BLUE))
            .append(colored(String.format(" 📦 Controller [%d]: ", index), BOLD_YELLOW))
            .append(colored(String.format("%-" + (TOTAL_WIDTH - 22) + "s", className), BRIGHT_PURPLE))
            .append(colored("│", BRIGHT_BLUE)).append("\n");
        output.append(colored("│", BRIGHT_BLUE))
            .append(colored(String.format(" 🔗 Base URLs: %-" + (TOTAL_WIDTH - 16) + "s",
                baseUrls.isEmpty() ? "/" : String.join(", ", baseUrls)), CYAN))
            .append(colored("│", BRIGHT_BLUE)).append("\n");
        output.append(colored("├" + LINE + "┤", BRIGHT_BLUE)).append("\n");

        // 表头
        output.append(colored("│", BRIGHT_BLUE))
            .append(colored(String.format(" %-4s ", "No."), BOLD_WHITE))
            .append(colored("│", BRIGHT_BLUE))
            .append(colored(String.format(" %-50s ", "URL Path"), BOLD_WHITE))
            .append(colored("│", BRIGHT_BLUE))
            .append(colored(String.format(" %-11s ", "HTTP Method"), BOLD_WHITE))
            .append(colored("│", BRIGHT_BLUE))
            .append(colored(String.format(" %-38s ", "Method Name"), BOLD_WHITE))
            .append(colored("│", BRIGHT_BLUE))
            .append(colored(String.format(" %-24s ", "Parameters"), BOLD_WHITE))
            .append(colored("│", BRIGHT_BLUE)).append("\n");

        // 表头分隔线
        String headerSeparator = String.format("├%s┼%s┼%s┼%s┼%s┤",
            "─".repeat(COL_NO),
            "─".repeat(COL_URL),
            "─".repeat(COL_HTTP),
            "─".repeat(COL_METHOD),
            "─".repeat(COL_PARAM));
        output.append(colored(headerSeparator, BRIGHT_BLUE)).append("\n");

        if (routerMethods.isEmpty()) {
            output.append(colored("│", BRIGHT_BLUE))
                .append(colored(String.format(" %-" + (TOTAL_WIDTH - 2) + "s ", "⚠️  No route methods found"), YELLOW))
                .append(colored("│", BRIGHT_BLUE)).append("\n");
        } else {
            for (int i = 0; i < routerMethods.size(); i++) {
                printRouterRow(output, routerMethods.get(i), baseUrls, i + 1);
            }
        }

        output.append(colored("└" + LINE + "┘", BRIGHT_BLUE)).append("\n");
    }

    private static void printRouterRow(StringBuilder output, RouterMethod routerMethod,
                                       List<String> baseUrls, int index) {
        JMethod jMethod = routerMethod.getJMethod();
        String methodName = jMethod.getName();
        String httpMethod = routerMethod.getHttpMethod().toString();
        List<String> urls = routerMethod.getUrls();
        List<String> fullUrls = StringUtils.buildFullUrls(baseUrls, urls);

        List<String> paramLines = formatParametersForConsole(jMethod);

        int maxRows = Math.max(fullUrls.size(), paramLines.size());

        for (int i = 0; i < maxRows; i++) {
            output.append(colored("│", BRIGHT_BLUE));

            // No. 列 (6字符宽度 = " %-4s ")
            if (i == 0) {
                output.append(colored(String.format(" %-4d ", index), BOLD_WHITE));
            } else {
                output.append("      ");
            }
            output.append(colored("│", BRIGHT_BLUE));

            // URL Path 列 (52字符宽度 = " %-50s ")
            if (i < fullUrls.size()) {
                String url = fullUrls.get(i);
                output.append(colored(String.format(" %-50s ", truncate(url, 50)), GREEN));
            } else {
                output.append(" ".repeat(52));
            }
            output.append(colored("│", BRIGHT_BLUE));

            // HTTP Method 列 (13字符宽度 = " " + 8字符色块 + 4空格)
            if (i == 0) {
                String paddedMethod = httpMethodColor(httpMethod);
                output.append(" ").append(paddedMethod).append("    ");  // 1 + 8 + 4 = 13
            } else {
                output.append(" ".repeat(13));
            }
            output.append(colored("│", BRIGHT_BLUE));

            // Method Name 列 (40字符宽度 = " %-38s ")
            if (i == 0) {
                output.append(colored(String.format(" %-38s ", truncate(methodName, 38)), PURPLE));
            } else {
                output.append(" ".repeat(40));
            }
            output.append(colored("│", BRIGHT_BLUE));

            // Parameters 列 (26字符宽度 = " %-24s ")
            if (i < paramLines.size()) {
                String paramLine = paramLines.get(i);
                output.append(colored(String.format(" %-24s ", truncate(paramLine, 24)), CYAN));
            } else {
                output.append(" ".repeat(26));
            }
            output.append(colored("│", BRIGHT_BLUE));

            output.append("\n");
        }
    }

    /**
     * HttpMethod 请求类型颜色和背景
     * @param method 方法请求类型
     * @return 颜色
     */
    private static String httpMethodColor(String method) {
        // 确保每个方法名都是 8 个可见字符(通过空格填充)
        return switch (method) {
            case "GET"    -> BG_GREEN + BOLD_WHITE + "  GET   " + RESET;  // 2空格 + 3字符 + 3空格
            case "POST"   -> BG_BLUE + BOLD_WHITE + "  POST  " + RESET;   // 2空格 + 4字符 + 2空格
            case "PUT"    -> "\033[48;5;208m" + BOLD_WHITE + "  PUT   " + RESET; // 2空格 + 3字符 + 3空格
            case "DELETE" -> BG_RED + BOLD_WHITE + " DELETE " + RESET;    // 1空格 + 6字符 + 1空格
            case "PATCH"  -> BG_PURPLE + BOLD_WHITE + " PATCH  " + RESET; // 1空格 + 5字符 + 2空格
            default       -> "\033[48;5;240m" + BOLD_WHITE + "  ANY   " + RESET; // 2空格 + 3字符 + 3空格
        };
    }

    private static List<String> formatParametersForConsole(JMethod jMethod) {
        List<Type> paramTypes = jMethod.getParamTypes();
        if (paramTypes.isEmpty()) {
            return List.of("-");
        }

        return IntStream.range(0, paramTypes.size())
            .mapToObj(i -> {
                Type type = paramTypes.get(i);
                String typeName = getSimpleTypeName(String.valueOf(type));

                // 获取该参数的所有注解
                List<String> annotations = jMethod.getParamAnnotations(i).stream()
                    .map(Annotation::getType)
                    .map(RouterAnalysisResultToConsole::getSimpleTypeName)
                    .toList();

                if (annotations.isEmpty()) {
                    return typeName;
                } else {
                    String annotationsStr = annotations.stream()
                        .map(a -> "@" + a)
                        .collect(Collectors.joining(" "));
                    return annotationsStr + " " + typeName;
                }
            })
            .toList();
    }

    /**
     * 获取简单类名 java.util.ArrayList -> ArrayList
     * TODO: 优化 Java Doc
     * @param fullTypeName 完整类名
     * @return 简单类名
     */
    public static String getSimpleTypeName(String fullTypeName) {
        int lastDot = fullTypeName.lastIndexOf('.');
        return lastDot > 0 ? fullTypeName.substring(lastDot + 1) : fullTypeName;
    }

    /**
     * 截断字符串以适应最大长度，超出部分用省略号表示
     * @param str 字符串
     * @param maxLength 最大长度
     * @return 截断后的字符串
     */
    private static String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
}