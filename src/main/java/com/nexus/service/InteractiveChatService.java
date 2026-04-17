package com.nexus.service;

import com.nexus.domain.ChatMessage;
import com.nexus.domain.LlmModel;
import com.nexus.domain.TaskType;
import com.nexus.util.TerminalUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Interactive multi-turn coding session.
 *
 * Enters a conversational loop in the terminal where the user can:
 *  - Chat with the routed LLM across multiple turns
 *  - Inject local files into the context with /read
 *  - Save generated code blocks to disk with /write
 *  - List directory contents with /ls
 *  - View accumulated spending with /cost
 *  - Clear conversation history with /clear (keeps session metrics)
 *  - Exit and rate the session with /exit
 *
 * All token + cost data is accumulated and returned as a ChatResult
 * so the caller (RoutingMenu) can close the AgentSession record correctly.
 *
 * File access is governed by profile policy settings:
 *   policy.allow_file_write  — guards /write
 *   context.max_injection_tokens — truncates /read payloads
 */
public class InteractiveChatService {

    // ANSI codes for chat UX (lean on TerminalUtils where possible)
    private static final String CYAN  = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String RESET = TerminalUtils.RESET;
    private static final String GRAY  = TerminalUtils.GRAY;
    private static final String AMBER = TerminalUtils.AMBER;
    private static final String BOLD  = TerminalUtils.BOLD;

    private static final Pattern CODE_BLOCK = Pattern.compile("```[\\w]*\\n([\\s\\S]*?)```", Pattern.DOTALL);

    private final LlmCallService llmCallService;
    private final ProfileService profileService;
    private final Scanner scanner;

    public InteractiveChatService(LlmCallService llmCallService,
                                  ProfileService profileService,
                                  Scanner scanner) {
        this.llmCallService  = llmCallService;
        this.profileService  = profileService;
        this.scanner         = scanner;
    }

    /** Returned to RoutingMenu once the user closes the session. */
    public record ChatResult(int totalInputTokens, int totalOutputTokens,
                             double totalCost, int turns, double qualityScore,
                             String recap, String fullTranscript) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Public entry point
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Starts the interactive chat loop.
     *
     * @param userId    authenticated user's DB id
     * @param sessionId the already-created AgentSession id (for display)
     * @param model     routed LLM model
     * @param taskType  task context (used in system prompt)
     * @return accumulated metrics for session closing
     */
    public ChatResult run(int userId, int sessionId, LlmModel model, TaskType taskType) {
        return run(userId, sessionId, model, taskType, null);
    }

