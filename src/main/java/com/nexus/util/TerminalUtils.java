package com.nexus.util;

import java.nio.charset.Charset;

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
    public static final String GRAY    = "\u001B[38;5;244m";
    public static final String BG_DARK = "\u001B[48;5;235m";

    private static final int DEFAULT_WIDTH = 62;
    private static final boolean UNICODE = supportsUnicode();

    private static final String CH_TL = UNICODE ? "\u250c" : "+";
    private static final String CH_TR = UNICODE ? "\u2510" : "+";
    private static final String CH_BL = UNICODE ? "\u2514" : "+";
    private static final String CH_BR = UNICODE ? "\u2518" : "+";
    private static final String CH_H = UNICODE ? "\u2500" : "-";
    private static final String CH_V = UNICODE ? "\u2502" : "|";
    private static final String CH_T = UNICODE ? "\u252c" : "+";
    private static final String CH_B = UNICODE ? "\u2534" : "+";
    private static final String CH_L = UNICODE ? "\u251c" : "+";
    private static final String CH_R = UNICODE ? "\u2524" : "+";
    private static final String CH_X = UNICODE ? "\u253c" : "+";

    private static final String ICON_OK = UNICODE ? "\u2714" : "OK";
    private static final String ICON_ERR = UNICODE ? "\u2716" : "ERR";
    private static final String ICON_INFO = UNICODE ? "\u2139" : "INFO";
    private static final String ICON_WARN = UNICODE ? "\u26a0" : "WARN";
    private static final String ICON_STEP = UNICODE ? "\u2192" : "->";
    private static final String ICON_PROMPT = UNICODE ? "\u203a" : ">";
    private static final String GLYPH_DOT = UNICODE ? "\u00b7" : "|";

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static void printBanner() {
        System.out.println();
        printWordmark();
        System.out.println(AMBER + BOLD + "  Nexus Ember Grid" + RESET + GRAY + "  " + GLYPH_DOT + "  Contextd Memory  " + GLYPH_DOT + "  Intent-locked Execution" + RESET);
        System.out.println(GRAY + "  v2.1.1  " + GLYPH_DOT + "  local-first  " + GLYPH_DOT + "  profile-driven automation" + RESET);
        System.out.println();
    }

    private static void printWordmark() {
        if (UNICODE) {
            System.out.println(GOLD + BOLD + "    ███╗   ██╗███████╗██╗  ██╗██╗   ██╗███████╗" + RESET);
            System.out.println(AMBER + BOLD + "    ████╗  ██║██╔════╝╚██╗██╔╝██║   ██║██╔════╝" + RESET);
            System.out.println(ORANGE + BOLD + "    ██╔██╗ ██║█████╗   ╚███╔╝ ██║   ██║███████╗" + RESET);
            System.out.println(ORANGE + BOLD + "    ██║╚██╗██║██╔══╝   ██╔██╗ ██║   ██║╚════██║" + RESET);
            System.out.println(ORANGE + BOLD + "    ██║ ╚████║███████╗██╔╝ ██╗╚██████╔╝███████║" + RESET);
            System.out.println(DIM + "    ╚═╝  ╚═══╝╚══════╝╚═╝  ╚═╝ ╚═════╝ ╚══════╝" + RESET);
        } else {
            System.out.println(GOLD + BOLD + "    _   _ ________   ___   _ _   _ ____" + RESET);
            System.out.println(AMBER + BOLD + "   | \\ | |  ____\\ \\ / / | | \\ | / __ \\" + RESET);
            System.out.println(ORANGE + BOLD + "   |  \\| | |__   \\ V /  | |  \\| | |  | |" + RESET);
            System.out.println(ORANGE + BOLD + "   | . ` |  __|   > <   | | . ` | |  | |" + RESET);
            System.out.println(ORANGE + BOLD + "   | |\\  | |____ / . \\  | | |\\  | |__| |" + RESET);
            System.out.println(DIM + "   |_| \\_|______/_/ \\_\\ |_|_| \\_|\\____/" + RESET);
        }
        printSeparator("NEXUS");
    }

    public static void printHelp() {
        printBanner();
        printSeparator("QUICK START");
        System.out.println("  nexus <command> [options]");
        System.out.println();
        printSeparator("COMMAND SECTIONS");
        String[] headers = {"Section", "Primary Commands", "Purpose"};
        String[][] rows = {
            {"Operate", "call, codegen, recipe, tool", "Run generation and tool workflows"},
            {"Memory", "memory, profile", "Persist context and behavior settings"},
            {"Safety", "policy simulate, provider check", "Preview guardrails before execution"},
            {"Reliability", "smoke run, onboard", "Validate end-to-end readiness"},
            {"Interactive", "start", "Open sectioned dashboard UI"}
        };
        printTable(headers, rows);
        System.out.println();
        printSeparator("EXAMPLES");
        System.out.println("  " + GRAY + "nexus start" + RESET);
        System.out.println("  " + GRAY + "nexus onboard --user admin --provider GROQ --mode balanced" + RESET);
        System.out.println("  " + GRAY + "nexus profile wizard --user admin --mode balanced" + RESET);
        System.out.println("  " + GRAY + "nexus policy simulate --user admin --command \"codegen run --output src/Foo.java\"" + RESET);
        System.out.println();
        System.out.println(GRAY + "Docs: https://github.com/swarnimlovesyou/Nexus" + RESET);
        System.out.println();
    }

    public static void printTopology() {
        System.out.println();
        System.out.println(BOLD + "  SYSTEM TOPOLOGY OVERVIEW" + RESET);
        System.out.println(GRAY + "  " + repeat(CH_H, 56) + RESET);
        System.out.println("      [User Request]");
        System.out.println("            |");
        System.out.println("            v");
        System.out.println("      +---------------+        +---------------+");
        System.out.println("      | NEXUS ROUTER  | <----> | CONTEXTD DB   |");
        System.out.println("      +---------------+        +---------------+");
        System.out.println("            |");
        System.out.println("      +-----+-----+-----+");
        System.out.println("      |   GPT   | CLAUDE | GEMINI |");
        System.out.println(GRAY + "  " + repeat(CH_H, 56) + RESET);
        System.out.println();
    }

    public static void printHeader(String title) {
        System.out.println();
        String upper = title == null ? "" : title.toUpperCase();
        int padTotal = Math.max(0, DEFAULT_WIDTH - upper.length() - 2);
        int left = padTotal / 2;
        int right = padTotal - left;
        System.out.println(AMBER + BOLD + repeat(CH_H, left) + " " + upper + " " + repeat(CH_H, right) + RESET);
    }

    public static void printSeparator(String label) {
        if (label == null || label.isBlank()) {
            System.out.println(GRAY + repeat("-", DEFAULT_WIDTH) + RESET);
            return;
        }
        String clean = " " + label + " ";
        int right = Math.max(0, DEFAULT_WIDTH - clean.length() - 2);
        System.out.println(GRAY + repeat(CH_H, 2) + AMBER + clean + GRAY + repeat(CH_H, right) + RESET);
    }

    public static void printBox(String title, String content) {
        String safeTitle = title == null ? "" : title;
        String safeContent = content == null ? "" : content;
        String[] lines = safeContent.split("\\n", -1);

        int width = Math.max(40, safeTitle.length() + 4);
        for (String line : lines) {
            width = Math.max(width, stripAnsi(line).length() + 2);
        }

        String top = CH_TL + CH_H + " " + safeTitle + " " + repeat(CH_H, Math.max(0, width - safeTitle.length() - 4)) + CH_TR;
        System.out.println(AMBER + top + RESET);
        for (String line : lines) {
            int visible = stripAnsi(line).length();
            System.out.println(AMBER + CH_V + RESET + line + repeat(" ", Math.max(0, width - visible)) + AMBER + CH_V + RESET);
        }
        System.out.println(AMBER + CH_BL + repeat(CH_H, width) + CH_BR + RESET);
    }

    public static void printTable(String[] headers, String[][] rows) {
        int cols = headers.length;
        int[] widths = new int[cols];

        for (int i = 0; i < cols; i++) {
            widths[i] = stripAnsi(headers[i]).length();
        }

        for (String[] row : rows) {
            for (int i = 0; i < cols && i < row.length; i++) {
                widths[i] = Math.max(widths[i], stripAnsi(row[i]).length());
            }
        }

        System.out.println(AMBER + buildBorder(CH_TL, CH_T, CH_TR, widths) + RESET);

        StringBuilder headerRow = new StringBuilder();
        headerRow.append(AMBER).append(CH_V).append(RESET);
        for (int i = 0; i < cols; i++) {
            headerRow
                .append(" ")
                .append(BOLD).append(GOLD).append(padRight(headers[i], widths[i])).append(RESET)
                .append(" ")
                .append(AMBER).append(CH_V).append(RESET);
        }
        System.out.println(headerRow);

            System.out.println(AMBER + buildBorder(CH_L, CH_X, CH_R, widths) + RESET);

        for (String[] row : rows) {
            StringBuilder rowLine = new StringBuilder();
            rowLine.append(AMBER).append(CH_V).append(RESET);
            for (int i = 0; i < cols; i++) {
                String cell = i < row.length ? row[i] : "";
                int cellLen = stripAnsi(cell).length();
                rowLine
                    .append(" ")
                    .append(cell)
                    .append(repeat(" ", Math.max(0, widths[i] - cellLen)))
                    .append(" ")
                    .append(AMBER).append(CH_V).append(RESET);
            }
            System.out.println(rowLine);
        }

        System.out.println(AMBER + buildBorder(CH_BL, CH_B, CH_BR, widths) + RESET);
    }

    private static String buildBorder(String left, String join, String right, int[] widths) {
        StringBuilder sb = new StringBuilder(left);
        for (int i = 0; i < widths.length; i++) {
            sb.append(repeat("-", widths[i] + 2));
            sb.append(i < widths.length - 1 ? join : right);
        }
        return sb.toString();
    }

    public static String progressBar(double value, double max, int width) {
        int filled = max == 0 ? 0 : (int) Math.round((value / max) * width);
        filled = Math.max(0, Math.min(width, filled));
        String on = UNICODE ? "\u2588" : "#";
        String off = UNICODE ? "\u2591" : ".";
        String bar = AMBER + repeat(on, filled) + GRAY + repeat(off, width - filled) + RESET;
        double pct = max == 0 ? 0 : (value / max) * 100;
        return bar + " " + GOLD + String.format("%.0f%%", pct) + RESET;
    }

    public static void printHorizontalChart(String title, java.util.Map<String, Double> data) {
        System.out.println(BOLD + "  " + title + RESET);
        int maxKeyLen = data.keySet().stream().mapToInt(String::length).max().orElse(0);
        for (var entry : data.entrySet()) {
            String label = padRight(entry.getKey(), maxKeyLen);
            System.out.print("  " + GRAY + label + RESET + "  ");
            System.out.println(progressBar(entry.getValue(), 1.0, 20));
        }
    }

    public static String confidenceBar(double confidence) {
        String color = confidence >= 0.7 ? GREEN : confidence >= 0.4 ? YELLOW : RED;
        int filled = (int) Math.round(confidence * 10);
        String on = UNICODE ? "\u2588" : "#";
        String off = UNICODE ? "\u2591" : ".";
        return color + repeat(on, Math.max(0, filled)) + GRAY + repeat(off, Math.max(0, 10 - filled)) + RESET
               + " " + color + String.format("%.0f%%", confidence * 100) + RESET;
    }

    public static void printSuccess(String msg) { System.out.println(GREEN + "  " + ICON_OK + " " + RESET + msg); }
    public static void printError(String msg)   { System.out.println(RED + "  " + ICON_ERR + " " + RESET + msg); }
    public static void printInfo(String msg)    { System.out.println(AMBER + "  " + ICON_INFO + " " + RESET + msg); }
    public static void printWarn(String msg)    { System.out.println(YELLOW + "  " + ICON_WARN + " " + RESET + msg); }
    public static void printStep(String msg)    { System.out.println(AMBER + "  " + ICON_STEP + " " + BOLD + msg + RESET); }

    public static void printKeyValue(String key, String value) {
        System.out.printf("  " + GRAY + "%-24s" + RESET + " : %s%n", key, value);
    }

    public static void printPrompt(String user) {
        System.out.print(AMBER + "  nexus" + GRAY + "@" + AMBER + user.toLowerCase() + " " + GOLD + ICON_PROMPT + " " + RESET);
    }

    public static void printAuthPrompt() {
        System.out.print(AMBER + "  nexus" + GRAY + "@" + AMBER + "auth " + GOLD + ICON_PROMPT + " " + RESET);
    }

    public static void spinner(String label, int durationMs) {
        String[] frames = {"|", "/", "-", "\\\\"};
        long end = System.currentTimeMillis() + durationMs;
        int i = 0;
        while (System.currentTimeMillis() < end) {
            System.out.print("\r  " + AMBER + frames[i % 4] + RESET + " " + label);
            System.out.flush();
            try { Thread.sleep(80); } catch (InterruptedException ignored) {}
            i++;
        }
        System.out.print("\r  " + GREEN + ICON_OK + RESET + " " + label + "          \n");
    }

    private static String padRight(String s, int len) {
        int sLen = stripAnsi(s).length();
        return s + repeat(" ", Math.max(0, len - sLen));
    }

    public static String stripAnsi(String s) {
        if (s == null || s.isEmpty()) return "";

        StringBuilder out = new StringBuilder(s.length());
        boolean inEscape = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!inEscape) {
                if (c == 0x1B) {
                    inEscape = true;
                } else {
                    out.append(c);
                }
                continue;
            }

            // ANSI control sequences end with an alphabetic command byte.
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                inEscape = false;
            }
        }

        return out.toString();
    }

    private static String repeat(String value, int count) {
        return count <= 0 ? "" : value.repeat(count);
    }

    private static boolean supportsUnicode() {
        String encoding = Charset.defaultCharset().name().toUpperCase();
        if (encoding.contains("UTF")) return true;

        String wt = System.getenv("WT_SESSION");
        if (wt != null && !wt.isBlank()) return true;

        String term = System.getenv("TERM");
        if (term == null) return false;
        String t = term.toLowerCase();
        return t.contains("xterm") || t.contains("utf");
    }
}
