package org.example.printer.router.file;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.spring.router.ControllerClass;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.example.printer.router.console.ConsoleColors.*;

/**
 * HTML report generator for router analysis results
 */
public class RouterAnalysisResultToHtml {

    private static final Logger logger = LogManager.getLogger(RouterAnalysisResultToHtml.class);

    private static final String OUTPUT_DIR = "output/router-analysis";

    /**
     * Generate HTML report and return the file path
     */
    public static String generateReport(List<ControllerClass> controllerClasses) throws IOException {
        Path outputPath = Paths.get(OUTPUT_DIR);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = String.format("router-analysis-report_%s.html", timestamp);
        Path filePath = outputPath.resolve(fileName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
            HtmlReportGenerator.writeHtmlReport(writer, controllerClasses);
        }

        return filePath.toString();
    }

    /**
     * Generate HTML report and log the result
     */
    public static void generateAndLogReport(List<ControllerClass> controllerClasses) {
        try {
            String filePath = generateReport(controllerClasses);
            logger.info(colored("Router analysis report saved to: " + filePath, GREEN));
        } catch (IOException e) {
            logger.error(colored("Failed to generate report file", RED), e);
        }
    }
}
