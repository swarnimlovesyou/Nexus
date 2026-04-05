package com.nexus.util;



public class TerminalUtils {
    // ANSI Colors
    public static final String RESET   = "\u001B[0m";
    public static final String RED     = "\u001B[31m";
    public static final String GREEN   = "\u001B[32m";
    public static final String YELLOW  = "\u001B[33m";
    public static final String BLUE    = "\u001B[34m";
    public static final String PURPLE  = "\u001B[35m";
    public static final String CYAN    = "\u001B[36m";
    public static final String WHITE   = "\u001B[37m";
    public static final String BOLD    = "\u001B[1m";
    public static final String DIM     = "\u001B[2m";
    public static final String ORANGE  = "\u001B[38;5;208m";
    public static final String AMBER   = "\u001B[38;5;214m";
    public static final String GOLD    = "\u001B[38;5;220m";
    public static final String GRAY    = "\u001B[38;5;245m";
    public static final String BG_DARK = "\u001B[48;5;235m";

    // ─── Screen Control ─────────────────────────────────────────────────────────
    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    // ─── Banner / Branding ──────────────────────────────────────────────────────
    public static void printBanner() {
        System.out.println();
        System.out.println(GOLD + BOLD + "    ███╗   ██╗███████╗██╗  ██╗██╗   ██╗███████╗" + RESET);
        System.out.println(AMBER + BOLD + "    ████╗  ██║██╔════╝╚██╗██╔╝██║   ██║██╔════╝" + RESET);
        System.out.println(ORANGE + BOLD + "    ██╔██╗ ██║█████╗   ╚███╔╝ ██║   ██║███████╗" + RESET);
        System.out.println(ORANGE + BOLD + "    ██║╚██╗██║██╔══╝   ██╔██╗ ██║   ██║╚════██║" + RESET);
        System.out.println(RED    + BOLD + "    ██║ ╚████║███████╗██╔╝ ██╗╚██████╔╝███████║" + RESET);
        System.out.println(DIM    +        "    ╚═╝  ╚═══╝╚══════╝╚═╝  ╚═╝ ╚═════╝ ╚══════╝" + RESET);
        System.out.println();
        System.out.println(GRAY + "      LLM Autopilot  ·  Contextd Memory  ·  Intelligent Routing" + RESET);
        System.out.println(GRAY + "      v2.0.0  ·  local-first  ·  your data stays yours" + RESET);
        System.out.println();
    }

    public static void printHelp() {
        printBanner();
        System.out.println(BOLD + "USAGE" + RESET);
        System.out.println("  nexus <command>");
        System.out.println();
        System.out.println(BOLD + "COMMANDS" + RESET);
        System.out.printf("  %-12s  %s%n", AMBER + "start" + RESET, "Launch the interactive Nexus autopilot CLI");
        System.out.printf("  %-12s  %s%n", AMBER + "session" + RESET, "Manage DB-backed coding sessions from command mode");
        System.out.printf("  %-12s  %s%n", AMBER + "finance" + RESET, "Generate spend analysis reports from command mode");
        System.out.println();
        System.out.println(BOLD + "EXAMPLES" + RESET);
        System.out.println("  " + GRAY + "nexus start" + RESET);
        System.out.println("  " + GRAY + "nexus session list --user admin" + RESET);
        System.out.println("  " + GRAY + "nexus session start --user admin --task CODE_GENERATION" + RESET);
        System.out.println("  " + GRAY + "nexus finance report --user admin --range 30d" + RESET);
        System.out.println();
        System.out.println(GRAY + "Docs: https://github.com/swarnimlovesyou/Nexus" + RESET);
        System.out.println();
    }

    // ─── Headers & Separators ───────────────────────────────────────────────────
    public static void printHeader(String title) {
        int padTotal = Math.max(0, 50 - title.length());
        int left = padTotal / 2;
        int right = padTotal - left;
        String bar = "─".repeat(left) + " " + title.toUpperCase() + " " + "─".repeat(right);
        System.out.println();
        System.out.println(AMBER + BOLD + bar + RESET);
    }

    public static void printSeparator(String label) {
        int w = 54;
        if (label == null || label.isEmpty()) {
            System.out.println(GRAY + "─".repeat(w) + RESET);
        } else {
            int fill = Math.max(0, w - label.length() - 4);
            System.out.println(GRAY + "──" + AMBER + " " + label + " " + GRAY + "─".repeat(fill) + RESET);
        }
    }

    // ─── Box Renderer ───────────────────────────────────────────────────────────
    public static void printBox(String title, String content) {
        String[] lines = content.split("\n");
        int width = Math.max(title.length() + 4, 62);
        for (String line : lines) width = Math.max(width, stripAnsi(line).length() + 4);

        System.out.println(AMBER + "╭─ " + BOLD + title + RESET + AMBER + " " + "─".repeat(width - title.length() - 3) + "╮" + RESET);
        for (String line : lines) {
            int lineLen = stripAnsi(line).length();
            int padding = Math.max(0, width - lineLen - 2);
            System.out.println(AMBER + "│ " + RESET + line + " ".repeat(padding) + AMBER + "│" + RESET);
        }
        System.out.println(AMBER + "╰" + "─".repeat(width) + "╯" + RESET);
    }

