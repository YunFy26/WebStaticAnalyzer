package org.example.printer.router.console;

/**
 * ANSI color codes for console output
 */
public class ConsoleColors {
    // Reset
    public static final String RESET = "\033[0m";

    // Regular Colors
    public static final String BLACK = "\033[0;30m";
    public static final String RED = "\033[0;31m";
    public static final String GREEN = "\033[0;32m";
    public static final String YELLOW = "\033[0;33m";
    public static final String BLUE = "\033[0;34m";
    public static final String PURPLE = "\033[0;35m";
    public static final String CYAN = "\033[0;36m";
    public static final String WHITE = "\033[0;37m";

    // Bold
    public static final String BOLD = "\033[1m";
    public static final String BOLD_RED = "\033[1;31m";
    public static final String BOLD_GREEN = "\033[1;32m";
    public static final String BOLD_YELLOW = "\033[1;33m";
    public static final String BOLD_BLUE = "\033[1;34m";
    public static final String BOLD_PURPLE = "\033[1;35m";
    public static final String BOLD_CYAN = "\033[1;36m";
    public static final String BOLD_WHITE = "\033[1;37m";

    // Background
    public static final String BG_RED = "\033[41m";
    public static final String BG_GREEN = "\033[42m";
    public static final String BG_YELLOW = "\033[43m";
    public static final String BG_BLUE = "\033[44m";
    public static final String BG_PURPLE = "\033[45m";
    public static final String BG_CYAN = "\033[46m";

    // High Intensity
    public static final String BRIGHT_RED = "\033[0;91m";
    public static final String BRIGHT_GREEN = "\033[0;92m";
    public static final String BRIGHT_YELLOW = "\033[0;93m";
    public static final String BRIGHT_BLUE = "\033[0;94m";
    public static final String BRIGHT_PURPLE = "\033[0;95m";
    public static final String BRIGHT_CYAN = "\033[0;96m";

    public static String colored(String text, String color) {
        return color + text + RESET;
    }

    public static String httpMethodColor(String method) {
        return switch (method) {
            case "GET" -> BG_GREEN + BOLD_WHITE + " " + method + " " + RESET;
            case "POST" -> BG_BLUE + BOLD_WHITE + " " + method + " " + RESET;
            case "PUT" -> "\033[48;5;208m" + BOLD_WHITE + " " + method + " " + RESET; // Orange background
            case "DELETE" -> BG_RED + BOLD_WHITE + " " + method + " " + RESET;
            case "PATCH" -> BG_PURPLE + BOLD_WHITE + " " + method + " " + RESET;
            default -> "\033[48;5;240m" + BOLD_WHITE + " " + method + " " + RESET; // Gray background
        };
    }
}
