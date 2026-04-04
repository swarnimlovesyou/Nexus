package com.nexus.util;

public class TerminalUtils {
    // ANSI Colors
    public static final String RESET = "\u001B[0m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";
    public static final String BOLD = "\u001B[1m";
    public static final String ORANGE = "\u001B[38;5;208m"; // Close to Claude brand color

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static void printHeader(String title) {
        String border = "=".repeat(title.length() + 10);
        System.out.println(ORANGE + BOLD + border + RESET);
        System.out.println(ORANGE + BOLD + "     " + title.toUpperCase() + "     " + RESET);
        System.out.println(ORANGE + BOLD + border + RESET);
    }

    public static void printClaudeStyleBox(String title, String content) {
        String[] lines = content.split("\n");
        int width = Math.max(title.length(), 60);
        for (String line : lines) {
            width = Math.max(width, line.length());
        }
        width += 4;

        String horizontal = "─".repeat(width);
        System.out.println(ORANGE + "╭─ " + BOLD + title + RESET + ORANGE + " " + "─".repeat(width - title.length() - 3) + "╮" + RESET);
        for (String line : lines) {
            int padding = width - line.length();
            System.out.println(ORANGE + "│ " + RESET + line + " ".repeat(padding - 1) + ORANGE + "│" + RESET);
        }
        System.out.println(ORANGE + "╰" + horizontal + "╯" + RESET);
    }

    public static void printBanner() {
        System.out.println(ORANGE + BOLD);
        System.out.println("    _   _                      ");
        System.out.println("   | \\ | |                     ");
        System.out.println("   |  \\| | ___  __  _  _  ___  ");
        System.out.println("   | . ` |/ _ \\ \\ \\/ /| || __| ");
        System.out.println("   | |\\  |  __/  >  < | ||__ \\ ");
        System.out.println("   \\_| \\_/\\___| /_/\\_\\|__||___/ ");
        System.out.println("                               ");
        System.out.println("   Nexus Autopilot v1.0.0      ");
        System.out.println(RESET);
    }

    public static void printSuccess(String message) {
        System.out.println(GREEN + "✔ " + message + RESET);
    }

    public static void printError(String message) {
        System.out.println(RED + "✖ " + message + RESET);
    }

    public static void printInfo(String message) {
        System.out.println(CYAN + "ℹ " + message + RESET);
    }

    public static void printStep(String step) {
        System.out.println(YELLOW + "→ " + BOLD + step + RESET);
    }
}
