package org.example.printer.router.file;

import org.example.spring.analysis.router.ControllerClass;
import org.example.spring.analysis.router.RouterMethod;
import org.example.utils.StringUtils;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

import java.io.BufferedWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HTML report generator for router analysis
 */
public class HtmlReportGenerator {

    public static void writeHtmlReport(BufferedWriter writer, List<ControllerClass> controllerClasses)
        throws IOException {
        int totalRoutes = controllerClasses.stream()
            .mapToInt(c -> c.getRouterMethods().size())
            .sum();

        writeHtmlHeader(writer);
        writeReportHeader(writer, controllerClasses.size(), totalRoutes);
        writeTableOfContents(writer, controllerClasses);

        for (int i = 0; i < controllerClasses.size(); i++) {
            writeControllerSection(writer, controllerClasses.get(i), i + 1);
        }

        writeReportFooter(writer, controllerClasses.size(), totalRoutes);
        writeHtmlFooter(writer);
    }

    private static void writeHtmlHeader(BufferedWriter writer) throws IOException {
        writer.write("""
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Spring MVC Router Analysis Report</title>
                    <style>
                """);
        writer.write(getStyles());
        writer.write("""
                    </style>
                </head>
                <body>
                """);
    }

    /**
     * CSS样式
     * TODO：提取CSS代码到单独一个类中
     * @return CSS样式
     */
    private static String getStyles() {
        return """
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }
                
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    min-height: 100vh;
                    padding: 20px;
                    line-height: 1.6;
                }
                
                .container {
                    max-width: 1400px;
                    margin: 0 auto;
                }
                
                .header {
                    background: white;
                    padding: 40px;
                    border-radius: 15px;
                    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
                    margin-bottom: 30px;
                    text-align: center;
                }
                
                .header h1 {
                    color: #333;
                    font-size: 36px;
                    margin-bottom: 15px;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    -webkit-background-clip: text;
                    -webkit-text-fill-color: transparent;
                    background-clip: text;
                }
                
                .timestamp {
                    color: #666;
                    font-size: 14px;
                    margin-bottom: 20px;
                }
                
                .summary {
                    display: flex;
                    justify-content: center;
                    gap: 20px;
                    flex-wrap: wrap;
                    margin-top: 20px;
                }
                
                .badge {
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    color: white;
                    padding: 12px 24px;
                    border-radius: 25px;
                    font-size: 16px;
                    font-weight: 600;
                    box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
                }
                
                .toc {
                    background: white;
                    padding: 30px;
                    border-radius: 15px;
                    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
                    margin-bottom: 30px;
                }
                
                .toc h2 {
                    color: #333;
                    margin-bottom: 20px;
                    font-size: 24px;
                    border-bottom: 3px solid #667eea;
                    padding-bottom: 10px;
                }
                
                .toc ul {
                    list-style: none;
                }
                
                .toc li {
                    padding: 12px 15px;
                    border-bottom: 1px solid #eee;
                    transition: all 0.3s ease;
                }
                
                .toc li:hover {
                    background: #f8f9fa;
                    padding-left: 20px;
                }
                
                .toc li:last-child {
                    border-bottom: none;
                }
                
                .toc a {
                    color: #667eea;
                    text-decoration: none;
                    font-weight: 500;
                    font-size: 16px;
                }
                
                .toc a:hover {
                    color: #764ba2;
                }
                
                .route-count {
                    float: right;
                    background: #e3f2fd;
                    color: #1976d2;
                    padding: 4px 12px;
                    border-radius: 12px;
                    font-size: 12px;
                    font-weight: 600;
                }
                
                .controller-section {
                    background: white;
                    padding: 30px;
                    border-radius: 15px;
                    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
                    margin-bottom: 30px;
                    animation: fadeIn 0.5s ease;
                }
                
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(20px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                
                .controller-header {
                    border-bottom: 4px solid transparent;
                    border-image: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
                    border-image-slice: 1;
                    padding-bottom: 20px;
                    margin-bottom: 25px;
                }
                
                .controller-header h2 {
                    color: #333;
                    font-size: 28px;
                    margin-bottom: 10px;
                }
                
                .controller-full-name {
                    color: #999;
                    font-size: 14px;
                    font-family: 'Courier New', monospace;
                }
                
                .controller-info {
                    background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
                    padding: 20px;
                    border-radius: 10px;
                    margin-bottom: 25px;
                }
                
                .controller-info p {
                    margin: 8px 0;
                    color: #333;
                    font-size: 15px;
                }
                
                .base-url {
                    display: inline-block;
                    background: white;
                    color: #1976d2;
                    padding: 4px 12px;
                    border-radius: 5px;
                    font-family: 'Courier New', monospace;
                    font-size: 13px;
                    margin: 3px;
                    border: 2px solid #e3f2fd;
                }
                
                table {
                    width: 100%;
                    border-collapse: separate;
                    border-spacing: 0;
                    margin-bottom: 25px;
                    overflow: hidden;
                    border-radius: 10px;
                    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
                }
                
                thead {
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                }
                
                th {
                    color: white;
                    padding: 15px 12px;
                    text-align: left;
                    font-weight: 600;
                    font-size: 14px;
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                }
                
                td {
                    padding: 12px;
                    border-bottom: 1px solid #f0f0f0;
                }
                
                tbody tr {
                    transition: all 0.3s ease;
                }
                
                tbody tr:hover {
                    background: #f8f9fa;
                    transform: scale(1.01);
                    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
                }
                
                tbody tr:last-child td {
                    border-bottom: none;
                }
                
                .row-number {
                    text-align: center;
                    color: #999;
                    font-weight: 600;
                }
                
                .http-method {
                    display: inline-block;
                    padding: 6px 14px;
                    border-radius: 6px;
                    font-weight: 700;
                    font-size: 11px;
                    letter-spacing: 0.5px;
                    text-align: center;
                    min-width: 70px;
                }
                
                .http-get { 
                    background: linear-gradient(135deg, #4caf50 0%, #66bb6a 100%);
                    color: white; 
                    box-shadow: 0 2px 8px rgba(76, 175, 80, 0.4);
                }
                
                .http-post { 
                    background: linear-gradient(135deg, #2196f3 0%, #42a5f5 100%);
                    color: white;
                    box-shadow: 0 2px 8px rgba(33, 150, 243, 0.4);
                }
                
                .http-put { 
                    background: linear-gradient(135deg, #ff9800 0%, #ffa726 100%);
                    color: white;
                    box-shadow: 0 2px 8px rgba(255, 152, 0, 0.4);
                }
                
                .http-delete { 
                    background: linear-gradient(135deg, #f44336 0%, #ef5350 100%);
                    color: white;
                    box-shadow: 0 2px 8px rgba(244, 67, 54, 0.4);
                }
                
                .http-patch { 
                    background: linear-gradient(135deg, #9c27b0 0%, #ab47bc 100%);
                    color: white;
                    box-shadow: 0 2px 8px rgba(156, 39, 176, 0.4);
                }
                
                .http-any { 
                    background: linear-gradient(135deg, #607d8b 0%, #78909c 100%);
                    color: white;
                    box-shadow: 0 2px 8px rgba(96, 125, 139, 0.4);
                }
                
                .url-pattern {
                    font-family: 'Courier New', monospace;
                    background: #f5f5f5;
                    padding: 4px 10px;
                    border-radius: 5px;
                    color: #d32f2f;
                    font-size: 13px;
                    font-weight: 500;
                    border-left: 3px solid #d32f2f;
                }
                
                .method-name {
                    font-family: 'Courier New', monospace;
                    color: #1976d2;
                    font-weight: 600;
                    font-size: 14px;
                }
                
                .params-detail {
                    background: linear-gradient(135deg, #e8eaf6 0%, #c5cae9 100%);
                    padding: 20px;
                    border-radius: 10px;
                    margin-top: 15px;
                }
                
                .params-detail h4 {
                    color: #333;
                    margin-bottom: 15px;
                    font-size: 18px;
                }
                
                .param-method {
                    background: white;
                    padding: 15px;
                    border-radius: 8px;
                    margin-bottom: 15px;
                    border-left: 4px solid #667eea;
                }
                
                .param-method h5 {
                    color: #667eea;
                    margin-bottom: 10px;
                    font-size: 16px;
                }
                
                .param-item {
                    padding: 10px;
                    margin: 8px 0;
                    background: #f8f9fa;
                    border-left: 3px solid #764ba2;
                    border-radius: 5px;
                    font-family: 'Courier New', monospace;
                    font-size: 13px;
                }
                
                .param-annotation {
                    color: #9c27b0;
                    font-weight: 700;
                }
                
                .no-routes {
                    text-align: center;
                    color: #999;
                    padding: 40px;
                    font-style: italic;
                    font-size: 16px;
                }
                
                .footer {
                    background: white;
                    padding: 30px;
                    border-radius: 15px;
                    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
                    text-align: center;
                    color: #666;
                }
                
                .footer p {
                    margin: 10px 0;
                    font-size: 16px;
                }
                
                .copyright {
                    font-size: 13px;
                    color: #999;
                    margin-top: 15px;
                }
                
                @media (max-width: 768px) {
                    table {
                        font-size: 12px;
                    }
                    
                    th, td {
                        padding: 8px 6px;
                    }
                    
                    .header h1 {
                        font-size: 24px;
                    }
                }
                """;
    }