    /**
     * Starts the interactive chat loop with optional continuation context.
     * The context is injected as a system message before the first user turn.
     */
    public ChatResult run(int userId, int sessionId, LlmModel model, TaskType taskType, String resumeContext) {
        // Read profile settings
        String scope               = profileService.currentWorkspaceScope();
        boolean allowFileWrite     = profileService.isActionAllowed(userId, scope, "policy.allow_file_write");
        int maxInjectionTokens     = profileService.getIntSetting(userId, scope, "context.max_injection_tokens", 900);

        // Conversation state
        List<ChatMessage> history  = new ArrayList<>();
        history.add(ChatMessage.system(buildSystemPrompt(taskType, scope)));
        if (resumeContext != null && !resumeContext.trim().isEmpty()) {
            history.add(ChatMessage.system("Continuation context from prior session:\n" + resumeContext.trim()));
        }

        // Accumulation
        int    totalInputTokens  = 0;
        int    totalOutputTokens = 0;
        double totalCost         = 0.0;
        int    turns             = 0;

        // State between turns
        String lastCodeBlock     = null;
        String pendingFileCtx    = null;

        printChatHeader(sessionId, model, taskType, allowFileWrite);
        if (resumeContext != null && !resumeContext.trim().isEmpty()) {
            TerminalUtils.printInfo("Loaded continuation context from vault for this session.");
        }

        // ── Main chat loop ────────────────────────────────────────────────────
        while (true) {
            printChatPrompt(sessionId);
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) continue;

            // ── Command handling ──────────────────────────────────────────────
            if (input.startsWith("/")) {
                String[] parts = input.split("\\s+", 2);
                String cmd     = parts[0].toLowerCase();
                String arg     = parts.length > 1 ? parts[1].trim() : "";

                switch (cmd) {
                    case "/exit", "/quit", "/close" -> {
                        return closePrompt(totalInputTokens, totalOutputTokens, totalCost, turns, history);
                    }
                    case "/help"  -> printHelp(allowFileWrite);
                    case "/cost"  -> printCostSummary(totalInputTokens, totalOutputTokens, totalCost, turns, model);
                    case "/read"  -> pendingFileCtx = handleRead(arg, maxInjectionTokens);
                    case "/write" -> {
                        if (lastCodeBlock != null) handleWrite(lastCodeBlock, allowFileWrite);
                        else                       TerminalUtils.printInfo("No code block detected in last response.");
                    }
                    case "/ls"    -> handleLs(arg.isEmpty() ? "." : arg);
                    case "/clear" -> {
                        history.clear();
                        history.add(ChatMessage.system(buildSystemPrompt(taskType, scope)));
                        pendingFileCtx = null;
                        lastCodeBlock  = null;
                        TerminalUtils.printSuccess("Conversation history cleared. Cost metrics preserved.");
                    }
                    default -> TerminalUtils.printError("Unknown command '" + cmd + "'. Type /help.");
                }
                continue;
            }

            // ── Compose user message (inject file context if queued) ──────────
            String userContent = pendingFileCtx != null
                ? pendingFileCtx + "\n\n---\n\n" + input
                : input;
            pendingFileCtx = null;
            history.add(ChatMessage.user(userContent));

            // ── LLM call ──────────────────────────────────────────────────────
            try {
                System.out.print("  " + GRAY + model.getName() + " is thinking..." + RESET);
                LlmCallService.LlmCallResult resp = llmCallService.executeConversation(userId, model, history);

                // Flush the "thinking" line
                System.out.print("\r" + " ".repeat(60) + "\r");

                history.add(ChatMessage.assistant(resp.content()));
                totalInputTokens  += resp.inputTokens();
                totalOutputTokens += resp.outputTokens();
                totalCost         += resp.costUsd();
                turns++;

                // Extract the last code block for /write
                lastCodeBlock = extractLastCodeBlock(resp.content());

                // Render response
                printAssistantResponse(resp, model);
                printTurnStats(resp, totalCost, turns);

                if (lastCodeBlock != null && allowFileWrite) {
                    TerminalUtils.printInfo("Code block detected — type /write to save it to a file.");
                }

            } catch (Exception e) {
                // Roll back the user message so history stays clean
                if (!history.isEmpty() && history.get(history.size() - 1).isUser()) {
                    history.remove(history.size() - 1);
                }
                System.out.print("\r" + " ".repeat(60) + "\r");
                TerminalUtils.printError("Call failed: " + e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // /exit — collect quality rating and return ChatResult
    // ─────────────────────────────────────────────────────────────────────────

    private ChatResult closePrompt(int totalIn, int totalOut, double totalCost, int turns, List<ChatMessage> history) {
        System.out.println();
        TerminalUtils.printSeparator("SESSION CLOSING");
        printCostSummary(totalIn, totalOut, totalCost, turns, null);

        System.out.println();
        System.out.print("  Rate this session quality (0.0-1.0, Enter=0.75): ");
        String raw = scanner.nextLine().trim();
        double q = 0.75;
        if (!raw.isEmpty()) {
            try {
                double parsed = Double.parseDouble(raw);
                if (parsed >= 0.0 && parsed <= 1.0) q = parsed;
                else TerminalUtils.printWarn("Out of range — using default 0.75.");
            } catch (NumberFormatException ignored) {
                TerminalUtils.printWarn("Invalid — using default 0.75.");
            }
        }
        return new ChatResult(totalIn, totalOut, totalCost, turns, q,
            buildRecap(history), buildFullTranscript(history));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // /read
    // ─────────────────────────────────────────────────────────────────────────

    private String handleRead(String pathArg, int maxTokens) {
        if (pathArg.isEmpty()) {
            System.out.print("  File path to inject: ");
            pathArg = scanner.nextLine().trim();
        }
        if (pathArg.isEmpty()) { TerminalUtils.printError("No path provided."); return null; }

        try {
            Path p = Paths.get(pathArg);
            if (!Files.exists(p))      { TerminalUtils.printError("File not found: " + pathArg); return null; }
            if (!Files.isRegularFile(p)){ TerminalUtils.printError("Not a regular file: " + pathArg); return null; }

            String content = Files.readString(p);
            int estimatedTokens = content.length() / 4;

            if (estimatedTokens > maxTokens) {
                TerminalUtils.printWarn(String.format(
                    "File is ~%d tokens, limit is %d. Truncating to fit.", estimatedTokens, maxTokens));
                content = content.substring(0, maxTokens * 4);
                content += "\n\n[... TRUNCATED — increase context.max_injection_tokens in Profile to inject more]";
            }

            String ctx = "=== FILE: " + p.toAbsolutePath() + " ===\n" + content + "\n=== END FILE ===";
            TerminalUtils.printSuccess(String.format(
                "File injected: %s (~%d tokens). It will be included with your next message.",
                p.getFileName(), Math.min(estimatedTokens, maxTokens)));
            return ctx;

        } catch (InvalidPathException e) {
            TerminalUtils.printError("Invalid path: " + e.getMessage());
            return null;
        } catch (IOException e) {
            TerminalUtils.printError("Could not read file: " + e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // /write
    // ─────────────────────────────────────────────────────────────────────────

    private void handleWrite(String codeBlock, boolean allowFileWrite) {
        if (!allowFileWrite) {
            TerminalUtils.printError("File write is disabled by your profile policy (policy.allow_file_write=false).");
            TerminalUtils.printInfo("Change it in Account + Profile → Set Profile Setting.");
            return;
        }

        System.out.println();
        System.out.println("  " + GRAY + "Code block preview (first 5 lines):" + RESET);
        String[] lines = codeBlock.split("\n");
        for (int i = 0; i < Math.min(5, lines.length); i++) {
            System.out.println("  " + CYAN + lines[i] + RESET);
        }
        if (lines.length > 5) System.out.println("  " + GRAY + "  ... (" + (lines.length - 5) + " more lines)" + RESET);
        System.out.println();

        System.out.print("  Save to filename (e.g. Main.java): ");
        String filename = scanner.nextLine().trim();
        if (filename.isEmpty()) { TerminalUtils.printInfo("Cancelled."); return; }

        try {
            Path target = Paths.get(filename);
            Files.writeString(target, codeBlock);
            TerminalUtils.printSuccess("Saved to: " + target.toAbsolutePath());
        } catch (IOException e) {
            TerminalUtils.printError("Write failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // /ls
    // ─────────────────────────────────────────────────────────────────────────

    private void handleLs(String pathArg) {
        try {
            Path dir = Paths.get(pathArg);
            if (!Files.isDirectory(dir)) { TerminalUtils.printError("Not a directory: " + pathArg); return; }

            System.out.println();
            System.out.println("  " + AMBER + dir.toAbsolutePath() + RESET);
            Files.list(dir)
                .sorted()
                .limit(40)
                .forEach(p -> {
                    String type = Files.isDirectory(p) ? CYAN + "[DIR] " + RESET : "      ";
                    System.out.println("  " + type + p.getFileName());
                });
            System.out.println();
        } catch (IOException | InvalidPathException e) {
            TerminalUtils.printError("Could not list directory: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Display helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void printChatHeader(int sessionId, LlmModel model, TaskType taskType, boolean allowWrite) {
        System.out.println();
        TerminalUtils.printSeparator("CODING SESSION #" + sessionId + " — " + taskType.name());
        System.out.println("  " + AMBER + "Model   " + RESET + model.getName() + " (" + model.getProvider() + ")");
        System.out.println("  " + AMBER + "Task    " + RESET + taskType.name());
        System.out.println("  " + AMBER + "Files   " + RESET + (allowWrite ? GREEN + "read + write enabled" : GRAY + "read-only") + RESET);
        System.out.println();
        System.out.println("  " + GRAY + "Commands: /exit /read /write /ls /cost /clear /help" + RESET);
        System.out.println("  " + GRAY + "Type your message and press Enter." + RESET);
        System.out.println();
    }

    private void printChatPrompt(int sessionId) {
        System.out.print("  " + CYAN + "session:" + sessionId + RESET + " > ");
    }

    private void printAssistantResponse(LlmCallService.LlmCallResult resp, LlmModel model) {
        System.out.println();
        System.out.println("  " + AMBER + BOLD + model.getName() + RESET + " >");
        System.out.println();

        // Render with light syntax emphasis — code blocks in cyan
        String content = resp.content();
        String[] segments = content.split("(?=```)|(?<=```)");
        boolean inCode = false;
        for (String seg : segments) {
            if (seg.startsWith("```")) inCode = !inCode;
            String color = inCode ? CYAN : RESET;
            // Indent each line for readability
            for (String line : seg.split("\n", -1)) {
                System.out.println("  " + color + line + RESET);
            }
        }
        System.out.println();
    }

    private void printTurnStats(LlmCallService.LlmCallResult resp, double totalCostSoFar, int turns) {
        String mode = resp.simulated() ? GRAY + "[SIMULATED]" + RESET : GREEN + "[REAL]" + RESET;
        System.out.printf("  %s  Tokens: in=%-5d out=%-5d | This turn: $%.7f | Session total: %s$%.6f%s  (%d turn%s)%n",
                mode,
                resp.inputTokens(), resp.outputTokens(), resp.costUsd(),
                AMBER, totalCostSoFar, RESET,
                turns, turns == 1 ? "" : "s");
        System.out.println();
    }

    private void printCostSummary(int totalIn, int totalOut, double totalCost, int turns, LlmModel model) {
        TerminalUtils.printSeparator("COST SUMMARY");
        System.out.printf("  Turns              : %d%n", turns);
        System.out.printf("  Total input tokens : %d%n", totalIn);
        System.out.printf("  Total output tokens: %d%n", totalOut);
        System.out.printf("  Total tokens       : %d%n", totalIn + totalOut);
        System.out.printf("  Total cost         : " + AMBER + "$%.7f" + RESET + "%n", totalCost);
        if (model != null) {
            System.out.printf("  Rate               : $%.5f / 1k tokens%n", model.getCostPer1kTokens());
        }
    }

    private void printHelp(boolean allowWrite) {
        System.out.println();
        System.out.println("  " + AMBER + "/exit             " + RESET + "Close session, rate quality, save to history");
        System.out.println("  " + AMBER + "/read [filepath]  " + RESET + "Inject a file into the next message");
        if (allowWrite) {
            System.out.println("  " + AMBER + "/write            " + RESET + "Save last detected code block to a file");
        } else {
            System.out.println("  " + GRAY  + "/write            " + RESET + GRAY + "Disabled (policy.allow_file_write=false)" + RESET);
        }
        System.out.println("  " + AMBER + "/ls [dir]         " + RESET + "List files in a directory (default: current)");
        System.out.println("  " + AMBER + "/cost             " + RESET + "Show accumulated token usage and spending");
        System.out.println("  " + AMBER + "/clear            " + RESET + "Clear conversation history (keeps cost metrics)");
        System.out.println("  " + AMBER + "/help             " + RESET + "Show this help");
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────────────────────────────────

    private String buildSystemPrompt(TaskType taskType, String scope) {
        return "You are Nexus, an expert coding assistant. "
            + "Task context: " + taskType.name() + ". "
            + "Workspace: " + System.getProperty("user.dir") + ". "
            + "Be concise and accurate. Provide complete, working code. "
            + "When generating code, wrap it in a single fenced code block so the user can save it easily. "
            + "Scope: " + scope + ".";
    }

    /**
     * Extracts the last code block from a response.
     * Used to populate the /write buffer.
     */
    private String extractLastCodeBlock(String content) {
        if (content == null) return null;
        Matcher m = CODE_BLOCK.matcher(content);
        String last = null;
        while (m.find()) {
            last = m.group(1).trim();
        }
        return last;
    }

    private String buildRecap(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) return "";

        List<ChatMessage> visible = history.stream()
            .filter(m -> m.isUser() || m.isAssistant())
            .collect(Collectors.toList());
        if (visible.isEmpty()) return "";

        int start = Math.max(0, visible.size() - 8);
        StringBuilder recap = new StringBuilder();
        for (int i = start; i < visible.size(); i++) {
            ChatMessage m = visible.get(i);
            String role = m.isUser() ? "User" : "Assistant";
            String text = m.content() == null ? "" : m.content().trim().replace("\n", " ");
            if (text.length() > 180) text = text.substring(0, 180) + "...";
            recap.append(role).append(": ").append(text).append("\n");
        }
        return recap.toString().trim();
    }

    private String buildFullTranscript(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) return "";

        List<ChatMessage> visible = history.stream()
            .filter(m -> m.isUser() || m.isAssistant())
            .collect(Collectors.toList());
        if (visible.isEmpty()) return "";

        StringBuilder transcript = new StringBuilder();
        for (int i = 0; i < visible.size(); i++) {
            ChatMessage m = visible.get(i);
            String role = m.isUser() ? "User" : "Assistant";
            transcript.append("[").append(i + 1).append("] ").append(role).append(":\n");
            transcript.append(m.content() == null ? "" : m.content().trim()).append("\n\n");
        }
        return transcript.toString().trim();
    }
}
