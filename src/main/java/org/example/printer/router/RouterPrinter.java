package org.example.printer.router;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.printer.router.console.RouterAnalysisResultToConsole;
import org.example.printer.router.file.RouterAnalysisResultToHtml;
import org.example.spring.analysis.RouterAnalysis;
import org.example.spring.router.ControllerClass;
import pascal.taie.World;

import java.util.List;

import static org.example.printer.router.console.ConsoleColors.*;

/**
 * Pretty printer for router analysis results
 */
public class RouterPrinter {

    private static final Logger logger = LogManager.getLogger(RouterPrinter.class);

    public static void printRouterInfo() {
        List<ControllerClass> controllerClasses = World.get().getResult(RouterAnalysis.ID);

        if (controllerClasses == null || controllerClasses.isEmpty()) {
            logger.warn(colored("Router Analysis did not find any routing information...", YELLOW));
            return;
        }

        // 控制台彩色输出
        RouterAnalysisResultToConsole.printToConsole(controllerClasses);

        // HTML文件输出
        RouterAnalysisResultToHtml.generateAndLogReport(controllerClasses);
    }
}