    // ─── Table Renderer ─────────────────────────────────────────────────────────
    public static void printTable(String[] headers, String[][] rows) {
        int cols = headers.length;
        int[] widths = new int[cols];
        for (int i = 0; i < cols; i++) widths[i] = stripAnsi(headers[i]).length();
        for (String[] row : rows) {
            for (int i = 0; i < Math.min(cols, row.length); i++) {
                widths[i] = Math.max(widths[i], stripAnsi(row[i]).length());
            }
        }

        // Top border
        StringBuilder top = new StringBuilder(AMBER + "┌");
        for (int i = 0; i < cols; i++) top.append("─".repeat(widths[i] + 2)).append(i < cols - 1 ? "┬" : "┐");
        System.out.println(top + RESET);

        // Header row
        StringBuilder hdr = new StringBuilder(AMBER + "│");
        for (int i = 0; i < cols; i++) hdr.append(" ").append(BOLD).append(GOLD).append(padRight(headers[i], widths[i])).append(RESET).append(AMBER).append(" │");
        System.out.println(hdr + RESET);

        // Header separator
        StringBuilder mid = new StringBuilder(AMBER + "├");
        for (int i = 0; i < cols; i++) mid.append("─".repeat(widths[i] + 2)).append(i < cols - 1 ? "┼" : "┤");
        System.out.println(mid + RESET);

        // Data rows
        for (String[] row : rows) {
            StringBuilder rowSb = new StringBuilder(AMBER + "│");
            for (int i = 0; i < cols; i++) {
                String cell = i < row.length ? row[i] : "";
                int cellLen = stripAnsi(cell).length();
                rowSb.append(" ").append(cell).append(" ".repeat(Math.max(0, widths[i] - cellLen))).append(AMBER).append(" │");
            }
            System.out.println(rowSb + RESET);
        }

        // Bottom border
        StringBuilder bot = new StringBuilder(AMBER + "└");
        for (int i = 0; i < cols; i++) bot.append("─".repeat(widths[i] + 2)).append(i < cols - 1 ? "┴" : "┘");
        System.out.println(bot + RESET);
    }

    // ─── Progress Bar ───────────────────────────────────────────────────────────
    public static String progressBar(double value, double max, int width) {
        int filled = max == 0 ? 0 : (int) Math.round((value / max) * width);
        filled = Math.max(0, Math.min(width, filled));
        String bar = AMBER + "█".repeat(filled) + GRAY + "░".repeat(width - filled) + RESET;
        return bar + " " + GOLD + String.format("%.0f%%", max == 0 ? 0 : (value / max) * 100) + RESET;
    }

    public static String confidenceBar(double confidence) {
        String color = confidence >= 0.7 ? GREEN : confidence >= 0.4 ? YELLOW : RED;
        int filled = (int) Math.round(confidence * 10);
        return color + "█".repeat(filled) + GRAY + "░".repeat(10 - filled) + RESET
               + " " + color + String.format("%.0f%%", confidence * 100) + RESET;
    }

    // ─── Status Printers ────────────────────────────────────────────────────────
    public static void printSuccess(String msg) { System.out.println(GREEN + "  ✔ " + RESET + msg); }
    public static void printError(String msg)   { System.out.println(RED   + "  ✖ " + RESET + msg); }
    public static void printInfo(String msg)    { System.out.println(CYAN  + "  ℹ " + RESET + msg); }
    public static void printWarn(String msg)    { System.out.println(YELLOW + "  ⚠ " + RESET + msg); }
    public static void printStep(String msg)    { System.out.println(AMBER + "  → " + BOLD + msg + RESET); }

    // ─── Key-Value ──────────────────────────────────────────────────────────────
    public static void printKeyValue(String key, String value) {
        System.out.printf("  " + GRAY + "%-24s" + RESET + " %s%n", key + " ·············".substring(0, Math.max(0, 24 - key.length())), value);
    }

    // ─── Prompt ─────────────────────────────────────────────────────────────────
    public static void printPrompt(String user) {
        System.out.print(AMBER + "  nexus" + GRAY + "@" + AMBER + user.toLowerCase() + " " + GOLD + "❯ " + RESET);
    }

    // ─── Auth prompt ────────────────────────────────────────────────────────────
    public static void printAuthPrompt() {
        System.out.print(AMBER + "  nexus" + GRAY + "@" + AMBER + "auth " + GOLD + "❯ " + RESET);
    }

    // ─── Spinner (fake async feel) ──────────────────────────────────────────────
    public static void spinner(String label, int durationMs) {
        String[] frames = {"|", "/", "─", "\\"};
        long end = System.currentTimeMillis() + durationMs;
        int i = 0;
        while (System.currentTimeMillis() < end) {
            System.out.print("\r  " + AMBER + frames[i % 4] + RESET + " " + label);
            System.out.flush();
            try { Thread.sleep(80); } catch (InterruptedException ignored) {}
            i++;
        }
        System.out.print("\r  " + GREEN + "✔" + RESET + " " + label + "          \n");
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────
    private static String padRight(String s, int len) {
        int sLen = stripAnsi(s).length();
        return s + " ".repeat(Math.max(0, len - sLen));
    }

    public static String stripAnsi(String s) {
        return s == null ? "" : s.replaceAll("\u001B\\[[;\\d]*m", "");
    }
}