    private static void writeReportHeader(BufferedWriter writer, int controllerCount, int routeCount)
        throws IOException {
        writer.write("    <div class=\"container\">\n");
        writer.write("        <div class=\"header\">\n");
        writer.write("            <h1>🎯 Spring MVC Router Analysis Report</h1>\n");
        writer.write(String.format("            <p class=\"timestamp\">📅 Generated at: %s</p>\n",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        writer.write("            <div class=\"summary\">\n");
        writer.write(String.format("                <span class=\"badge\">📦 Controllers: <strong>%d</strong></span>\n",
            controllerCount));
        writer.write(String.format("                <span class=\"badge\">📍 Routes: <strong>%d</strong></span>\n",
            routeCount));
        writer.write("            </div>\n");
        writer.write("        </div>\n\n");
    }

    private static void writeTableOfContents(BufferedWriter writer, List<ControllerClass> controllerClasses)
        throws IOException {
        writer.write("        <div class=\"toc\">\n");
        writer.write("            <h2>📑 Table of Contents</h2>\n");
        writer.write("            <ul>\n");

        for (int i = 0; i < controllerClasses.size(); i++) {
            ControllerClass controller = controllerClasses.get(i);
            String className = controller.getJClass().getName();
            String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
            int routeCount = controller.getRouterMethods().size();

            writer.write("                <li>\n");
            writer.write(String.format("                    <a href=\"#controller-%d\">%d. %s</a>\n",
                i + 1, i + 1, simpleClassName));
            writer.write(String.format("                    <span class=\"route-count\">%d routes</span>\n", routeCount));
            writer.write("                </li>\n");
        }

        writer.write("            </ul>\n");
        writer.write("        </div>\n\n");
    }

    private static void writeControllerSection(BufferedWriter writer, ControllerClass controller, int index)
        throws IOException {
        String className = controller.getJClass().getName();
        String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
        List<String> baseUrls = controller.getBaseUrls();
        List<RouterMethod> routerMethods = controller.getRouterMethods();

        writer.write(String.format("        <div class=\"controller-section\" id=\"controller-%d\">\n", index));
        writer.write("            <div class=\"controller-header\">\n");
        writer.write(String.format("                <h2>📦 %d. %s</h2>\n", index, simpleClassName));
        writer.write(String.format("                <p class=\"controller-full-name\">%s</p>\n", className));
        writer.write("            </div>\n");

        writer.write("            <div class=\"controller-info\">\n");
        writer.write("                <p><strong>🔗 Base URLs:</strong> ");
        if (baseUrls.isEmpty()) {
            writer.write("<span class=\"base-url\">/</span>");
        } else {
            for (String url : baseUrls) {
                writer.write(String.format("<span class=\"base-url\">%s</span> ", escapeHtml(url)));
            }
        }
        writer.write("</p>\n");
        writer.write(String.format("                <p><strong>📍 Total Routes:</strong> %d</p>\n",
            routerMethods.size()));
        writer.write("            </div>\n");

        if (routerMethods.isEmpty()) {
            writer.write("            <div class=\"no-routes\">⚠️ No route methods found in this controller</div>\n");
        } else {
            writeRouteTable(writer, controller, baseUrls, routerMethods);
            writeParameterDetails(writer, routerMethods);
        }

        writer.write("        </div>\n\n");
    }

    private static void writeRouteTable(BufferedWriter writer, ControllerClass controller,
                                        List<String> baseUrls, List<RouterMethod> routerMethods)
        throws IOException {
        writer.write("            <table>\n");
        writer.write("                <thead>\n");
        writer.write("                    <tr>\n");
        writer.write("                        <th style=\"width: 50px;\">No.</th>\n");
        writer.write("                        <th style=\"width: 35%;\">URL Pattern</th>\n");
        writer.write("                        <th style=\"width: 120px;\">HTTP Method</th>\n");
        writer.write("                        <th style=\"width: 25%;\">Method Name</th>\n");
        writer.write("                        <th>Parameters</th>\n");
        writer.write("                    </tr>\n");
        writer.write("                </thead>\n");
        writer.write("                <tbody>\n");

        for (int i = 0; i < routerMethods.size(); i++) {
            RouterMethod routerMethod = routerMethods.get(i);
            JMethod jMethod = routerMethod.getJMethod();
            List<String> fullUrls = StringUtils.buildFullUrls(baseUrls, routerMethod.getUrls());
            String httpMethod = routerMethod.getHttpMethod().toString();
            String params = formatParametersForTable(jMethod);

            for (int j = 0; j < fullUrls.size(); j++) {
                writer.write("                    <tr>\n");
                if (j == 0) {
                    writer.write(String.format("                        <td class=\"row-number\">%d</td>\n", i + 1));
                    writer.write(String.format("                        <td><span class=\"url-pattern\">%s</span></td>\n",
                        escapeHtml(fullUrls.get(j))));
                    writer.write(String.format("                        <td><span class=\"http-method http-%s\">%s</span></td>\n",
                        httpMethod.toLowerCase(), httpMethod));
                    writer.write(String.format("                        <td><span class=\"method-name\">%s</span></td>\n",
                        escapeHtml(jMethod.getName())));
                    writer.write(String.format("                        <td>%s</td>\n", escapeHtml(params)));
                } else {
                    writer.write("                        <td></td>\n");
                    writer.write(String.format("                        <td><span class=\"url-pattern\">%s</span></td>\n",
                        escapeHtml(fullUrls.get(j))));
                    writer.write("                        <td colspan=\"3\"></td>\n");
                }
                writer.write("                    </tr>\n");
            }
        }

        writer.write("                </tbody>\n");
        writer.write("            </table>\n");
    }

    private static void writeParameterDetails(BufferedWriter writer, List<RouterMethod> routerMethods)
        throws IOException {
        boolean hasParams = routerMethods.stream()
            .anyMatch(rm -> !rm.getJMethod().getParamTypes().isEmpty());

        if (!hasParams) {
            return;
        }

        writer.write("            <div class=\"params-detail\">\n");
        writer.write("                <h4>📝 Parameter Details</h4>\n");

        for (int i = 0; i < routerMethods.size(); i++) {
            RouterMethod routerMethod = routerMethods.get(i);
            JMethod jMethod = routerMethod.getJMethod();
            List<Type> paramTypes = jMethod.getParamTypes();

            if (paramTypes.isEmpty()) {
                continue;
            }

            writer.write("                <div class=\"param-method\">\n");
            writer.write(String.format("                    <h5>[%d] %s</h5>\n",
                i + 1, escapeHtml(jMethod.getName())));

            for (int j = 0; j < paramTypes.size(); j++) {
                String paramInfo = formatParameterForHtml(jMethod, j);
                writer.write(String.format("                    <div class=\"param-item\">%s</div>\n", paramInfo));
            }

            writer.write("                </div>\n");
        }

        writer.write("            </div>\n");
    }

    private static void writeReportFooter(BufferedWriter writer, int controllerCount, int routeCount)
        throws IOException {
        writer.write("        <div class=\"footer\">\n");
        writer.write(String.format("            <p>✅ Analysis Complete: %d Controllers, %d Routes</p>\n",
            controllerCount, routeCount));
        writer.write("            <p class=\"copyright\">Generated by Spring MVC Router Analysis Tool</p>\n");
        writer.write("        </div>\n");
        writer.write("    </div>\n");
    }

    private static void writeHtmlFooter(BufferedWriter writer) throws IOException {
        writer.write("</body>\n");
        writer.write("</html>\n");
    }

    private static String formatParametersForTable(JMethod jMethod) {
        List<Type> paramTypes = jMethod.getParamTypes();
        if (paramTypes.isEmpty()) {
            return "-";
        }

        return paramTypes.stream()
            .limit(2)
            .map(type -> {
                String typeName = type.toString();
                int lastDot = typeName.lastIndexOf('.');
                return lastDot > 0 ? typeName.substring(lastDot + 1) : typeName;
            })
            .collect(Collectors.joining(", ")) +
            (paramTypes.size() > 2 ? "..." : "");
    }

    private static String formatParameterForHtml(JMethod jMethod, int paramIndex) {
        Type paramType = jMethod.getParamTypes().get(paramIndex);
        String paramName = String.format("param%d", paramIndex);

        Collection<Annotation> annotations = jMethod.getParamAnnotations(paramIndex);
        String annotationStr = annotations.isEmpty()
            ? ""
            : annotations.stream()
            .map(ann -> {
                String simpleName = ann.getType().substring(ann.getType().lastIndexOf('.') + 1);
                return "<span class=\"param-annotation\">@" + simpleName + "</span>";
            })
            .collect(Collectors.joining(" ")) + " ";

        return String.format("Parameter %d: %s%s %s",
            paramIndex, annotationStr, escapeHtml(paramType.toString()), paramName);
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
