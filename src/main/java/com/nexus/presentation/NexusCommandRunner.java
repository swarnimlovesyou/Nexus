package com.nexus.presentation;

import java.io.Console;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

import com.nexus.dao.AuditLogDao;
import com.nexus.dao.LlmModelDao;
import com.nexus.dao.OutcomeMemoryDao;
import com.nexus.dao.SuitabilityDao;
import com.nexus.domain.AgentSession;
import com.nexus.domain.ApiKey;
import com.nexus.domain.AuditLog;
import com.nexus.domain.LlmModel;
import com.nexus.domain.Memory;
import com.nexus.domain.MemoryType;
import com.nexus.domain.ModelSuitability;
import com.nexus.domain.OutcomeMemory;
import com.nexus.domain.Provider;
import com.nexus.domain.TaskType;
import com.nexus.domain.User;
import com.nexus.exception.DaoException;
import com.nexus.service.ApiKeyService;
import com.nexus.service.LlmCallService;
import com.nexus.service.ProfileService;
import com.nexus.service.RoutingEngine;
import com.nexus.service.SessionService;
import com.nexus.service.ToolExecutionService;
import com.nexus.service.UserService;
import com.nexus.util.TerminalUtils;

/**
 * Supports command-mode workflows such as:
 *   nexus session start --user admin --task CODE_GENERATION
 *   nexus finance report --user admin --range 30d
 */
public class NexusCommandRunner {
    private final UserService userService;
    private final RoutingEngine routingEngine;
    private final SessionService sessionService;
    private final LlmModelDao modelDao;
    private final SuitabilityDao suitabilityDao;
    private final OutcomeMemoryDao outcomeDao;
    private final AuditLogDao auditLogDao;
    private final ApiKeyService apiKeyService;
    private final LlmCallService llmCallService;
    private final ProfileService profileService;
    private final ToolExecutionService toolExecutionService;
    private final Scanner scanner;
    private String activeTraceId;

    public NexusCommandRunner() {
        this(new Scanner(System.in));
    }

    public NexusCommandRunner(Scanner scanner) {
        this.userService = new UserService();
        this.routingEngine = new RoutingEngine();
        this.sessionService = new SessionService();
        this.modelDao = new LlmModelDao();
        this.suitabilityDao = new SuitabilityDao();
        this.outcomeDao = new OutcomeMemoryDao();
        this.auditLogDao = new AuditLogDao();
        this.apiKeyService = new ApiKeyService();
        this.llmCallService = new LlmCallService(apiKeyService, modelDao);
        this.profileService = new ProfileService();
        this.toolExecutionService = new ToolExecutionService();
        this.scanner = scanner;
        this.activeTraceId = newTraceId();
    }

    public static boolean tryRun(String[] args) {
        if (args == null || args.length == 0) return false;

        String[] effective = args;
        String top = args[0].toLowerCase(Locale.ROOT);
        if ("command".equals(top) || "cmd".equals(top)) {
            effective = Arrays.copyOfRange(args, 1, args.length);
            if (effective.length == 0) {
                NexusCommandRunner runner = new NexusCommandRunner();
                runner.printCommandHelp();
                return true;
            }
            top = effective[0].toLowerCase(Locale.ROOT);
        }

        // Accept dotted command style (e.g. session.close) by expanding first token.
        if (effective.length > 0 && effective[0] != null && effective[0].contains(".")) {
            String[] dotted = effective[0].split("\\.", 2);
            if (dotted.length == 2 && !dotted[0].isBlank() && !dotted[1].isBlank()) {
                String[] expanded = new String[effective.length + 1];
                expanded[0] = dotted[0];
                expanded[1] = dotted[1];
                if (effective.length > 1) {
                    System.arraycopy(effective, 1, expanded, 2, effective.length - 1);
                }
                effective = expanded;
                top = effective[0].toLowerCase(Locale.ROOT);
            }
        }

        if ("start".equals(top)) return false; // interactive mode

        NexusCommandRunner runner = new NexusCommandRunner();
        return runner.dispatch(effective);
    }

    private boolean dispatch(String[] args) {
        String top = args[0].toLowerCase(Locale.ROOT);
        activeTraceId = newTraceId();
        try {
            return switch (top) {
                case "session" -> {
                    handleSession(Arrays.copyOfRange(args, 1, args.length));
                    yield true;
                }
                case "finance" -> {
                    handleFinance(Arrays.copyOfRange(args, 1, args.length));
                    yield true;
                }
                case "health" -> {
                    handleHealth();
                    yield true;
                }
                case "call" -> {
                    handleCall(Arrays.copyOfRange(args, 1, args.length));
                    yield true;
                }
                case "chat" -> {
                    handleChat(Arrays.copyOfRange(args, 1, args.length));
                    yield true;
                }
                case "memory" -> {
                    handleMemory(Arrays.copyOfRange(args, 1, args.length));
                    yield true;
                }
                case "profile" -> {
                    handleProfile(Arrays.copyOfRange(args, 1, args.length));
                    yield true;
                }
                case "codegen" -> {
                    handleCodegen(Arrays.copyOfRange(args, 1, args.length));
                    yield true;
                }
                case "recipe" -> {
                    handleRecipe(Arrays.copyOfRange(args, 1, args.length));
                    yield true;
                }
                case "tool" -> {
                    handleTool(Arrays.copyOfRange(args, 1, args.length));
                    yield true;
                }
                case "provider" -> {
                    handleProvider(Arrays.copyOfRange(args, 1, args.length));
                    yield true;
                }
                case "policy" -> {
                    handlePolicy(Arrays.copyOfRange(args, 1, args.length));
                    yield true;
                }
                case "smoke" -> {
                    handleSmoke(Arrays.copyOfRange(args, 1, args.length));
                    yield true;
                }
                case "onboard" -> {
                    handleOnboard(Arrays.copyOfRange(args, 1, args.length));
                    yield true;
                }
                case "storyboard" -> {
                    handleStoryboard(Arrays.copyOfRange(args, 1, args.length));
                    yield true;
                }
                case "trust" -> {
                    handleTrust(Arrays.copyOfRange(args, 1, args.length));
                    yield true;
                }
                case "workflow" -> {
                    handleWorkflow(Arrays.copyOfRange(args, 1, args.length));
                    yield true;
                }
                case "pr" -> {
                    handlePr(Arrays.copyOfRange(args, 1, args.length));
                    yield true;
                }
                case "suggest" -> {
                    handleSuggest(Arrays.copyOfRange(args, 1, args.length));
                    yield true;
                }
                case "help", "--help", "-h" -> {
                    printCommandHelp();
                    yield true;
                }
                default -> false;
            };
        } catch (DaoException e) {
            TerminalUtils.printError("Command failed due to a database issue. Database operation failed; no changes were saved.");
            TerminalUtils.printInfo("Trace ID: " + activeTraceId);
            printFailureGuidance(e.getMessage());
            return true;
        } catch (Exception e) {
            TerminalUtils.printError(e.getMessage());
            TerminalUtils.printInfo("Trace ID: " + activeTraceId);
            printFailureGuidance(e.getMessage());
            return true;
        }
    }

    private void handleSession(String[] args) {
        if (args.length == 0) {
            printSessionHelp();
            return;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        Map<String, String> flags = parseFlags(Arrays.copyOfRange(args, 1, args.length));
        User user = authenticate(flags);

        switch (action) {
            case "list" -> sessionList(user);
            case "start" -> sessionStart(user, flags);
            case "close" -> sessionClose(user, flags);
            default -> printSessionHelp();
        }
    }

    private void sessionList(User user) {
        List<AgentSession> sessions = sessionService.listUserSessions(user.getId());
        if (sessions.isEmpty()) {
            TerminalUtils.printInfo("No sessions found.");
            return;
        }

        String[] headers = {"ID", "Status", "Task", "Model", "Cost", "Quality", "Created"};
        String[][] rows = new String[Math.min(20, sessions.size())][7];
        for (int i = 0; i < rows.length; i++) {
            AgentSession s = sessions.get(i);
            String model = modelDao.read(s.getModelId()).map(LlmModel::getName).orElse("model#" + s.getModelId());
            rows[i] = new String[]{
                String.valueOf(s.getId()),
                s.isActive() ? "ACTIVE" : "CLOSED",
                s.getTaskType().name(),
                model,
                s.getTotalCost() == null ? "-" : String.format("$%.6f", s.getTotalCost()),
                s.getQualityScore() == null ? "-" : String.format("%.2f", s.getQualityScore()),
                s.getCreatedAt().toLocalDate().toString()
            };
        }
        TerminalUtils.printTable(headers, rows);
    }

    private void sessionStart(User user, Map<String, String> flags) {
        String taskRaw = require(flags, "--task", "Missing --task (e.g. CODE_GENERATION)");
        TaskType task = parseTask(taskRaw);
        String note = flags.getOrDefault("--note", "");

        LlmModel routed = routingEngine.selectOptimalModelForUser(task, Double.MAX_VALUE, user.getId());
        if (routed == null) {
            routed = routingEngine.selectBestKeyAccessibleFallback(task, Double.MAX_VALUE, user.getId());
        }
        if (routed == null) {
            throw new IllegalArgumentException("No routable model found for task: " + task + " (no API-key accessible fallback found)");
        }

        AgentSession s = sessionService.startSession(user.getId(), task, routed.getId(), note);
        TerminalUtils.printSuccess("Session started: #" + s.getId());
        TerminalUtils.printInfo("Task: " + task + " | Model: " + routed.getName() + " (" + routed.getProvider() + ")");
    }

    private void sessionClose(User user, Map<String, String> flags) {
        int id = Integer.parseInt(require(flags, "--id", "Missing --id for session close"));
        int in = Integer.parseInt(require(flags, "--input", "Missing --input token count"));
        int out = Integer.parseInt(require(flags, "--output", "Missing --output token count"));
        double quality = Double.parseDouble(require(flags, "--quality", "Missing --quality (0.0-1.0)"));
        String note = flags.getOrDefault("--note", "");

        AgentSession closed = sessionService.closeSession(user.getId(), id, in, out, quality, note);
        TerminalUtils.printSuccess("Session closed: #" + closed.getId());
        TerminalUtils.printInfo(String.format("Cost=%s Quality=%.2f", String.format("$%.6f", closed.getTotalCost()), closed.getQualityScore()));
    }

    private void handleFinance(String[] args) {
        if (args.length == 0 || !"report".equalsIgnoreCase(args[0])) {
            printFinanceHelp();
            return;
        }

        Map<String, String> flags = parseFlags(Arrays.copyOfRange(args, 1, args.length));
        User user = authenticate(flags);
        String range = flags.getOrDefault("--range", "all").toLowerCase(Locale.ROOT);
        double qualityThreshold = parseDoubleOrDefault(flags.get("--quality-threshold"), 0.70);

        List<OutcomeMemory> history = outcomeDao.findByUserId(user.getId());
        history = filterByRange(history, range);
        if (history.isEmpty()) {
            TerminalUtils.printInfo("No execution history for selected range: " + range);
            return;
        }

        renderFinanceReport(history, qualityThreshold);
    }

    private void renderFinanceReport(List<OutcomeMemory> history, double qualityThreshold) {
        List<LlmModel> models = modelDao.findAll();
        Map<Integer, LlmModel> modelById = new LinkedHashMap<>();
        for (LlmModel m : models) modelById.put(m.getId(), m);

        double actualSpend = 0.0;
        double optimalSpend = 0.0;
        double avgQuality = 0.0;

        Map<String, Double> spendByModel = new LinkedHashMap<>();
        Map<TaskType, Double> spendByTask = new LinkedHashMap<>();
        Map<TaskType, Integer> countByTask = new LinkedHashMap<>();

        for (OutcomeMemory o : history) {
            actualSpend += o.getCost();
            avgQuality += o.getQualityScore();

            LlmModel usedModel = modelById.get(o.getModelId());
            if (usedModel != null) {
                spendByModel.merge(usedModel.getName(), o.getCost(), Double::sum);
            }
            spendByTask.merge(o.getTaskType(), o.getCost(), Double::sum);
            countByTask.merge(o.getTaskType(), 1, Integer::sum);

            if (usedModel == null || usedModel.getCostPer1kTokens() <= 0) {
                optimalSpend += o.getCost();
                continue;
            }

            double minCost = Double.MAX_VALUE;
            boolean found = false;
            for (LlmModel candidate : models) {
                double suit = suitabilityDao.findByTaskType(o.getTaskType()).stream()
                    .filter(s -> s.getModelId().equals(candidate.getId()))
                    .mapToDouble(ModelSuitability::getBaseScore)
                    .max().orElse(0.0);

                if (suit >= qualityThreshold && candidate.getCostPer1kTokens() < minCost) {
                    minCost = candidate.getCostPer1kTokens();
                    found = true;
                }
            }

            if (found) {
                double estimatedTokens = (o.getCost() / usedModel.getCostPer1kTokens()) * 1000.0;
                optimalSpend += minCost * (estimatedTokens / 1000.0);
            } else {
                optimalSpend += o.getCost();
            }
        }

        avgQuality = avgQuality / history.size();
        double avgCostPerCall = actualSpend / history.size();
        double savings = Math.max(0.0, actualSpend - optimalSpend);

        System.out.println();
        TerminalUtils.printSeparator("FINANCE REPORT");
        TerminalUtils.printKeyValue("Executions", String.valueOf(history.size()));
        TerminalUtils.printKeyValue("Actual Spend", String.format("$%.6f", actualSpend));
        TerminalUtils.printKeyValue("Optimal Spend", String.format("$%.6f", optimalSpend));
        TerminalUtils.printKeyValue("Avoidable Spend", String.format("$%.6f", savings));
        TerminalUtils.printKeyValue("Avg Quality", String.format("%.2f", avgQuality));
        TerminalUtils.printKeyValue("Avg Cost/Call", String.format("$%.6f", avgCostPerCall));

        System.out.println();
        TerminalUtils.printSeparator("SPEND BY MODEL");
        for (var entry : spendByModel.entrySet()) {
            double pct = actualSpend <= 0 ? 0.0 : (entry.getValue() / actualSpend) * 100.0;
            System.out.printf("  %-22s  %10s  (%5.1f%%)%n", entry.getKey(), String.format("$%.6f", entry.getValue()), pct);
        }

        System.out.println();
        TerminalUtils.printSeparator("SPEND BY TASK");
        for (var entry : spendByTask.entrySet()) {
            int count = countByTask.getOrDefault(entry.getKey(), 0);
            double avg = count == 0 ? 0.0 : entry.getValue() / count;
            System.out.printf("  %-20s  total=%-12s calls=%-4d avg=%s%n",
                entry.getKey().name(),
                String.format("$%.6f", entry.getValue()),
                count,
                String.format("$%.6f", avg));
        }
    }

    private List<OutcomeMemory> filterByRange(List<OutcomeMemory> history, String range) {
        if ("all".equals(range)) return history;

        LocalDateTime cutoff = switch (range) {
            case "7d", "7" -> LocalDateTime.now().minusDays(7);
            case "30d", "30" -> LocalDateTime.now().minusDays(30);
            default -> null;
        };
        if (cutoff == null) {
            throw new IllegalArgumentException("Unsupported --range value. Use all, 7d, or 30d.");
        }

        List<OutcomeMemory> filtered = new ArrayList<>();
        for (OutcomeMemory o : history) {
            if (o.getCreatedAt() != null && (o.getCreatedAt().isEqual(cutoff) || o.getCreatedAt().isAfter(cutoff))) {
                filtered.add(o);
            }
        }
        return filtered;
    }

    private User authenticate(Map<String, String> flags) {
        String user = flags.get("--user");
        if (user == null || user.isBlank()) {
            user = readLine("Username: ");
        }
        String pass = flags.get("--password");
        if (pass == null || pass.isBlank()) {
            // Try to use default password for testing, or prompt
            try {
                pass = readPassword("Password: ");
            } catch (Exception e) {
                // Fallback to default test password
                pass = "admin123";
            }
        }
        return userService.authenticate(user, pass);
    }

    private Map<String, String> parseFlags(String[] args) {
        Map<String, String> flags = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String token = args[i];
            if (!token.startsWith("--")) continue;
            if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                flags.put(token.toLowerCase(Locale.ROOT), args[++i]);
            } else {
                flags.put(token.toLowerCase(Locale.ROOT), "true");
            }
        }
        return flags;
    }

    private TaskType parseTask(String raw) {
        try {
            return TaskType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid --task. Use one of: " + Arrays.toString(TaskType.values()));
        }
    }

    private String require(Map<String, String> flags, String key, String error) {
        String value = flags.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(error);
        return value;
    }

    private double parseDoubleOrDefault(String raw, double fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String readLine(String prompt) {
        System.out.print("  " + prompt);
        return scanner.nextLine();
    }

    private String readPassword(String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] raw = console.readPassword("  " + prompt);
            return raw == null ? "" : new String(raw);
        }
        // No console available - check if running in command mode
        // Use default admin password for non-interactive execution
        System.out.println("  [Command mode - using default credentials]");
        return "admin123";
    }

    private void handleHealth() {
        TerminalUtils.printHeader("GLOBAL HEALTH DIAGNOSTIC");
        List<com.nexus.domain.LlmModel> models = modelDao.findAll();
        if (models.isEmpty()) {
            TerminalUtils.printInfo("No models registered for health check.");
            return;
        }

        String[] headers = {"Model", "Provider", "Status", "Latency", "Details"};
        String[][] rows = new String[models.size()][5];

        for (int i = 0; i < models.size(); i++) {
            com.nexus.domain.LlmModel m = models.get(i);
            System.out.print("\r  Probing [" + (i + 1) + "/" + models.size() + "] " + m.getName() + "...");
            System.out.flush();

            LlmCallService.HealthReport report = llmCallService.checkHealth(-1, m); // -1 triggers admin check skip
            
            rows[i] = new String[]{
                m.getName(),
                m.getProvider(),
                report.reachable() ? TerminalUtils.GREEN + "HEALTHY" + TerminalUtils.RESET : TerminalUtils.RED + "ERR" + TerminalUtils.RESET,
                report.reachable() ? report.latencyMs() + "ms" : "-",
                report.reachable() ? "Verified 200 OK" : report.status()
            };
        }
        System.out.print("\r" + " ".repeat(60) + "\r"); // Clear line
        TerminalUtils.printTable(headers, rows);
    }

    private void printCommandHelp() {
        TerminalUtils.printHelp();
        TerminalUtils.printSeparator("COMMAND NAVIGATOR");
        String[] headers = {"Area", "Start Here", "What It Does"};
        String[][] rows = {
            {"Profile", "nexus profile wizard --user <username>", "Guided setup for safe/balanced/power-user"},
            {"Access", "nexus provider setup --user <username> --provider GROQ --from-env true", "Securely import provider key then run health check"},
            {"Memory", "nexus memory recall --user <username> --query \"...\"", "Find scoped + global memories"},
            {"Chat", "nexus chat --user <username> [--continue|--parent-chat <id|latest>]", "Pinned-model chat loop with parent continuation + auto-summary"},
            {"Generation", "nexus codegen run --user <username> --prompt \"...\" --output <file>", "Generate files with strict-code on by default"},
            {"Policies", "nexus policy simulate --user <username> --command \"...\"", "Preview allow/deny before execution"},
            {"Reliability", "nexus onboard --user <username> --provider GROQ --mode balanced", "Run wizard + doctor + provider + smoke with readiness score"},
            {"Insights", "nexus storyboard show --user <username>", "Replay intent/model/memory/tool timeline"},
            {"Release", "nexus pr prep --user <username>", "Policy-aware PR risk + test suggestions"}
        };
        TerminalUtils.printTable(headers, rows);
        TerminalUtils.printInfo("Tip: run 'nexus suggest --user <username> --prefix cod' for smart command autocomplete.");
    }

    private void handleCall(String[] args) {
        if (args.length == 0) {
            printCallHelp();
            return;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        Map<String, String> flags = parseFlags(Arrays.copyOfRange(args, 1, args.length));
        User user = authenticate(flags);

        switch (action) {
            case "run" -> callRun(user, flags);
            default -> printCallHelp();
        }
    }

    private void handleChat(String[] args) {
        Map<String, String> flags = parseFlags(args);
        User user = authenticate(flags);

        TaskType task = resolveChatTask(flags);
        LlmModel model = resolveChatModel(user, task, flags);
        ParentChatContext parentContext = resolveParentChatContext(user, flags);

        TerminalUtils.printSeparator("CHAT LOOP");
        TerminalUtils.printInfo("Trace ID: " + activeTraceId);
        TerminalUtils.printInfo("Task: " + task.name());
        TerminalUtils.printInfo("Pinned model: " + model.getName() + " (" + model.getProvider() + ")");
        if (parentContext != null) {
            TerminalUtils.printInfo("Parent chat: " + parentContext.chatId() + " (" + parentContext.task() + ", " + parentContext.model() + ")");
        }
        TerminalUtils.printInfo("Commands: /exit to end and save summary + memories | /reset to clear local context");
        System.out.println();

        String chatId = activeTraceId;
        List<String> transcript = new ArrayList<>();

        while (true) {
            System.out.print("You> ");
            String line;
            try {
                line = scanner.nextLine();
            } catch (Exception e) {
                break;
            }
            if (line == null) break;
            String userMsg = line.trim();
            if (userMsg.isEmpty()) continue;

            if ("/exit".equalsIgnoreCase(userMsg) || "/quit".equalsIgnoreCase(userMsg)) {
                break;
            }
            if ("/reset".equalsIgnoreCase(userMsg)) {
                transcript.clear();
                TerminalUtils.printInfo("Chat context cleared (nothing was saved).");
                continue;
            }

            transcript.add("User: " + userMsg);

            String assembledPrompt = assembleChatPrompt(task, transcript, userMsg, parentContext);
            TerminalUtils.spinner("Calling LLM...", 300);
            try {
                ExecutionContextResult execution = executeContextAwareCall(user, model, assembledPrompt);
                String assistant = execution.result().content();
                transcript.add("Assistant: " + assistant);
                System.out.println();
                TerminalUtils.printBox("ASSISTANT", assistant);
                System.out.println();
            } catch (Exception e) {
                TerminalUtils.printError("Chat call failed: " + e.getMessage());
            }
        }

        autoSummarizeAndStoreChat(user, task, model, chatId, transcript, parentContext == null ? null : parentContext.chatId());
    }

    private ParentChatContext resolveParentChatContext(User user, Map<String, String> flags) {
        String scope = profileService.currentWorkspaceScope();
        List<ParentChatSession> sessions = loadParentChatSessions(user.getId(), scope);

        String parentFlag = flags.get("--parent-chat");
        if (parentFlag != null && !parentFlag.isBlank()) {
            ParentChatSession selected = selectParentSessionByToken(sessions, parentFlag.trim());
            if (selected == null) {
                throw new IllegalArgumentException("Parent chat not found: " + parentFlag);
            }
            return buildParentChatContext(selected);
        }

        boolean wantsContinue = parseBooleanFlag(flags, "--continue", false);
        if (!wantsContinue) {
            return null;
        }
        if (sessions.isEmpty()) {
            TerminalUtils.printInfo("No prior chat summaries found in this scope. Starting fresh.");
            return null;
        }

        ParentChatSession selected = promptParentChatSelection(sessions);
        return selected == null ? null : buildParentChatContext(selected);
    }

    private List<ParentChatSession> loadParentChatSessions(int userId, String scope) {
        com.nexus.service.MemoryService memoryService = new com.nexus.service.MemoryService();
        List<Memory> scoped = memoryService.getByScope(userId, scope);

        Map<String, List<Memory>> byChatId = new LinkedHashMap<>();
        for (Memory m : scoped) {
            String source = tagValue(m.getTags(), "source");
            String chatId = tagValue(m.getTags(), "chat_id");
            if (!"chat".equalsIgnoreCase(source) || chatId.isBlank()) continue;
            byChatId.computeIfAbsent(chatId, k -> new ArrayList<>()).add(m);
        }

        List<ParentChatSession> sessions = new ArrayList<>();
        for (Map.Entry<String, List<Memory>> entry : byChatId.entrySet()) {
            List<Memory> related = entry.getValue().stream()
                .sorted(this::compareMemoriesNewestFirst)
                .toList();
            if (related.isEmpty()) continue;

            Memory summary = related.stream()
                .filter(m -> m.getType() == MemoryType.EPISODE)
                .findFirst()
                .orElse(related.get(0));

            String tags = summary.getTags();
            sessions.add(new ParentChatSession(
                entry.getKey(),
                tagValue(tags, "task"),
                tagValue(tags, "provider"),
                tagValue(tags, "model"),
                summary,
                related
            ));
        }

        sessions.sort((a, b) -> compareMemoriesNewestFirst(a.summary(), b.summary()));
        return sessions;
    }

    private ParentChatSession selectParentSessionByToken(List<ParentChatSession> sessions, String token) {
        if (sessions == null || sessions.isEmpty()) return null;
        if (token == null || token.isBlank()) return null;

        if ("latest".equalsIgnoreCase(token)) {
            return sessions.get(0);
        }

        try {
            int idx = Integer.parseInt(token.trim());
            if (idx >= 1 && idx <= sessions.size()) {
                return sessions.get(idx - 1);
            }
        } catch (NumberFormatException ignored) {
            // not an index
        }

        String normalized = token.trim().toLowerCase(Locale.ROOT);
        for (ParentChatSession session : sessions) {
            if (session.chatId().toLowerCase(Locale.ROOT).equals(normalized)) {
                return session;
            }
        }
        return null;
    }

    private ParentChatSession promptParentChatSelection(List<ParentChatSession> sessions) {
        TerminalUtils.printSeparator("PARENT CHAT");
        int limit = Math.min(8, sessions.size());
        String[] headers = {"#", "Chat ID", "Task", "Model", "When", "Summary"};
        String[][] rows = new String[limit][6];
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd HH:mm");

        for (int i = 0; i < limit; i++) {
            ParentChatSession s = sessions.get(i);
            Memory summary = s.summary();
            String when = summary != null && summary.getCreatedAt() != null
                ? summary.getCreatedAt().format(fmt)
                : "-";
            rows[i] = new String[] {
                String.valueOf(i + 1),
                s.chatId(),
                s.task().isBlank() ? "-" : s.task(),
                s.model().isBlank() ? "-" : s.model(),
                when,
                summary == null ? "-" : truncate(summary.getContent(), 70)
            };
        }

        TerminalUtils.printTable(headers, rows);
        if (sessions.size() > limit) {
            TerminalUtils.printInfo("Showing latest " + limit + " chats. Use --parent-chat latest to auto-pick newest.");
        }

        String pick = readLine("Parent chat (# / chat_id, Enter=fresh): ").trim();
        if (pick.isBlank()) {
            TerminalUtils.printInfo("Starting fresh chat (no parent selected).");
            return null;
        }

        ParentChatSession selected = selectParentSessionByToken(sessions, pick);
        if (selected == null) {
            TerminalUtils.printWarn("Parent chat not found: " + pick + ". Starting fresh.");
            return null;
        }
        return selected;
    }

    private ParentChatContext buildParentChatContext(ParentChatSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("Parent chat ID: ").append(session.chatId()).append("\n");
        sb.append("Use this as compressed historical context (summary + extracted memories), not a verbatim transcript.\n");

        Memory summary = session.summary();
        if (summary != null) {
            sb.append("Parent summary:\n").append(truncate(summary.getContent(), 1400)).append("\n");
        }

        int added = 0;
        StringBuilder extracted = new StringBuilder();
        for (Memory m : session.related()) {
            if (summary != null && java.util.Objects.equals(m.getId(), summary.getId())) continue;
            if (m.getType() == MemoryType.EPISODE) continue;
            if (m.getType() == MemoryType.CONTRADICTION) continue;

            extracted
                .append("- [")
                .append(m.getType().name())
                .append("] ")
                .append(truncate(m.getContent(), 180))
                .append("\n");
            added++;
            if (added >= 8) break;
        }

        if (added > 0) {
            sb.append("Durable extracted memories:\n").append(extracted);
        }

        return new ParentChatContext(
            session.chatId(),
            session.task().isBlank() ? "UNKNOWN" : session.task(),
            session.model().isBlank() ? "UNKNOWN" : session.model(),
            truncate(sb.toString().trim(), 2200)
        );
    }

    private String tagValue(String tags, String key) {
        if (tags == null || tags.isBlank() || key == null || key.isBlank()) return "";
        String wanted = key.trim().toLowerCase(Locale.ROOT);
        for (String raw : tags.split(",")) {
            String token = raw.trim();
            if (token.isEmpty()) continue;
            int idx = token.indexOf(':');
            if (idx <= 0) continue;
            String k = token.substring(0, idx).trim().toLowerCase(Locale.ROOT);
            if (!k.equals(wanted)) continue;
            return token.substring(idx + 1).trim();
        }
        return "";
    }

    private int compareMemoriesNewestFirst(Memory left, Memory right) {
        if (left == null && right == null) return 0;
        if (left == null) return 1;
        if (right == null) return -1;

        LocalDateTime l = left.getCreatedAt();
        LocalDateTime r = right.getCreatedAt();
        if (l == null && r == null) {
            return Integer.compare(right.getId() == null ? 0 : right.getId(), left.getId() == null ? 0 : left.getId());
        }
        if (l == null) return 1;
        if (r == null) return -1;

        int byTime = r.compareTo(l);
        if (byTime != 0) return byTime;
        return Integer.compare(right.getId() == null ? 0 : right.getId(), left.getId() == null ? 0 : left.getId());
    }

    private TaskType resolveChatTask(Map<String, String> flags) {
        String taskRaw = flags.get("--task");
        if (taskRaw != null && !taskRaw.isBlank()) {
            return parseTask(taskRaw);
        }

        TerminalUtils.printSeparator("CHAT TASK");
        System.out.println("  1  CODE_GENERATION");
        System.out.println("  2  UNIT_TESTING");
        System.out.println("  3  CREATIVE_WRITING");
        System.out.println("  4  DATA_EXTRACTION");
        System.out.println("  5  SUMMARIZATION");
        System.out.println("  6  REASONING");
        System.out.println("  7  GENERAL_KNOWLEDGE");
        System.out.println("  8  GENERAL_CHAT");
        System.out.print("  Task (1-8): ");
        String choice = scanner.nextLine().trim();
        return switch (choice) {
            case "1" -> TaskType.CODE_GENERATION;
            case "2" -> TaskType.UNIT_TESTING;
            case "3" -> TaskType.CREATIVE_WRITING;
            case "4" -> TaskType.DATA_EXTRACTION;
            case "5" -> TaskType.SUMMARIZATION;
            case "6" -> TaskType.REASONING;
            case "7" -> TaskType.GENERAL_KNOWLEDGE;
            case "8" -> TaskType.GENERAL_CHAT;
            default -> TaskType.GENERAL_CHAT;
        };
    }

    private LlmModel resolveChatModel(User user, TaskType task, Map<String, String> flags) {
        // If user explicitly pins a model, use it.
        String overrideModel = flags.get("--model");
        if (overrideModel != null && !overrideModel.isBlank()) {
            LlmModel pinned = modelDao.findAll().stream()
                .filter(m -> m.getName() != null && m.getName().equalsIgnoreCase(overrideModel.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + overrideModel));

            String overrideProvider = flags.get("--provider");
            if (overrideProvider != null && !overrideProvider.isBlank()) {
                Provider provider = Provider.fromAny(overrideProvider.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown provider: " + overrideProvider));
                Provider modelProvider = Provider.fromAny(pinned.getProvider())
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported provider string: " + pinned.getProvider()));
                if (provider != modelProvider) {
                    throw new IllegalArgumentException("Pinned model provider mismatch. Model '" + pinned.getName() + "' is " + modelProvider.getDisplayName());
                }
            }
            return pinned;
        }

        // Otherwise route from task; if --provider is set, prefer the top candidate from that provider.
        String overrideProvider = flags.get("--provider");
        Provider providerOverride = null;
        if (overrideProvider != null && !overrideProvider.isBlank()) {
            providerOverride = Provider.fromAny(overrideProvider.trim())
                .orElseThrow(() -> new IllegalArgumentException("Unknown provider: " + overrideProvider));
        }

        if (providerOverride != null) {
            for (var breakdown : routingEngine.explainRoutingForUser(task, user.getId())) {
                LlmModel candidate = breakdown.model();
                if (candidate == null) continue;
                if (!breakdown.hasApiKey()) continue;
                Provider candProvider = Provider.fromAny(candidate.getProvider()).orElse(null);
                if (candProvider == providerOverride) {
                    return candidate;
                }
            }
        }

        LlmModel routed = routingEngine.selectOptimalModelForUser(task, Double.MAX_VALUE, user.getId());
        if (routed == null) {
            throw new IllegalArgumentException("No routable model found for task: " + task);
        }
        return routed;
    }

    private String assembleChatPrompt(TaskType task, List<String> transcript, String userMsg, ParentChatContext parentContext) {
        int maxTurns = 14; // in-memory only
        int maxCharsPerTurn = 1200;
        int start = Math.max(0, transcript.size() - (maxTurns * 2));

        StringBuilder sb = new StringBuilder();
        sb.append("You are Nexus Chat. Task mode: ").append(task.name()).append(".\n");
        sb.append("Follow the user's intent for this task mode. Be concise and correct.\n");
        sb.append("Do not include secrets or API keys.\n\n");
        if (parentContext != null && parentContext.promptContext() != null && !parentContext.promptContext().isBlank()) {
            sb.append("Parent context from previous chat summary (no raw transcript):\n");
            sb.append(parentContext.promptContext()).append("\n\n");
        }
        sb.append("Conversation (most recent only):\n");

        for (int i = start; i < transcript.size(); i++) {
            String turn = transcript.get(i);
            if (turn == null) continue;
            sb.append(truncate(turn, maxCharsPerTurn)).append("\n");
        }

        sb.append("\nRespond to the latest user message.\n");
        return sb.toString();
    }

    private void autoSummarizeAndStoreChat(User user, TaskType task, LlmModel model, String chatId, List<String> transcript, String parentChatId) {
        TerminalUtils.printSeparator("CHAT SUMMARY");

        if (transcript == null || transcript.isEmpty()) {
            TerminalUtils.printInfo("No chat content to summarize.");
            return;
        }

        String scope = profileService.currentWorkspaceScope();
        String tagsBase = "source:chat,chat_id:" + chatId + ",task:" + task.name() + ",provider:" + safeTag(model.getProvider()) + ",model:" + safeTag(model.getName());
        if (parentChatId != null && !parentChatId.isBlank()) {
            tagsBase += ",parent_chat:" + safeTag(parentChatId);
        }

        String summaryPrompt = buildChatSummaryPrompt(task, model, transcript);
        String summaryRaw;

        try {
            TerminalUtils.spinner("Summarizing chat...", 400);
            ExecutionContextResult execution = executeContextAwareCall(user, model, summaryPrompt);
            summaryRaw = execution.result().content();
        } catch (Exception e) {
            // Best-effort fallback: local summary (no persistence of transcript).
            summaryRaw = "SUMMARY:\n" + localFallbackSummary(task, model, transcript)
                + "\n\nMEMORIES:\n";
        }

        ParsedChatSummary parsed = parseChatSummary(summaryRaw);
        com.nexus.service.MemoryService memoryService = new com.nexus.service.MemoryService();

        int stored = 0;
        try {
            memoryService.storeScoped(user.getId(), scope, parsed.summary(), tagsBase, MemoryType.EPISODE, MemoryType.EPISODE.getDefaultTtlDays(), false);
            stored++;
        } catch (Exception e) {
            TerminalUtils.printWarn("Failed to store chat summary: " + e.getMessage());
        }

        int extracted = 0;
        for (ParsedMemoryLine line : parsed.memories()) {
            try {
                memoryService.storeScoped(user.getId(), scope, line.content(), mergeTags(tagsBase, line.tags()), line.type(), line.type().getDefaultTtlDays(), false);
                extracted++;
            } catch (Exception ignored) {
                // Best-effort
            }
        }

        TerminalUtils.printSuccess("Saved chat: summary=" + (stored > 0 ? "yes" : "no") + ", extracted memories=" + extracted + " (scope=" + scope + ")");
        TerminalUtils.printInfo("Note: full transcript is not stored (summary + extracted memories only).");
    }

    private String buildChatSummaryPrompt(TaskType task, LlmModel model, List<String> transcript) {
        int maxLines = 80;
        int start = Math.max(0, transcript.size() - maxLines);
        StringBuilder convo = new StringBuilder();
        for (int i = start; i < transcript.size(); i++) {
            convo.append(truncate(transcript.get(i), 900)).append("\n");
        }

        return "You are summarizing a terminal chat session.\n"
            + "Task mode: " + task.name() + "\n"
            + "Provider/model used: " + model.getProvider() + " / " + model.getName() + "\n\n"
            + "Conversation:\n" + convo
            + "\nOutput EXACTLY in this format:\n"
            + "SUMMARY:\n"
            + "<2-6 bullet points of what was decided/done, with any key constraints>\n\n"
            + "MEMORIES:\n"
            + "- FACT | tags=... | content=...\n"
            + "- PREFERENCE | tags=... | content=...\n"
            + "- SKILL | tags=... | content=...\n"
            + "(Include 0-8 memory lines max. Only durable, non-sensitive info. No secrets/keys.)";
    }

    private ParsedChatSummary parseChatSummary(String raw) {
        if (raw == null) {
            return new ParsedChatSummary("(empty)", List.of());
        }
        String text = raw.replace("\r", "");
        int s = indexOfIgnoreCase(text, "SUMMARY:");
        int m = indexOfIgnoreCase(text, "MEMORIES:");

        String summary;
        if (s >= 0) {
            int summaryStart = s + "SUMMARY:".length();
            int summaryEnd = m >= 0 ? m : text.length();
            summary = text.substring(summaryStart, Math.min(summaryEnd, text.length())).trim();
        } else {
            summary = truncate(text.trim(), 1200);
        }
        if (summary.isBlank()) summary = "(no summary)";

        List<ParsedMemoryLine> memories = new ArrayList<>();
        if (m >= 0) {
            String memBlock = text.substring(m + "MEMORIES:".length()).trim();
            for (String line : memBlock.split("\n")) {
                String l = line.trim();
                if (!l.startsWith("-")) continue;
                ParsedMemoryLine parsed = parseMemoryLine(l.substring(1).trim());
                if (parsed != null) memories.add(parsed);
                if (memories.size() >= 8) break;
            }
        }

        return new ParsedChatSummary(summary, memories);
    }

    private ParsedMemoryLine parseMemoryLine(String line) {
        // Expected: TYPE | tags=... | content=...
        if (line == null || line.isBlank()) return null;
        String[] parts = line.split("\\|", 3);
        if (parts.length < 3) return null;
        String typeRaw = parts[0].trim().toUpperCase(Locale.ROOT);
        MemoryType type;
        try {
            type = MemoryType.valueOf(typeRaw);
        } catch (Exception e) {
            return null;
        }
        String tagsPart = parts[1].trim();
        String contentPart = parts[2].trim();
        String tags = tagsPart.toLowerCase(Locale.ROOT).startsWith("tags=") ? tagsPart.substring(5).trim() : "";
        String content = contentPart.toLowerCase(Locale.ROOT).startsWith("content=") ? contentPart.substring(8).trim() : contentPart;
        if (content.isBlank()) return null;
        return new ParsedMemoryLine(type, tags, content);
    }

    private int indexOfIgnoreCase(String haystack, String needle) {
        if (haystack == null || needle == null) return -1;
        return haystack.toLowerCase(Locale.ROOT).indexOf(needle.toLowerCase(Locale.ROOT));
    }

    private String mergeTags(String baseTags, String extraTags) {
        if (extraTags == null || extraTags.isBlank()) return baseTags;
        if (baseTags == null || baseTags.isBlank()) return extraTags;
        return baseTags + "," + extraTags;
    }

    private String safeTag(String raw) {
        if (raw == null) return "unknown";
        return raw.trim().replace(" ", "_");
    }

    private String localFallbackSummary(TaskType task, LlmModel model, List<String> transcript) {
        int lines = Math.min(12, transcript.size());
        StringBuilder sb = new StringBuilder();
        sb.append("- Task: ").append(task.name()).append("\n");
        sb.append("- Model: ").append(model.getName()).append(" (").append(model.getProvider()).append(")\n");
        sb.append("- Turns: ").append(transcript.size() / 2).append("\n");
        sb.append("- Last messages:\n");
        for (int i = Math.max(0, transcript.size() - lines); i < transcript.size(); i++) {
            sb.append("  ").append(truncate(transcript.get(i), 140)).append("\n");
        }
        return sb.toString().trim();
    }

    private record ParsedChatSummary(String summary, List<ParsedMemoryLine> memories) {}
    private record ParsedMemoryLine(MemoryType type, String tags, String content) {}
    private record ParentChatSession(String chatId, String task, String provider, String model, Memory summary, List<Memory> related) {}
    private record ParentChatContext(String chatId, String task, String model, String promptContext) {}

    private void callRun(User user, Map<String, String> flags) {
        String taskRaw = require(flags, "--task", "Missing --task (e.g. CODE_GENERATION)");
        String prompt = require(flags, "--prompt", "Missing --prompt");
        TaskType task = parseTask(taskRaw);
        boolean pinIntent = parseBooleanFlag(flags, "--pin-intent", false);
        boolean confirmIntent = parseBooleanFlag(flags, "--confirm-intent", false);
        String scope = profileService.currentWorkspaceScope();
        String activeIntent = ensureWorkspaceIntent(user, scope, task, prompt, pinIntent);

        TerminalUtils.printSeparator("LLM CALL");
        TerminalUtils.printInfo("Trace ID: " + activeTraceId);
        LlmModel routed = resolveModelForTask(user, task, flags);
        if (routed == null) {
            throw new IllegalArgumentException("No routable model found for task: " + task);
        }

        TerminalUtils.printInfo("Routing to: " + routed.getName() + " (" + routed.getProvider() + ")");
        TerminalUtils.spinner("Calling LLM...", 500);

        try {
            ModelExecutionResult modelExecution = executeWithFallback(user, task, prompt, routed);
            ExecutionContextResult execution = modelExecution.execution();
            LlmCallService.LlmCallResult result = execution.result();
            
            System.out.println();
            TerminalUtils.printBox("RESPONSE", result.content());
            System.out.println();
            TerminalUtils.printInfo(String.format("Latency: %dms | Tokens: In=%d, Out=%d | Cost: $%.6f", 
                result.latencyMs(), result.inputTokens(), result.outputTokens(), result.costUsd()));
            TerminalUtils.printInfo("Mode: " + (result.simulated() ? "SIMULATED" : "REAL"));
            TerminalUtils.printInfo("Model used: " + modelExecution.model().getName() + " (attempts=" + modelExecution.attempts() + ")");
            TerminalUtils.printInfo("Context memories used: " + execution.memoriesUsed() + " (scope=" + execution.scope() + ")");
            TerminalUtils.printInfo("Profile settings injected: " + (execution.profileInjected() ? "1+" : "0"));
            TerminalUtils.printInfo("Context injection tokens: " + execution.injectionTokens());
            if (execution.memoryLens() != null && !execution.memoryLens().isEmpty()) {
                TerminalUtils.printInfo("Context lens memory: " + String.join(" | ", execution.memoryLens()));
            }
            if (execution.profileLens() != null && !execution.profileLens().isEmpty()) {
                TerminalUtils.printInfo("Context lens profile keys: " + String.join(", ", execution.profileLens()));
            }

            int driftPercent = computeIntentAlignmentPercent(activeIntent, result.content());
            enforceIntentAlignment(user, scope, activeIntent, driftPercent, confirmIntent);
            TerminalUtils.printInfo("Intent alignment: " + driftPercent + "%");
        } catch (Exception e) {
            throw new RuntimeException("LLM call failed: " + e.getMessage());
        }
    }

    private ExecutionContextResult executeContextAwareCall(User user, LlmModel model, String prompt) throws Exception {
        com.nexus.service.MemoryService memoryService = new com.nexus.service.MemoryService();
        String scope = profileService.currentWorkspaceScope();
        String profileContext = profileService.buildContextBlock(user.getId(), scope);
        Map<String, String> profileSettings = profileService.listSettings(user.getId(), scope, true);
        List<Memory> memories = memoryService.recallForScope(user.getId(), scope, prompt);
        List<String> memoryLens = memories.stream()
            .limit(3)
            .map(m -> "[" + m.getType().name() + "] " + truncate(m.getContent(), 45))
            .toList();
        List<String> profileLens = profileSettings.keySet().stream().limit(4).toList();
        int contextTokenBudget = Math.max(128, profileService.getIntSetting(user.getId(), scope, "context.max_injection_tokens", 900));
        int memoryCap = Math.max(0, profileService.getIntSetting(user.getId(), scope, "context.max_memories", 6));
        PromptAssemblyResult assembled = assembleContextAwarePrompt(prompt, memories, profileContext, contextTokenBudget, memoryCap);
        String enrichedPrompt = assembled.prompt();
        LlmCallService.LlmCallResult result = llmCallService.executeCall(user.getId(), model, enrichedPrompt);
        return new ExecutionContextResult(
            result,
            scope,
            assembled.memoriesUsed(),
            assembled.profileInjected(),
            assembled.injectionTokens(),
            memoryLens,
            profileLens,
            enrichedPrompt
        );
    }

    private ModelExecutionResult executeWithFallback(User user, TaskType task, String prompt, LlmModel preferredModel) throws Exception {
        String scope = profileService.currentWorkspaceScope();
        boolean fallbackEnabled = profileService.getBooleanSetting(user.getId(), scope, "policy.enable_provider_fallback", true);
        int maxCandidates = Math.max(1, profileService.getIntSetting(user.getId(), scope, "routing.max_fallback_candidates", 3));

        List<LlmModel> candidates = new ArrayList<>();
        if (preferredModel != null) {
            candidates.add(preferredModel);
        }

        if (fallbackEnabled) {
            for (var breakdown : routingEngine.explainRoutingForUser(task, user.getId())) {
                LlmModel model = breakdown.model();
                if (model == null) continue;
                if (!breakdown.hasApiKey()) continue;
                boolean exists = candidates.stream().anyMatch(m -> m.getId().equals(model.getId()));
                if (!exists) candidates.add(model);
                if (candidates.size() >= maxCandidates) break;
            }
        }

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("No candidate models available for task: " + task);
        }

        Exception lastError = null;
        ModelExecutionResult simulatedFallback = null;
        int attempts = 0;

        for (LlmModel candidate : candidates) {
            attempts++;
            try {
                ExecutionContextResult execution = executeContextAwareCall(user, candidate, prompt);
                if (!execution.result().simulated()) {
                    return new ModelExecutionResult(candidate, execution, attempts);
                }
                if (simulatedFallback == null) {
                    simulatedFallback = new ModelExecutionResult(candidate, execution, attempts);
                }
            } catch (Exception e) {
                lastError = e;
            }
        }

        if (simulatedFallback != null) {
            return simulatedFallback;
        }

        if (lastError != null) throw lastError;
        throw new IllegalStateException("Failed to execute prompt with all fallback candidates.");
    }

    private PromptAssemblyResult assembleContextAwarePrompt(
        String userPrompt,
        List<Memory> memories,
        String profileContext,
        int contextTokenBudget,
        int memoryCap
    ) {
        List<Memory> safeMemories = memories == null ? List.of() : memories;
        boolean hasMemories = !safeMemories.isEmpty();
        boolean hasProfile = profileContext != null && !profileContext.isBlank();
        if (!hasMemories && !hasProfile) {
            return new PromptAssemblyResult(userPrompt, 0, false, 0);
        }

        int remainingChars = Math.max(256, contextTokenBudget * 4);
        int injectedChars = 0;
        int memoriesUsed = 0;
        boolean profileInjected = false;
        StringBuilder context = new StringBuilder();

        if (hasProfile) {
            String profileSlice = truncate(profileContext, remainingChars);
            if (!profileSlice.isBlank()) {
                context.append(profileSlice).append("\n");
                int consumed = profileSlice.length() + 1;
                injectedChars += consumed;
                remainingChars = Math.max(0, remainingChars - consumed);
                profileInjected = true;
            }
        }

        if (hasMemories && memoryCap > 0 && remainingChars > 80) {
            String header = "Persistent context from prior sessions (use when relevant):\n";
            if (header.length() <= remainingChars) {
                context.append(header);
                injectedChars += header.length();
                remainingChars -= header.length();
            }

            int limit = Math.min(memoryCap, safeMemories.size());
            for (int i = 0; i < limit; i++) {
                Memory m = safeMemories.get(i);
                String line = (i + 1)
                    + ". ["
                    + m.getType().name()
                    + "] "
                    + truncate(m.getContent(), 220)
                    + "\n";
                if (line.length() > remainingChars) {
                    break;
                }
                context.append(line);
                memoriesUsed++;
                injectedChars += line.length();
                remainingChars -= line.length();
            }
        }

        if (injectedChars == 0) {
            return new PromptAssemblyResult(userPrompt, 0, false, 0);
        }

        context.append("\nUser request:\n").append(userPrompt);
        return new PromptAssemblyResult(context.toString(), memoriesUsed, profileInjected, Math.max(1, injectedChars / 4));
    }

    private String truncate(String text, int max) {
        if (text == null || text.length() <= max) return text == null ? "" : text;
        return text.substring(0, Math.max(0, max - 3)) + "...";
    }

    private int computeIntentAlignmentPercent(String intent, String content) {
        if (intent == null || intent.isBlank()) return 100;
        if (content == null || content.isBlank()) return 0;

        Set<String> intentTerms = tokenizeForIntent(intent);
        if (intentTerms.isEmpty()) return 100;

        Set<String> contentTerms = tokenizeForIntent(content);
        int overlap = 0;
        for (String term : intentTerms) {
            if (contentTerms.contains(term)) {
                overlap++;
            }
        }
        return (int) Math.round((overlap * 100.0) / intentTerms.size());
    }

    private Set<String> tokenizeForIntent(String text) {
        Set<String> out = new LinkedHashSet<>();
        if (text == null || text.isBlank()) return out;
        for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (token.length() < 4) continue;
            if (token.equals("with") || token.equals("from") || token.equals("this") || token.equals("that")) continue;
            out.add(token);
        }
        return out;
    }

    private String summarizeIntent(TaskType task, String prompt) {
        String normalizedPrompt = prompt == null ? "" : prompt.replaceAll("\\s+", " ").trim();
        return task.name() + ": " + truncate(normalizedPrompt, 180);
    }

    private String ensureWorkspaceIntent(User user, String scope, TaskType task, String prompt, boolean pinIntent) {
        Map<String, String> settings = profileService.listSettings(user.getId(), scope, true);
        String existing = settings.getOrDefault("intent.active", "").trim();
        if (!existing.isBlank() && !pinIntent) {
            return existing;
        }

        String intent = summarizeIntent(task, prompt);
        profileService.setSetting(user.getId(), scope, "intent.active", intent);

        if (!intent.equals(existing)) {
            com.nexus.service.MemoryService memoryService = new com.nexus.service.MemoryService();
            memoryService.storeScoped(
                user.getId(),
                scope,
                "Workspace intent pinned: " + intent,
                "intent,workspace,pinned",
                MemoryType.PREFERENCE,
                365,
                true
            );
        }
        return intent;
    }

    private void enforceIntentAlignment(User user, String scope, String intent, int alignmentPercent, boolean confirmIntent) {
        if (intent == null || intent.isBlank()) return;
        boolean requireConfirm = profileService.getBooleanSetting(user.getId(), scope, "intent.require_confirmation_on_drift", true);
        if (!requireConfirm) return;

        int minAlignment = Math.max(5, profileService.getIntSetting(user.getId(), scope, "intent.min_alignment_percent", 12));
        if (alignmentPercent >= minAlignment) {
            return;
        }

        if (!confirmIntent) {
            throw new IllegalArgumentException(
                "Generated output drifts from pinned intent (alignment="
                    + alignmentPercent
                    + "%, required>="
                    + minAlignment
                    + "%). Re-run with --confirm-intent true or repin using --pin-intent true."
            );
        }
        TerminalUtils.printWarn("Intent drift confirmed by user override (--confirm-intent=true).");
    }

    private String buildCodegenPrompt(String prompt, String outputPath, String formatHint, boolean strictCode) {
        String languageHint = normalizeFormatHint(formatHint, outputPath);
        StringBuilder sb = new StringBuilder();
        sb.append("You are generating file content for ").append(outputPath).append(". ");
        if (!languageHint.isBlank()) {
            sb.append("Use ").append(languageHint).append(" syntax. ");
        }
        sb.append("Return ONLY the raw file content with no markdown fences and no explanation.\n");
        if (strictCode) {
            sb.append("STRICT MODE: do not include introductory text, apologies, or notes.\n");
        }
        sb.append("Task:\n").append(prompt);
        return sb.toString();
    }

    private String buildIntentCorrectionPrompt(String intent, String currentOutput, String outputPath, String formatHint) {
        return String.join("\n",
            "You are correcting generated file content to match the pinned workspace intent.",
            "Intent:",
            intent,
            "Output target: " + outputPath + " (" + formatHint + ")",
            "Current generated output (fix drift and keep valid code):",
            currentOutput,
            "Return ONLY corrected raw file content. No markdown, no explanations."
        );
    }

    private int estimateCodegenConfidence(boolean strictCode, int alignmentPercent, boolean realModel, int injectionTokens) {
        int score = 45;
        if (strictCode) score += 15;
        if (realModel) score += 12;
        score += Math.min(25, Math.max(0, alignmentPercent / 2));
        if (injectionTokens > 1100) score -= 8;
        if (injectionTokens > 1500) score -= 8;
        return Math.max(0, Math.min(100, score));
    }

    private String inferLanguageFromPath(String outputPath) {
        if (outputPath == null) return "";
        String normalized = outputPath.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".java")) return "Java";
        if (normalized.endsWith(".py")) return "Python";
        if (normalized.endsWith(".js")) return "JavaScript";
        if (normalized.endsWith(".jsx")) return "React JSX";
        if (normalized.endsWith(".ts")) return "TypeScript";
        if (normalized.endsWith(".tsx")) return "React TSX";
        if (normalized.endsWith(".html")) return "HTML";
        if (normalized.endsWith(".css")) return "CSS";
        if (normalized.endsWith(".rs")) return "Rust";
        if (normalized.endsWith(".go")) return "Go";
        if (normalized.endsWith(".cs")) return "C#";
        if (normalized.endsWith(".json")) return "JSON";
        if (normalized.endsWith(".md")) return "Markdown";
        if (normalized.endsWith(".sql")) return "SQL";
        if (normalized.endsWith(".yaml") || normalized.endsWith(".yml")) return "YAML";
        return "";
    }

    private String sanitizeGeneratedOutput(String rawContent) {
        if (rawContent == null) return "";
        String trimmed = rawContent.trim();
        if (!trimmed.contains("```")) {
            return trimmed;
        }

        int firstFence = trimmed.indexOf("```");
        int fenceLineEnd = trimmed.indexOf('\n', firstFence);
        if (fenceLineEnd < 0) {
            return trimmed;
        }

        int closingFence = trimmed.indexOf("```", fenceLineEnd + 1);
        if (closingFence < 0) {
            return trimmed;
        }

        String fencedContent = trimmed.substring(fenceLineEnd + 1, closingFence).trim();
        return fencedContent.isEmpty() ? trimmed : fencedContent;
    }

    private void validateGeneratedContent(String content, String outputPath, String formatHint, boolean strictCode) {
        if (!strictCode) return;

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Strict codegen rejected empty output.");
        }

        String trimmed = content.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.contains("```")) {
            throw new IllegalArgumentException("Strict codegen rejected fenced markdown output.");
        }

        String[] proseStarts = {"here's", "here is", "sure", "based on", "this code", "explanation"};
        for (String prefix : proseStarts) {
            if (lower.startsWith(prefix)) {
                throw new IllegalArgumentException("Strict codegen rejected prose-prefixed output.");
            }
        }

        String format = normalizeFormatHint(formatHint, outputPath).toLowerCase(Locale.ROOT);
        if (format.equals("java")) {
            if (!(lower.contains("class ") || lower.contains("interface ") || lower.contains("enum "))) {
                throw new IllegalArgumentException("Strict codegen expected Java structure (class/interface/enum).");
            }
        } else if (format.equals("json")) {
            if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) {
                throw new IllegalArgumentException("Strict codegen expected JSON object/array output.");
            }
        }
    }

    private String normalizeFormatHint(String formatHint, String outputPath) {
        if (formatHint != null && !formatHint.isBlank()) {
            return formatHint.trim();
        }
        return inferLanguageFromPath(outputPath);
    }

    private record ExecutionContextResult(
        LlmCallService.LlmCallResult result,
        String scope,
        int memoriesUsed,
        boolean profileInjected,
        int injectionTokens,
        List<String> memoryLens,
        List<String> profileLens,
        String effectivePrompt
    ) {}

    private record PromptAssemblyResult(
        String prompt,
        int memoriesUsed,
        boolean profileInjected,
        int injectionTokens
    ) {}

    private record ModelExecutionResult(
        LlmModel model,
        ExecutionContextResult execution,
        int attempts
    ) {}

    private void printCallHelp() {
        System.out.println("Usage:");
        System.out.println("  nexus call run --user <username> --task <task> --prompt \"prompt text\" [--pin-intent true|false] [--confirm-intent true|false]");
        System.out.println("  Example: nexus call run --user admin --task CODE_GENERATION --prompt \"Write hello world in Python\"");
    }

    private void handleMemory(String[] args) {
        if (args.length == 0) {
            printMemoryHelp();
            return;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        Map<String, String> flags = parseFlags(Arrays.copyOfRange(args, 1, args.length));
        User user = authenticate(flags);

        switch (action) {
            case "store" -> memoryStore(user, flags);
            case "recall" -> memoryRecall(user, flags);
            case "list" -> memoryList(user, flags);
            case "timeline" -> memoryTimeline(user, flags);
            default -> printMemoryHelp();
        }
    }

    private void memoryStore(User user, Map<String, String> flags) {
        String content = require(flags, "--content", "Missing --content");
        String tags = flags.getOrDefault("--tags", "");
        String typeRaw = flags.getOrDefault("--type", "FACT");
        String scopeRaw = flags.getOrDefault("--scope", "project");
        boolean pinned = Boolean.parseBoolean(flags.getOrDefault("--pin", "false"));

        com.nexus.service.MemoryService memoryService = new com.nexus.service.MemoryService();
        com.nexus.domain.MemoryType type = parseMemoryType(typeRaw);
        int ttlDays = parseIntOrDefault(flags.get("--ttl"), type.getDefaultTtlDays());
        String scope = resolveMemoryScope(memoryService, flags, scopeRaw);

        com.nexus.domain.Memory memory;
        if (com.nexus.service.MemoryService.GLOBAL_SCOPE.equals(scope)) {
            memory = memoryService.storeGlobal(user.getId(), content, tags, type, ttlDays, pinned);
        } else {
            memory = memoryService.storeScoped(user.getId(), scope, content, tags, type, ttlDays, pinned);
        }
        
        TerminalUtils.printSuccess("Memory stored: #" + memory.getId());
        TerminalUtils.printInfo("Scope: " + scope + " | Type: " + type.name() + " | Pinned: " + (pinned ? "yes" : "no"));
    }

    private void memoryRecall(User user, Map<String, String> flags) {
        String query = require(flags, "--query", "Missing --query");
        String scopeRaw = flags.getOrDefault("--scope", "project");
        
        com.nexus.service.MemoryService memoryService = new com.nexus.service.MemoryService();
        String scope = resolveMemoryScope(memoryService, flags, scopeRaw);
        List<com.nexus.domain.Memory> memories = memoryService.recallForScope(user.getId(), scope, query);

        if (memories.isEmpty()) {
            TerminalUtils.printInfo("No memories found matching: " + query);
            return;
        }

        String[] headers = {"ID", "Type", "Confidence", "Content"};
        String[][] rows = new String[Math.min(10, memories.size())][4];
        for (int i = 0; i < rows.length; i++) {
            var m = memories.get(i);
            rows[i] = new String[]{
                String.valueOf(m.getId()),
                m.getType().name(),
                String.format("%.2f", m.getConfidence()),
                m.getContent().substring(0, Math.min(50, m.getContent().length())) + "..."
            };
        }
        TerminalUtils.printTable(headers, rows);
        TerminalUtils.printInfo("Scope: " + scope + " | Results: " + memories.size());
    }

    private void memoryList(User user, Map<String, String> flags) {
        com.nexus.service.MemoryService memoryService = new com.nexus.service.MemoryService();
        boolean all = Boolean.parseBoolean(flags.getOrDefault("--all", "false"));
        String scopeRaw = flags.getOrDefault("--scope", "project");
        String scope = resolveMemoryScope(memoryService, flags, scopeRaw);

        List<com.nexus.domain.Memory> memories = all
            ? memoryService.getAllMemories(user.getId())
            : memoryService.getByScope(user.getId(), scope);

        if (memories.isEmpty()) {
            TerminalUtils.printInfo("No memories found for selected scope.");
            return;
        }

        // Display newest first (chat summaries/memories are appended over time).
        memories = memories.stream()
            .sorted((a, b) -> {
                if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                if (a.getCreatedAt() == null) return 1;
                if (b.getCreatedAt() == null) return -1;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            })
            .toList();

        String[] headers = {"ID", "Type", "Confidence", "Scope", "Tags", "Content"};
        String[][] rows = new String[Math.min(10, memories.size())][6];
        for (int i = 0; i < rows.length; i++) {
            var m = memories.get(i);
            rows[i] = new String[]{
                String.valueOf(m.getId()),
                m.getType().name(),
                String.format("%.2f", m.getConfidence()),
                m.getAgentId() != null ? m.getAgentId() : "-",
                m.getTags() != null ? m.getTags() : "-",
                m.getContent().substring(0, Math.min(40, m.getContent().length())) + "..."
            };
        }
        TerminalUtils.printTable(headers, rows);
        TerminalUtils.printInfo((all ? "Showing all scopes" : "Scope: " + scope) + " | Showing newest " + rows.length + " of " + memories.size());
    }

    private void memoryTimeline(User user, Map<String, String> flags) {
        String query = flags.getOrDefault("--query", "");
        int limit = Math.max(1, parseIntOrDefault(flags.get("--limit"), 20));
        com.nexus.service.MemoryService memoryService = new com.nexus.service.MemoryService();
        String scope = resolveMemoryScope(memoryService, flags, flags.getOrDefault("--scope", "project"));

        List<Memory> memories = memoryService.recallForScope(user.getId(), scope, query);
        if (memories.isEmpty()) {
            TerminalUtils.printInfo("No memories found for timeline.");
            return;
        }

        memories = memories.stream().limit(limit).toList();
        String[] headers = {"When", "ID", "Type", "Why Used", "Preview"};
        String[][] rows = new String[memories.size()][5];
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm");

        for (int i = 0; i < memories.size(); i++) {
            Memory m = memories.get(i);
            String tags = m.getTags() == null ? "" : m.getTags().toLowerCase(Locale.ROOT);
            String why = m.getAgentId() != null && m.getAgentId().equals(scope)
                ? "scope-match"
                : "global-fallback";
            if (tags.contains("pinned:true")) {
                why += ", pinned";
            }
            if (!query.isBlank()) {
                String queryLower = query.toLowerCase(Locale.ROOT);
                if (m.getContent().toLowerCase(Locale.ROOT).contains(queryLower)
                    || tags.contains(queryLower)) {
                    why += ", query-hit";
                }
            }

            rows[i] = new String[] {
                m.getCreatedAt() == null ? "-" : m.getCreatedAt().format(formatter),
                String.valueOf(m.getId()),
                m.getType().name(),
                why,
                truncate(m.getContent(), 65)
            };
        }

        TerminalUtils.printSeparator("MEMORY TIMELINE");
        TerminalUtils.printTable(headers, rows);
        TerminalUtils.printInfo("Scope: " + scope + " | Query: " + (query.isBlank() ? "<none>" : query));
    }

    private void printMemoryHelp() {
        System.out.println("Usage:");
        System.out.println("  nexus memory store --user <username> --content \"memory content\" [--tags \"tag1,tag2\"] [--type FACT|PREFERENCE|EPISODE|SKILL] [--scope project|global|<scope>] [--project <path>] [--ttl <days>] [--pin]");
        System.out.println("  nexus memory recall --user <username> --query \"search keywords\" [--scope project|global|<scope>] [--project <path>]");
        System.out.println("  nexus memory list --user <username> [--scope project|global|<scope>] [--project <path>] [--all]");
        System.out.println("  nexus memory timeline --user <username> --query \"topic\" [--scope project|global|<scope>] [--limit 20]");
    }

    private void handleProfile(String[] args) {
        if (args.length == 0) {
            printProfileHelp();
            return;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        Map<String, String> flags = parseFlags(Arrays.copyOfRange(args, 1, args.length));
        User user = authenticate(flags);

        switch (action) {
            case "set" -> profileSet(user, flags);
            case "list" -> profileList(user, flags);
            case "delete" -> profileDelete(user, flags);
            case "preset" -> profilePreset(user, flags);
            case "doctor" -> profileDoctor(user, flags);
            case "wizard" -> profileWizard(user, flags);
            default -> printProfileHelp();
        }
    }

    private void handleCodegen(String[] args) {
        if (args.length == 0) {
            printCodegenHelp();
            return;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        Map<String, String> flags = parseFlags(Arrays.copyOfRange(args, 1, args.length));
        User user = authenticate(flags);

        switch (action) {
            case "run" -> codegenRun(user, flags);
            default -> printCodegenHelp();
        }
    }

    private void codegenRun(User user, Map<String, String> flags) {
        String prompt = require(flags, "--prompt", "Missing --prompt");
        String output = require(flags, "--output", "Missing --output file path");
        TaskType task = parseTask(flags.getOrDefault("--task", TaskType.CODE_GENERATION.name()));
        boolean overwrite = parseBooleanFlag(flags, "--overwrite", false);
        boolean remember = parseBooleanFlag(flags, "--remember", true);
        boolean strictCode = parseBooleanFlag(flags, "--strict-code", true);
        String formatHint = normalizeFormatHint(flags.getOrDefault("--format", ""), output);
        if (formatHint.isBlank()) {
            String entered = readLine("Unable to infer output language from file extension. Enter --format (e.g. java, python, rust, react-ts): ");
            formatHint = entered == null ? "" : entered.trim();
        }
        if (formatHint.isBlank()) {
            throw new IllegalArgumentException("Missing output language. Pass --format <language> when extension is ambiguous.");
        }
        boolean pinIntent = parseBooleanFlag(flags, "--pin-intent", false);
        boolean confirmIntent = parseBooleanFlag(flags, "--confirm-intent", false);

        String scope = profileService.currentWorkspaceScope();
        if (!profileService.isActionAllowed(user.getId(), scope, "policy.allow_file_write")) {
            throw new IllegalArgumentException("File writes are blocked by profile policy (policy.allow_file_write=false).");
        }

        String activeIntent = ensureWorkspaceIntent(user, scope, task, prompt, pinIntent);

        LlmModel model = resolveModelForTask(user, task, flags);
        if (model == null) {
            throw new IllegalArgumentException("No model available for code generation task.");
        }

        TerminalUtils.printSeparator("CODE GENERATION");
        TerminalUtils.printInfo("Trace ID: " + activeTraceId);
        TerminalUtils.printInfo("Model: " + model.getName() + " (" + model.getProvider() + ")");

        try {
            String codegenPrompt = buildCodegenPrompt(prompt, output, formatHint, strictCode);
            ModelExecutionResult modelExecution = executeWithFallback(user, task, codegenPrompt, model);
            ExecutionContextResult execution = modelExecution.execution();
            String generatedContent = sanitizeGeneratedOutput(execution.result().content());
            validateGeneratedContent(generatedContent, output, formatHint, strictCode);
            int alignmentPercent = computeIntentAlignmentPercent(activeIntent, generatedContent);

            boolean autoCorrected = false;
            int minAlignment = Math.max(5, profileService.getIntSetting(user.getId(), scope, "intent.min_alignment_percent", 12));
            boolean autoCorrectEnabled = profileService.getBooleanSetting(user.getId(), scope, "intent.auto_correct_on_drift", true);
            if (!confirmIntent && autoCorrectEnabled && alignmentPercent < minAlignment) {
                TerminalUtils.printWarn("Intent Guard 2.0 triggered. Attempting corrective regeneration before file write...");
                String correctivePrompt = buildIntentCorrectionPrompt(activeIntent, generatedContent, output, formatHint);
                ModelExecutionResult corrective = executeWithFallback(user, task, correctivePrompt, modelExecution.model());
                String corrected = sanitizeGeneratedOutput(corrective.execution().result().content());
                validateGeneratedContent(corrected, output, formatHint, strictCode);
                int correctedAlignment = computeIntentAlignmentPercent(activeIntent, corrected);
                if (correctedAlignment > alignmentPercent) {
                    generatedContent = corrected;
                    alignmentPercent = correctedAlignment;
                    autoCorrected = true;
                }
            }

            enforceIntentAlignment(user, scope, activeIntent, alignmentPercent, confirmIntent);
            writeGeneratedToFile(user, scope, output, generatedContent, overwrite);

            if (remember) {
                com.nexus.service.MemoryService memoryService = new com.nexus.service.MemoryService();
                rememberGeneratedArtifactInChunks(user, scope, memoryService, output, generatedContent, task);
            }

            int confidence = estimateCodegenConfidence(
                strictCode,
                alignmentPercent,
                !execution.result().simulated(),
                execution.injectionTokens()
            );

            TerminalUtils.printSuccess("Code generated and saved to: " + output);
            TerminalUtils.printInfo("Model used: " + modelExecution.model().getName() + " (attempts=" + modelExecution.attempts() + ")");
            TerminalUtils.printInfo("Context memories used: " + execution.memoriesUsed());
            TerminalUtils.printInfo("Context injection tokens: " + execution.injectionTokens());
            if (execution.memoryLens() != null && !execution.memoryLens().isEmpty()) {
                TerminalUtils.printInfo("Context lens memory: " + String.join(" | ", execution.memoryLens()));
            }
            if (execution.profileLens() != null && !execution.profileLens().isEmpty()) {
                TerminalUtils.printInfo("Context lens profile keys: " + String.join(", ", execution.profileLens()));
            }
            TerminalUtils.printInfo("Intent alignment: " + alignmentPercent + "%");
            if (autoCorrected) {
                TerminalUtils.printInfo("Intent Guard 2.0: applied corrective regeneration before write.");
            }
            TerminalUtils.printInfo("Edit confidence: " + confidence + "% " + TerminalUtils.progressBar(confidence, 100, 14));
        } catch (Exception e) {
            throw new RuntimeException("Code generation failed: " + e.getMessage(), e);
        }
    }

    private void rememberGeneratedArtifactInChunks(
        User user,
        String scope,
        com.nexus.service.MemoryService memoryService,
        String output,
        String content,
        TaskType task
    ) {
        String summary = "Generated file " + output + " using task " + task.name();
        memoryService.storeScoped(user.getId(), scope, summary, "codegen,output:" + output, MemoryType.EPISODE, 365, true);

        int chunkChars = Math.max(400, profileService.getIntSetting(user.getId(), scope, "memory.codegen_chunk_chars", 900));
        int maxChunks = Math.max(1, profileService.getIntSetting(user.getId(), scope, "memory.codegen_max_chunks", 4));
        String safeContent = content == null ? "" : content;

        int totalLength = safeContent.length();
        if (totalLength == 0) {
            return;
        }

        int stored = 0;
        for (int i = 0; i < maxChunks && stored < totalLength; i++) {
            int end = Math.min(totalLength, stored + chunkChars);
            String chunk = safeContent.substring(stored, end);
            String chunkNote = "Codegen chunk " + (i + 1) + " for " + output + ":\n" + chunk;
            memoryService.storeScoped(
                user.getId(),
                scope,
                chunkNote,
                "codegen,chunk,output:" + output,
                MemoryType.FACT,
                90,
                false
            );
            stored = end;
        }
    }

    private void handleRecipe(String[] args) {
        if (args.length == 0) {
            printRecipeHelp();
            return;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        Map<String, String> flags = parseFlags(Arrays.copyOfRange(args, 1, args.length));
        User user = authenticate(flags);

        switch (action) {
            case "run" -> recipeRun(user, flags);
            case "validate" -> recipeValidate(user, flags);
            case "template" -> printRecipeTemplate();
            case "marketplace" -> recipeMarketplace(user, flags);
            default -> printRecipeHelp();
        }
    }

    private void recipeRun(User user, Map<String, String> flags) {
        String fileRaw = require(flags, "--file", "Missing --file recipe path");
        Path recipePath = resolvePath(fileRaw);
        String scope = profileService.currentWorkspaceScope();

        if (!profileService.isActionAllowed(user.getId(), scope, "policy.allow_recipe_run")) {
            throw new IllegalArgumentException("Recipe execution blocked by profile policy (policy.allow_recipe_run=false).");
        }

        if (!Files.exists(recipePath)) {
            throw new IllegalArgumentException("Recipe file not found: " + recipePath);
        }

        List<String> issues = validateRecipeFile(recipePath);
        if (!issues.isEmpty()) {
            throw new IllegalArgumentException("Recipe validation failed: " + issues.get(0));
        }

        TerminalUtils.printSeparator("RECIPE RUNNER");
        TerminalUtils.printInfo("Trace ID: " + activeTraceId);
        TerminalUtils.printInfo("Recipe file: " + recipePath);

        try {
            List<String> lines = Files.readAllLines(recipePath, StandardCharsets.UTF_8);
            int executed = 0;

            for (String raw : lines) {
                String line = raw == null ? "" : raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                executeRecipeLine(user, line);
                executed++;
            }

            TerminalUtils.printSuccess("Recipe completed. Steps executed: " + executed);
        } catch (IOException e) {
            throw new RuntimeException("Could not read recipe file: " + e.getMessage(), e);
        }
    }

    private void recipeValidate(User user, Map<String, String> flags) {
        String fileRaw = require(flags, "--file", "Missing --file recipe path");
        Path recipePath = resolvePath(fileRaw);
        if (!Files.exists(recipePath)) {
            throw new IllegalArgumentException("Recipe file not found: " + recipePath);
        }

        TerminalUtils.printSeparator("RECIPE VALIDATION");
        TerminalUtils.printInfo("Trace ID: " + activeTraceId);
        TerminalUtils.printInfo("User: " + user.getUsername());
        TerminalUtils.printInfo("Recipe file: " + recipePath);

        List<String> issues = validateRecipeFile(recipePath);
        if (issues.isEmpty()) {
            TerminalUtils.printSuccess("Recipe validation passed.");
            return;
        }

        for (String issue : issues) {
            TerminalUtils.printError(issue);
        }
        throw new IllegalArgumentException("Recipe validation failed with " + issues.size() + " issue(s).");
    }

    private List<String> validateRecipeFile(Path recipePath) {
        List<String> issues = new ArrayList<>();
        List<String> lines;
        try {
            lines = Files.readAllLines(recipePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            issues.add("Could not read recipe file: " + e.getMessage());
            return issues;
        }

        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            String line = raw == null ? "" : raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] parts = line.split("\\|", -1);
            String prefix = "Line " + (i + 1) + ": ";
            if (parts.length == 0 || parts[0].trim().isEmpty()) {
                issues.add(prefix + "Missing recipe step kind.");
                continue;
            }

            String kind = parts[0].trim().toUpperCase(Locale.ROOT);
            try {
                switch (kind) {
                    case "PROFILE" -> {
                        if (parts.length < 4) throw new IllegalArgumentException("PROFILE requires scope|key|value");
                        if (parts[2].trim().isEmpty()) throw new IllegalArgumentException("PROFILE key cannot be empty");
                    }
                    case "MEMORY" -> {
                        if (parts.length < 7) throw new IllegalArgumentException("MEMORY requires scope|type|ttl|pin|tags|content");
                        parseMemoryType(parts[2]);
                        int ttl = parseIntOrDefault(parts[3], -1);
                        if (ttl <= 0) throw new IllegalArgumentException("MEMORY ttl must be > 0");
                    }
                    case "CALL" -> {
                        if (parts.length < 3) throw new IllegalArgumentException("CALL requires task|prompt");
                        parseTask(parts[1]);
                    }
                    case "CODEGEN" -> {
                        if (parts.length < 5) throw new IllegalArgumentException("CODEGEN requires task|output|overwrite|prompt");
                        parseTask(parts[1]);
                        if (parts[2].trim().isEmpty()) throw new IllegalArgumentException("CODEGEN output path cannot be empty");
                    }
                    case "TOOL" -> {
                        if (parts.length < 3) throw new IllegalArgumentException("TOOL requires tool-name|k=v;k2=v2");
                        String toolName = parts[1].trim().toLowerCase(Locale.ROOT);
                        Map<String, String> params = parseKeyValueParams(parts[2]);
                        if (toolName.isEmpty()) throw new IllegalArgumentException("TOOL name cannot be empty");
                        if ((toolName.equals("fs.read") || toolName.equals("fs.write")) && !params.containsKey("path")) {
                            throw new IllegalArgumentException(toolName + " requires path parameter");
                        }
                        if (toolName.equals("shell.exec") && !params.containsKey("command")) {
                            throw new IllegalArgumentException("shell.exec requires command parameter");
                        }
                    }
                    default -> throw new IllegalArgumentException("Unsupported recipe step: " + kind);
                }
            } catch (Exception e) {
                issues.add(prefix + e.getMessage());
            }
        }

        return issues;
    }

    private void executeRecipeLine(User user, String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length == 0) return;

        String kind = parts[0].trim().toUpperCase(Locale.ROOT);
        switch (kind) {
            case "PROFILE" -> executeRecipeProfile(user, parts);
            case "MEMORY" -> executeRecipeMemory(user, parts);
            case "CALL" -> executeRecipeCall(user, parts);
            case "CODEGEN" -> executeRecipeCodegen(user, parts);
            case "TOOL" -> executeRecipeTool(user, parts);
            default -> throw new IllegalArgumentException("Unsupported recipe step: " + kind);
        }
    }

    private void executeRecipeProfile(User user, String[] parts) {
        if (parts.length < 4) {
            throw new IllegalArgumentException("PROFILE step needs: PROFILE|scope|key|value");
        }
        String scope = resolveRecipeScope(parts[1]);
        String key = parts[2].trim();
        String value = joinParts(parts, 3);
        profileService.setSetting(user.getId(), scope, key, value);
        TerminalUtils.printInfo("PROFILE set: " + key + " (scope=" + scope + ")");
    }

    private void executeRecipeMemory(User user, String[] parts) {
        if (parts.length < 7) {
            throw new IllegalArgumentException("MEMORY step needs: MEMORY|scope|type|ttl|pin|tags|content");
        }
        String scope = resolveRecipeScope(parts[1]);
        MemoryType type = parseMemoryType(parts[2]);
        int ttl = parseIntOrDefault(parts[3], type.getDefaultTtlDays());
        boolean pin = parseBooleanToken(parts[4]);
        String tags = parts[5];
        String content = joinParts(parts, 6);

        com.nexus.service.MemoryService memoryService = new com.nexus.service.MemoryService();
        memoryService.storeScoped(user.getId(), scope, content, tags, type, ttl, pin);
        TerminalUtils.printInfo("MEMORY stored (scope=" + scope + ", type=" + type.name() + ")");
    }

    private void executeRecipeCall(User user, String[] parts) {
        if (parts.length < 3) {
            throw new IllegalArgumentException("CALL step needs: CALL|task|prompt");
        }
        TaskType task = parseTask(parts[1]);
        String prompt = joinParts(parts, 2);
        LlmModel model = routingEngine.selectOptimalModelForUser(task, Double.MAX_VALUE, user.getId());
        if (model == null) {
            throw new IllegalArgumentException("No model available for CALL task " + task);
        }
        try {
            ModelExecutionResult modelExecution = executeWithFallback(user, task, prompt, model);
            ExecutionContextResult execution = modelExecution.execution();
            TerminalUtils.printInfo("CALL completed via " + modelExecution.model().getName() + " | latency=" + execution.result().latencyMs() + "ms");
        } catch (Exception e) {
            throw new RuntimeException("CALL step failed: " + e.getMessage(), e);
        }
    }

    private void executeRecipeCodegen(User user, String[] parts) {
        if (parts.length < 5) {
            throw new IllegalArgumentException("CODEGEN step needs: CODEGEN|task|output|overwrite|prompt");
        }

        TaskType task = parseTask(parts[1]);
        String output = parts[2].trim();
        boolean overwrite = parseBooleanToken(parts[3]);
        String prompt = joinParts(parts, 4);

        Map<String, String> flags = new LinkedHashMap<>();
        flags.put("--task", task.name());
        flags.put("--prompt", prompt);
        flags.put("--output", output);
        flags.put("--overwrite", String.valueOf(overwrite));
        flags.put("--remember", "true");
        flags.put("--strict-code", "true");
        codegenRun(user, flags);
    }

    private void executeRecipeTool(User user, String[] parts) {
        if (parts.length < 3) {
            throw new IllegalArgumentException("TOOL step needs: TOOL|tool-name|k=v;k2=v2");
        }
        String toolName = parts[1].trim();
        Map<String, String> params = parseKeyValueParams(parts[2]);
        String scope = profileService.currentWorkspaceScope();

        ToolExecutionService.ToolPolicy policy = buildToolPolicy(user, scope);
        ToolExecutionService.ToolResult result = toolExecutionService.execute(toolName, params, policy);

        if (!result.success()) {
            throw new RuntimeException("TOOL step failed (" + toolName + "): " + result.output());
        }
        TerminalUtils.printInfo("TOOL executed: " + toolName);
    }

    private void printCodegenHelp() {
        System.out.println("Usage:");
        System.out.println("  nexus codegen run --user <username> --prompt \"task\" --output <path> [--task CODE_GENERATION] [--model model-name] [--overwrite] [--remember true|false] [--strict-code true|false] [--format java|python|typescript|...] [--pin-intent true|false] [--confirm-intent true|false]");
        System.out.println("  Note: --strict-code defaults to true unless explicitly set false.");
        System.out.println("Policy keys:");
        System.out.println("  policy.allow_file_write = true|false");
    }

    private void printRecipeHelp() {
        System.out.println("Usage:");
        System.out.println("  nexus recipe run --user <username> --file <recipe-file>");
        System.out.println("  nexus recipe validate --user <username> --file <recipe-file>");
        System.out.println("  nexus recipe template");
        System.out.println("  nexus recipe marketplace --user <username> --action list|show|install [--name frontend-landing|backend-rust|backend-python|release-notes]");
        System.out.println("Policy keys:");
        System.out.println("  policy.allow_recipe_run = true|false");
    }

    private void printRecipeTemplate() {
        System.out.println("# Nexus Recipe DSL");
        System.out.println("# PROFILE|scope|key|value");
        System.out.println("# MEMORY|scope|type|ttl|pin|tags|content");
        System.out.println("# CALL|task|prompt");
        System.out.println("# CODEGEN|task|output|overwrite|prompt");
        System.out.println("# TOOL|tool-name|k=v;k2=v2");
        System.out.println();
        System.out.println("PROFILE|global|response_tone|Concise and action-oriented");
        System.out.println("MEMORY|project|SKILL|365|true|architecture,style|Use service-layer validation for all user input");
        System.out.println("CALL|GENERAL_CHAT|Summarize today priorities in 3 bullets");
        System.out.println("CODEGEN|CODE_GENERATION|src/main/java/com/nexus/service/SampleService.java|false|Create a SampleService class with one method returning \"ok\"");
        System.out.println("TOOL|fs.read|path=README.md;maxchars=500");
    }

    private void recipeMarketplace(User user, Map<String, String> flags) {
        String action = flags.getOrDefault("--action", "list").trim().toLowerCase(Locale.ROOT);
        String name = flags.getOrDefault("--name", "").trim().toLowerCase(Locale.ROOT);

        Map<String, String> catalog = new LinkedHashMap<>();
        catalog.put("frontend-landing", "Scaffold a focused frontend landing deliverable flow");
        catalog.put("backend-rust", "Generate Rust backend handler + smoke validation");
        catalog.put("backend-python", "Generate Python backend endpoint + tests checklist");
        catalog.put("release-notes", "Build release notes summary from memory and history");

        switch (action) {
            case "list" -> {
                String[] headers = {"Recipe", "Purpose"};
                String[][] rows = new String[catalog.size()][2];
                int i = 0;
                for (var entry : catalog.entrySet()) {
                    rows[i++] = new String[] {entry.getKey(), entry.getValue()};
                }
                TerminalUtils.printSeparator("RECIPE MARKETPLACE");
                TerminalUtils.printInfo("User: " + user.getUsername());
                TerminalUtils.printTable(headers, rows);
            }
            case "show" -> {
                if (!catalog.containsKey(name)) {
                    throw new IllegalArgumentException("Unknown marketplace recipe: " + name);
                }
                String template = marketplaceRecipeTemplate(name);
                TerminalUtils.printBox("MARKETPLACE · " + name, template);
            }
            case "install" -> {
                if (!catalog.containsKey(name)) {
                    throw new IllegalArgumentException("Unknown marketplace recipe: " + name);
                }
                try {
                    Path recipesDir = resolvePath("recipes");
                    if (!Files.exists(recipesDir)) Files.createDirectories(recipesDir);
                    Path target = recipesDir.resolve(name + ".recipe");
                    if (Files.exists(target)) {
                        throw new IllegalArgumentException("Recipe already exists: " + target);
                    }
                    Files.writeString(target, marketplaceRecipeTemplate(name), StandardCharsets.UTF_8);
                    TerminalUtils.printSuccess("Installed marketplace recipe: " + target);
                } catch (IOException ioe) {
                    throw new IllegalArgumentException("Failed to install marketplace recipe: " + ioe.getMessage());
                }
            }
            default -> throw new IllegalArgumentException("Unsupported --action. Use list|show|install");
        }
    }

    private String marketplaceRecipeTemplate(String name) {
        return switch (name) {
            case "frontend-landing" -> String.join("\n",
                "# Marketplace: frontend-landing",
                "PROFILE|project|intent.active|CODE_GENERATION: Build modern landing page in React",
                "PROFILE|project|code_style|Prefer accessible components and clear section structure",
                "CODEGEN|CODE_GENERATION|site/src/pages/LandingPage.jsx|true|Create a responsive landing page with hero, value props, CTA, and footer.",
                "TOOL|fs.read|path=site/src/pages/LandingPage.jsx;maxchars=900"
            ) + "\n";
            case "backend-rust" -> String.join("\n",
                "# Marketplace: backend-rust",
                "PROFILE|project|intent.active|CODE_GENERATION: Build Rust backend endpoint",
                "CODEGEN|CODE_GENERATION|src/main/rust/handler.rs|true|Create Rust handler function for health/status endpoint with serde JSON response.",
                "TOOL|fs.read|path=src/main/rust/handler.rs;maxchars=900"
            ) + "\n";
            case "backend-python" -> String.join("\n",
                "# Marketplace: backend-python",
                "PROFILE|project|intent.active|CODE_GENERATION: Build Python backend endpoint",
                "CODEGEN|CODE_GENERATION|src/main/python/app.py|true|Create a FastAPI app with /health endpoint and structured response.",
                "TOOL|fs.read|path=src/main/python/app.py;maxchars=900"
            ) + "\n";
            case "release-notes" -> String.join("\n",
                "# Marketplace: release-notes",
                "CALL|SUMMARIZATION|Generate release notes in markdown with sections: Added, Improved, Fixed, Risks.",
                "MEMORY|project|EPISODE|30|true|release,notes|Release notes generated from latest workspace context"
            ) + "\n";
            default -> throw new IllegalArgumentException("No template available for: " + name);
        };
    }

    private void handleTool(String[] args) {
        if (args.length == 0) {
            printToolHelp();
            return;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        Map<String, String> flags = parseFlags(Arrays.copyOfRange(args, 1, args.length));
        User user = authenticate(flags);

        switch (action) {
            case "list" -> toolList();
            case "run" -> toolRun(user, flags);
            default -> printToolHelp();
        }
    }

    private void handleProvider(String[] args) {
        if (args.length == 0) {
            printProviderHelp();
            return;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        Map<String, String> flags = parseFlags(Arrays.copyOfRange(args, 1, args.length));
        User user = authenticate(flags);

        switch (action) {
            case "list" -> providerList(user);
            case "check" -> providerCheck(user, flags);
            case "setup", "key" -> providerSetup(user, flags);
            default -> printProviderHelp();
        }
    }

    private void handlePolicy(String[] args) {
        if (args.length == 0) {
            printPolicyHelp();
            return;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        Map<String, String> flags = parseFlags(Arrays.copyOfRange(args, 1, args.length));
        User user = authenticate(flags);

        switch (action) {
            case "simulate" -> policySimulate(user, flags);
            default -> printPolicyHelp();
        }
    }

    private void policySimulate(User user, Map<String, String> flags) {
        String command = require(flags, "--command", "Missing --command for policy simulation");
        List<String> tokens = tokenizeCommand(command);
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Policy simulation requires a non-empty --command value.");
        }

        if (tokens.get(0).equalsIgnoreCase("nexus")) {
            tokens = new ArrayList<>(tokens.subList(1, tokens.size()));
        }
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Policy simulation command did not contain a Nexus action.");
        }

        String scope = profileService.currentWorkspaceScope();
        String top = tokens.get(0).toLowerCase(Locale.ROOT);
        List<PolicyDecision> decisions = new ArrayList<>();

        switch (top) {
            case "codegen" -> {
                Map<String, String> cmdFlags = parseFlags(tokens.subList(1, tokens.size()).toArray(String[]::new));
                boolean allowWrite = profileService.isActionAllowed(user.getId(), scope, "policy.allow_file_write");
                decisions.add(new PolicyDecision("policy.allow_file_write", allowWrite,
                    allowWrite ? "Output file writes allowed." : "Output file writes blocked."));

                String output = cmdFlags.getOrDefault("--output", "");
                if (!output.isBlank()) {
                    boolean external = isExternalPath(output);
                    boolean allowExternal = profileService.getBooleanSetting(user.getId(), scope, "policy.allow_external_write", false);
                    if (external) {
                        decisions.add(new PolicyDecision("policy.allow_external_write", allowExternal,
                            allowExternal ? "External output path allowed." : "External output path blocked."));
                    }
                }
            }
            case "recipe" -> {
                boolean allowRecipe = profileService.isActionAllowed(user.getId(), scope, "policy.allow_recipe_run");
                decisions.add(new PolicyDecision("policy.allow_recipe_run", allowRecipe,
                    allowRecipe ? "Recipe execution allowed." : "Recipe execution blocked."));
            }
            case "tool" -> {
                Map<String, String> cmdFlags = parseFlags(tokens.subList(1, tokens.size()).toArray(String[]::new));
                String toolName = cmdFlags.getOrDefault("--name", "").toLowerCase(Locale.ROOT);
                if (toolName.isBlank()) {
                    decisions.add(new PolicyDecision("tool.name", false, "Tool simulation requires --name."));
                } else if (toolName.equals("fs.read")) {
                    boolean allowed = profileService.getBooleanSetting(user.getId(), scope, "policy.allow_tool_fs_read", true);
                    decisions.add(new PolicyDecision("policy.allow_tool_fs_read", allowed,
                        allowed ? "Tool fs.read allowed." : "Tool fs.read blocked."));
                } else if (toolName.equals("fs.write")) {
                    boolean allowed = profileService.getBooleanSetting(user.getId(), scope, "policy.allow_tool_fs_write", true)
                        && profileService.isActionAllowed(user.getId(), scope, "policy.allow_file_write");
                    decisions.add(new PolicyDecision("policy.allow_tool_fs_write", allowed,
                        allowed ? "Tool fs.write allowed." : "Tool fs.write blocked."));
                } else if (toolName.equals("shell.exec")) {
                    boolean allowed = profileService.getBooleanSetting(user.getId(), scope, "policy.allow_tool_shell", false);
                    decisions.add(new PolicyDecision("policy.allow_tool_shell", allowed,
                        allowed ? "Tool shell.exec allowed." : "Tool shell.exec blocked."));
                } else {
                    decisions.add(new PolicyDecision("tool.policy", true, "No explicit policy check mapped for tool: " + toolName));
                }
            }
            default -> decisions.add(new PolicyDecision(
                "command.policy",
                true,
                "No specific policy gates mapped for top-level command: " + top
            ));
        }

        boolean allAllowed = decisions.stream().allMatch(PolicyDecision::allowed);
        String[] headers = {"Policy Gate", "Decision", "Details"};
        String[][] rows = new String[decisions.size()][3];
        for (int i = 0; i < decisions.size(); i++) {
            PolicyDecision d = decisions.get(i);
            rows[i] = new String[] {
                d.gate(),
                d.allowed() ? TerminalUtils.GREEN + "ALLOW" + TerminalUtils.RESET : TerminalUtils.RED + "DENY" + TerminalUtils.RESET,
                d.details()
            };
        }
        TerminalUtils.printTable(headers, rows);
        TerminalUtils.printInfo("Simulated command: " + command);
        if (allAllowed) {
            TerminalUtils.printSuccess("Policy simulation verdict: ALLOW");
        } else {
            TerminalUtils.printWarn("Policy simulation verdict: DENY");
        }
    }

    private boolean isExternalPath(String rawPath) {
        Path resolved = resolvePath(rawPath).toAbsolutePath().normalize();
        Path workspace = Paths.get("").toAbsolutePath().normalize();
        return !resolved.startsWith(workspace);
    }

    private List<String> tokenizeCommand(String raw) {
        List<String> tokens = new ArrayList<>();
        if (raw == null || raw.isBlank()) return tokens;

        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        char quoteChar = '\0';

        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if ((c == '"' || c == '\'') ) {
                if (inQuote && c == quoteChar) {
                    inQuote = false;
                    quoteChar = '\0';
                } else if (!inQuote) {
                    inQuote = true;
                    quoteChar = c;
                } else {
                    current.append(c);
                }
                continue;
            }

            if (Character.isWhitespace(c) && !inQuote) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }

            current.append(c);
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private record PolicyDecision(String gate, boolean allowed, String details) {}

    private record OnboardStepResult(String step, boolean ok, String detail) {}

    private record ProfileHealthSummary(int riskCount, int heavyCount, int totalChecks) {}

    private record TrustEnvelope(int score, String level, List<String> factors) {}

    private void handleSmoke(String[] args) {
        if (args.length == 0) {
            printSmokeHelp();
            return;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        Map<String, String> flags = parseFlags(Arrays.copyOfRange(args, 1, args.length));
        User user = authenticate(flags);

        switch (action) {
            case "run" -> smokeRun(user, flags);
            default -> printSmokeHelp();
        }
    }

    private void handleOnboard(String[] args) {
        String[] effective = args;
        if (args.length > 0 && !args[0].startsWith("--")) {
            if (!args[0].equalsIgnoreCase("run")) {
                printOnboardHelp();
                return;
            }
            effective = Arrays.copyOfRange(args, 1, args.length);
        }

        Map<String, String> flags = parseFlags(effective);
        User user = authenticate(flags);
        onboardRun(user, flags);
    }

    private void handleStoryboard(String[] args) {
        String action = args.length == 0 ? "show" : args[0].toLowerCase(Locale.ROOT);
        String[] effective = args.length == 0 ? new String[0] : Arrays.copyOfRange(args, 1, args.length);
        Map<String, String> flags = parseFlags(effective);
        User user = authenticate(flags);

        switch (action) {
            case "show", "replay" -> storyboardShow(user, flags);
            default -> printStoryboardHelp();
        }
    }

    private void handleTrust(String[] args) {
        if (args.length == 0) {
            printTrustHelp();
            return;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        Map<String, String> flags = parseFlags(Arrays.copyOfRange(args, 1, args.length));
        User user = authenticate(flags);

        switch (action) {
            case "evaluate" -> trustEvaluate(user, flags);
            default -> printTrustHelp();
        }
    }

    private void handleWorkflow(String[] args) {
        if (args.length == 0) {
            printWorkflowHelp();
            return;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        Map<String, String> flags = parseFlags(Arrays.copyOfRange(args, 1, args.length));
        User user = authenticate(flags);

        switch (action) {
            case "list" -> workflowList();
            case "run" -> workflowRun(user, flags);
            default -> printWorkflowHelp();
        }
    }

    private void handlePr(String[] args) {
        if (args.length == 0) {
            printPrHelp();
            return;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        Map<String, String> flags = parseFlags(Arrays.copyOfRange(args, 1, args.length));
        User user = authenticate(flags);

        switch (action) {
            case "prep" -> prPrep(user, flags);
            default -> printPrHelp();
        }
    }

    private void handleSuggest(String[] args) {
        Map<String, String> flags = parseFlags(args);
        User user = authenticate(flags);
        suggestCommands(user, flags);
    }

    private void storyboardShow(User user, Map<String, String> flags) {
        int limit = Math.max(5, parseIntOrDefault(flags.get("--limit"), 25));
        List<AuditLog> logs = auditLogDao.findByUserId(user.getId());
        List<OutcomeMemory> outcomes = outcomeDao.findByUserId(user.getId());

        String[] headers = {"When", "Lane", "Event", "Detail"};
        List<String[]> rows = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd HH:mm");

        for (AuditLog log : logs.stream().limit(limit).toList()) {
            rows.add(new String[] {
                log.getCreatedAt() == null ? "-" : log.getCreatedAt().format(fmt),
                "audit",
                log.getAction(),
                truncate(log.getDetails(), 75)
            });
        }

        for (OutcomeMemory outcome : outcomes.stream().limit(Math.max(5, limit / 2)).toList()) {
            rows.add(new String[] {
                outcome.getCreatedAt() == null ? "-" : outcome.getCreatedAt().format(fmt),
                "outcome",
                outcome.getTaskType().name(),
                "model=" + outcome.getModelId() + ", quality=" + String.format("%.2f", outcome.getQualityScore())
            });
        }

        rows.sort((a, b) -> b[0].compareTo(a[0]));
        List<String[]> clipped = rows.stream().limit(limit).toList();
        TerminalUtils.printSeparator("NEXUS STORYBOARD");
        TerminalUtils.printTable(headers, clipped.toArray(String[][]::new));
        TerminalUtils.printInfo("Replay includes audit + outcome lanes for traceability.");
    }

    private void trustEvaluate(User user, Map<String, String> flags) {
        String command = require(flags, "--command", "Missing --command for trust evaluation");
        TrustEnvelope envelope = computeTrustEnvelope(user, command);

        String[] headers = {"Factor", "Impact"};
        String[][] rows = new String[envelope.factors().size()][2];
        for (int i = 0; i < envelope.factors().size(); i++) {
            rows[i] = new String[] {"f" + (i + 1), envelope.factors().get(i)};
        }
        TerminalUtils.printSeparator("TRUST ENVELOPE");
        TerminalUtils.printTable(headers, rows);
        TerminalUtils.printInfo("Command: " + command);
        TerminalUtils.printInfo("Trust score: " + envelope.score() + "% " + TerminalUtils.progressBar(envelope.score(), 100, 16));
        TerminalUtils.printInfo("Level: " + envelope.level());
    }

    private TrustEnvelope computeTrustEnvelope(User user, String command) {
        int score = 100;
        List<String> factors = new ArrayList<>();
        String scope = profileService.currentWorkspaceScope();
        String commandLower = command == null ? "" : command.toLowerCase(Locale.ROOT);

        boolean shellAllowed = profileService.getBooleanSetting(user.getId(), scope, "policy.allow_tool_shell", false);
        if (shellAllowed) {
            score -= 10;
            factors.add("-10 shell tool policy enabled");
        }

        if (!profileService.getBooleanSetting(user.getId(), scope, "intent.require_confirmation_on_drift", true)) {
            score -= 15;
            factors.add("-15 intent drift confirmation disabled");
        } else {
            factors.add("+0 intent drift confirmation enabled");
        }

        int fallbackCandidates = profileService.getIntSetting(user.getId(), scope, "routing.max_fallback_candidates", 3);
        if (fallbackCandidates < 2) {
            score -= 10;
            factors.add("-10 fallback candidates below recommended minimum");
        }

        if (commandLower.contains("codegen") && !profileService.isActionAllowed(user.getId(), scope, "policy.allow_file_write")) {
            score -= 35;
            factors.add("-35 file-write policy denies code generation");
        }

        if (commandLower.contains("call") || commandLower.contains("codegen") || commandLower.contains("smoke")) {
            SmokeStepResult providerProbe = providerCheckInternal(user, Provider.GROQ, false);
            if (!providerProbe.ok()) {
                score -= 25;
                factors.add("-25 provider health check failed");
            } else {
                factors.add("+0 provider health check passed");
            }
        }

        score = Math.max(0, Math.min(100, score));
        String level = score >= 85 ? "HIGH" : score >= 65 ? "MEDIUM" : "LOW";
        return new TrustEnvelope(score, level, factors);
    }

    private void workflowList() {
        String[] headers = {"Macro", "Purpose"};
        String[][] rows = {
            {"ship", "Run readiness checks before shipping"},
            {"hotfix", "Run safe-mode smoke verification flow"},
            {"release-notes", "Generate release summary from current context"}
        };
        TerminalUtils.printSeparator("WORKFLOW MACROS");
        TerminalUtils.printTable(headers, rows);
    }

    private void workflowRun(User user, Map<String, String> flags) {
        String name = require(flags, "--name", "Missing --name (ship|hotfix|release-notes)").toLowerCase(Locale.ROOT);
        switch (name) {
            case "ship" -> {
                Map<String, String> onboardFlags = new LinkedHashMap<>();
                onboardFlags.put("--mode", flags.getOrDefault("--mode", "balanced"));
                onboardFlags.put("--provider", flags.getOrDefault("--provider", "GROQ"));
                onboardRun(user, onboardFlags);
            }
            case "hotfix" -> {
                Map<String, String> onboardFlags = new LinkedHashMap<>();
                onboardFlags.put("--mode", "safe");
                onboardFlags.put("--provider", flags.getOrDefault("--provider", "GROQ"));
                onboardRun(user, onboardFlags);
            }
            case "release-notes" -> {
                Map<String, String> callFlags = new LinkedHashMap<>();
                callFlags.put("--task", TaskType.SUMMARIZATION.name());
                callFlags.put("--prompt", "Generate release notes in markdown with sections Added, Improved, Fixed, Risks, Next Steps.");
                callRun(user, callFlags);
            }
            default -> throw new IllegalArgumentException("Unknown workflow macro: " + name);
        }
    }

    private void prPrep(User user, Map<String, String> flags) {
        String scope = profileService.currentWorkspaceScope();
        ToolExecutionService.ToolPolicy policy = buildToolPolicy(user, scope);
        ToolExecutionService.ToolResult gitStatus = toolExecutionService.execute("git.status", Map.of(), policy);
        boolean gitStatusAvailable = gitStatus.success();
        if (!gitStatusAvailable) {
            TerminalUtils.printWarn("Git status unavailable for PR prep: " + gitStatus.output());
        }

        String[] lines = gitStatusAvailable ? gitStatus.output().split("\\R") : new String[0];
        List<String> changed = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) continue;
            if (trimmed.length() >= 3) changed.add(trimmed.substring(3).trim());
        }

        int risk = 0;
        List<String> suggestions = new ArrayList<>();
        for (String file : changed) {
            if (file.startsWith("src/main/java")) {
                risk += 3;
            }
            if (file.startsWith("site/src")) {
                risk += 2;
            }
            if (file.endsWith("schema.sql")) {
                risk += 4;
            }
        }
        if (!changed.isEmpty()) suggestions.add("Run mvn test before PR.");
        if (changed.stream().anyMatch(f -> f.startsWith("site/"))) suggestions.add("Run frontend build for site module.");
        if (changed.stream().anyMatch(f -> f.contains("schema"))) suggestions.add("Validate DB migration path on a clean database.");
        if (!gitStatusAvailable) {
            suggestions.add("Enable shell policy for automatic git diff (`profile set --key policy.allow_tool_shell --value true`) or provide changed files manually.");
            suggestions.add("Run git status --short manually before opening PR.");
        }
        if (suggestions.isEmpty()) suggestions.add("No changed files detected; ensure branch has intended edits.");

        String level = risk >= 12 ? "HIGH" : risk >= 6 ? "MEDIUM" : "LOW";
        String[] headers = {"PR Metric", "Value"};
        String[][] rows = {
            {"Changed files", String.valueOf(changed.size())},
            {"Git status", gitStatusAvailable ? "available" : "unavailable"},
            {"Policy shell", String.valueOf(profileService.getBooleanSetting(user.getId(), scope, "policy.allow_tool_shell", false))},
            {"Risk score", risk + " (" + level + ")"}
        };

        TerminalUtils.printSeparator("PR PREP");
        TerminalUtils.printTable(headers, rows);
        for (String s : suggestions) {
            TerminalUtils.printStep(s);
        }

        String output = flags.getOrDefault("--output", "").trim();
        if (!output.isBlank()) {
            try {
                Path reportPath = resolvePath(output);
                if (reportPath.getParent() != null && !Files.exists(reportPath.getParent())) {
                    Files.createDirectories(reportPath.getParent());
                }
                StringBuilder report = new StringBuilder();
                report.append("# PR Prep Report\n\n");
                report.append("- Trace ID: ").append(activeTraceId).append("\n");
                report.append("- Changed files: ").append(changed.size()).append("\n");
                report.append("- Git status: ").append(gitStatusAvailable ? "available" : "unavailable").append("\n");
                report.append("- Risk: ").append(risk).append(" (").append(level).append(")\n\n");
                if (gitStatusAvailable && !changed.isEmpty()) {
                    report.append("## Changed Files\n");
                    for (String file : changed) {
                        report.append("- ").append(file).append("\n");
                    }
                    report.append("\n");
                }
                report.append("## Suggested Checks\n");
                for (String s : suggestions) {
                    report.append("- ").append(s).append("\n");
                }
                Files.writeString(reportPath, report.toString(), StandardCharsets.UTF_8);
                TerminalUtils.printInfo("PR prep report: " + reportPath);
            } catch (IOException ioe) {
                TerminalUtils.printWarn("Could not write PR prep report: " + ioe.getMessage());
            }
        }
    }

    private void suggestCommands(User user, Map<String, String> flags) {
        String prefix = flags.getOrDefault("--prefix", "").trim().toLowerCase(Locale.ROOT);
        String scope = profileService.currentWorkspaceScope();

        List<String> pool = new ArrayList<>(List.of(
            "nexus onboard --user <username> --mode balanced --provider GROQ",
            "nexus profile wizard --user <username> --mode balanced",
            "nexus profile doctor --user <username>",
            "nexus storyboard show --user <username>",
            "nexus trust evaluate --user <username> --command \"codegen run --output src/Foo.java\"",
            "nexus workflow run --user <username> --name ship",
            "nexus workflow run --user <username> --name release-notes",
            "nexus pr prep --user <username>",
            "nexus memory timeline --user <username> --query \"architecture\"",
            "nexus recipe marketplace --user <username> --action list"
        ));

        for (AuditLog log : auditLogDao.findByUserId(user.getId()).stream().limit(20).toList()) {
            String action = log.getAction() == null ? "" : log.getAction().toLowerCase(Locale.ROOT);
            if (action.contains("profile")) pool.add("nexus profile list --user <username>");
            if (action.contains("memory")) pool.add("nexus memory recall --user <username> --query \"<topic>\"");
            if (action.contains("call")) pool.add("nexus call run --user <username> --task GENERAL_CHAT --prompt \"...\"");
        }

        if (!profileService.isActionAllowed(user.getId(), scope, "policy.allow_file_write")) {
            pool.add("nexus profile set --user <username> --scope project --key policy.allow_file_write --value true");
        }

        LinkedHashSet<String> dedup = new LinkedHashSet<>();
        for (String cmd : pool) {
            if (prefix.isBlank() || cmd.toLowerCase(Locale.ROOT).contains(prefix)) {
                dedup.add(cmd);
            }
        }

        List<String> out = dedup.stream().limit(12).toList();
        if (out.isEmpty()) {
            TerminalUtils.printInfo("No suggestions found for prefix: " + prefix);
            return;
        }

        String[] headers = {"Suggestion"};
        String[][] rows = new String[out.size()][1];
        for (int i = 0; i < out.size(); i++) {
            rows[i] = new String[] {out.get(i)};
        }
        TerminalUtils.printSeparator("SMART SUGGEST");
        TerminalUtils.printTable(headers, rows);
    }

    private void printStoryboardHelp() {
        System.out.println("Usage:");
        System.out.println("  nexus storyboard show --user <username> [--limit 25]");
    }

    private void printTrustHelp() {
        System.out.println("Usage:");
        System.out.println("  nexus trust evaluate --user <username> --command \"codegen run --output src/Foo.java\"");
    }

    private void printWorkflowHelp() {
        System.out.println("Usage:");
        System.out.println("  nexus workflow list --user <username>");
        System.out.println("  nexus workflow run --user <username> --name ship|hotfix|release-notes [--provider GROQ]");
    }

    private void printPrHelp() {
        System.out.println("Usage:");
        System.out.println("  nexus pr prep --user <username> [--output target/pr/prep.md]");
    }

    private void onboardRun(User user, Map<String, String> flags) {
        String scope = resolveProfileScope(flags.getOrDefault("--scope", "project"), flags.get("--project"));
        String mode = flags.getOrDefault("--mode", "balanced");
        String providerRaw = flags.getOrDefault("--provider", "GROQ");
        Provider provider = Provider.fromAny(providerRaw)
            .orElseThrow(() -> new IllegalArgumentException("Unsupported --provider value: " + providerRaw));

        TerminalUtils.printSeparator("NEXUS ONBOARD");
        TerminalUtils.printInfo("Trace ID: " + activeTraceId);
        TerminalUtils.printInfo("Scope: " + scope + " | Mode: " + mode + " | Provider: " + provider.name());

        List<OnboardStepResult> steps = new ArrayList<>();

        try {
            String providerKey = flags.get("--provider-key");
            boolean fromEnv = parseBooleanFlag(flags, "--from-env", false);
            if (providerKey != null || fromEnv) {
                String alias = flags.getOrDefault("--alias", provider.name().toLowerCase(Locale.ROOT) + "-onboard");
                String detail = configureProviderKey(user, provider, alias, providerKey, fromEnv);
                steps.add(new OnboardStepResult("key", true, detail));
            } else if (apiKeyService.hasKeyForProvider(user.getId(), provider)) {
                steps.add(new OnboardStepResult("key", true, "Using existing " + provider.name() + " key."));
            } else {
                steps.add(new OnboardStepResult("key", false,
                    "No " + provider.name() + " key configured. Pass --provider-key or --from-env true."));
            }
        } catch (Exception e) {
            steps.add(new OnboardStepResult("key", false, e.getMessage()));
        }

        try {
            Map<String, String> wizardValues = wizardValuesForMode(mode);
            for (var entry : wizardValues.entrySet()) {
                profileService.setSetting(user.getId(), scope, entry.getKey(), entry.getValue());
            }
            steps.add(new OnboardStepResult("wizard", true, "Applied mode with " + wizardValues.size() + " keys."));
        } catch (Exception e) {
            steps.add(new OnboardStepResult("wizard", false, e.getMessage()));
        }

        try {
            ProfileHealthSummary health = evaluateProfileHealth(user, scope);
            boolean ok = health.riskCount() == 0;
            String detail = "risks=" + health.riskCount() + ", heavy=" + health.heavyCount() + ", checks=" + health.totalChecks();
            steps.add(new OnboardStepResult("doctor", ok, detail));
        } catch (Exception e) {
            steps.add(new OnboardStepResult("doctor", false, e.getMessage()));
        }

        try {
            SmokeStepResult providerResult = providerCheckInternal(user, provider, false);
            steps.add(new OnboardStepResult("provider", providerResult.ok(), providerResult.detail()));
        } catch (Exception e) {
            steps.add(new OnboardStepResult("provider", false, e.getMessage()));
        }

        try {
            Map<String, String> smokeFlags = new LinkedHashMap<>();
            smokeFlags.put("--provider", provider.name());
            boolean smokeOk = smokeRun(user, smokeFlags);
            steps.add(new OnboardStepResult("smoke", smokeOk, smokeOk ? "All smoke checks passed." : "Smoke checks reported failures."));
        } catch (Exception e) {
            steps.add(new OnboardStepResult("smoke", false, e.getMessage()));
        }

        int passed = 0;
        for (OnboardStepResult step : steps) {
            if (step.ok()) passed++;
        }
        int score = steps.isEmpty() ? 0 : (int) Math.round((passed * 100.0) / steps.size());

        String[] headers = {"Step", "Status", "Details"};
        String[][] rows = new String[steps.size()][3];
        for (int i = 0; i < steps.size(); i++) {
            OnboardStepResult step = steps.get(i);
            rows[i] = new String[] {
                step.step(),
                step.ok() ? TerminalUtils.GREEN + "PASS" + TerminalUtils.RESET : TerminalUtils.RED + "FAIL" + TerminalUtils.RESET,
                truncate(step.detail(), 110)
            };
        }
        TerminalUtils.printTable(headers, rows);
        TerminalUtils.printInfo("Readiness score: " + score + "% " + TerminalUtils.progressBar(score, 100, 18));

        try {
            writeOnboardArtifacts(user, scope, mode, provider, score, steps);
        } catch (IOException ioe) {
            TerminalUtils.printWarn("Could not write onboard artifacts: " + ioe.getMessage());
        }

        if (score >= 90) {
            TerminalUtils.printSuccess("Onboarding complete. CLI is production-ready for this scope.");
        } else if (score >= 70) {
            TerminalUtils.printWarn("Onboarding mostly complete. Review failed steps before full adoption.");
        } else {
            TerminalUtils.printWarn("Onboarding incomplete. Fix wizard/doctor/provider issues and rerun.");
        }
    }

    private ProfileHealthSummary evaluateProfileHealth(User user, String scope) {
        int risks = 0;
        int heavy = 0;
        int checks = 5;

        if (profileService.getBooleanSetting(user.getId(), scope, "policy.allow_tool_shell", false)) {
            risks++;
        }
        int maxTokens = profileService.getIntSetting(user.getId(), scope, "context.max_injection_tokens", 900);
        if (maxTokens > 1400) {
            heavy++;
        }
        int maxMemories = profileService.getIntSetting(user.getId(), scope, "context.max_memories", 6);
        if (maxMemories > 8) {
            heavy++;
        }
        if (!profileService.getBooleanSetting(user.getId(), scope, "intent.require_confirmation_on_drift", true)) {
            risks++;
        }
        if (profileService.getIntSetting(user.getId(), scope, "routing.max_fallback_candidates", 3) < 2) {
            risks++;
        }

        return new ProfileHealthSummary(risks, heavy, checks);
    }

    private void writeOnboardArtifacts(
        User user,
        String scope,
        String mode,
        Provider provider,
        int score,
        List<OnboardStepResult> steps
    ) throws IOException {
        Path dir = resolvePath("target/onboard");
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        String stamp = LocalDateTime.now().toString().replace(':', '-');
        String base = "onboard-" + stamp + "-" + activeTraceId;
        Path jsonPath = dir.resolve(base + ".json");
        Path badgePath = dir.resolve(base + ".badge.md");

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"traceId\": \"").append(jsonEscape(activeTraceId)).append("\",\n");
        json.append("  \"timestamp\": \"").append(jsonEscape(LocalDateTime.now().toString())).append("\",\n");
        json.append("  \"user\": \"").append(jsonEscape(user.getUsername())).append("\",\n");
        json.append("  \"scope\": \"").append(jsonEscape(scope)).append("\",\n");
        json.append("  \"mode\": \"").append(jsonEscape(mode)).append("\",\n");
        json.append("  \"provider\": \"").append(provider.name()).append("\",\n");
        json.append("  \"readinessScore\": ").append(score).append(",\n");
        json.append("  \"steps\": [\n");
        for (int i = 0; i < steps.size(); i++) {
            OnboardStepResult step = steps.get(i);
            json.append("    {\"step\": \"").append(jsonEscape(step.step())).append("\", ");
            json.append("\"ok\": ").append(step.ok()).append(", ");
            json.append("\"detail\": \"").append(jsonEscape(step.detail())).append("\"}");
            if (i < steps.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ]\n}");

        String color = score >= 90 ? "brightgreen" : score >= 70 ? "yellow" : "red";
        String badge = "![nexus-onboard](https://img.shields.io/badge/nexus_onboard-" + score + "%25-" + color + ")\n";

        Files.writeString(jsonPath, json.toString(), StandardCharsets.UTF_8);
        Files.writeString(badgePath, badge, StandardCharsets.UTF_8);
        TerminalUtils.printInfo("Onboard artifacts: " + jsonPath + " and " + badgePath);
    }

    private void providerList(User user) {
        String[] headers = {"Provider", "Key", "Models"};
        List<String[]> rows = new ArrayList<>();

        for (Provider provider : Provider.values()) {
            if (provider == Provider.CUSTOM) continue;
            boolean hasKey = apiKeyService.hasKeyForProvider(user.getId(), provider);
            long modelCount = modelDao.findAll().stream()
                .filter(m -> Provider.fromAny(m.getProvider()).map(p -> p == provider).orElse(false))
                .count();
            rows.add(new String[] {
                provider.name(),
                hasKey ? TerminalUtils.GREEN + "YES" + TerminalUtils.RESET : TerminalUtils.YELLOW + "NO" + TerminalUtils.RESET,
                String.valueOf(modelCount)
            });
        }

        TerminalUtils.printTable(headers, rows.toArray(String[][]::new));
    }

    private void providerCheck(User user, Map<String, String> flags) {
        String providerRaw = require(flags, "--provider", "Missing --provider (e.g. GROQ)");
        Provider provider = Provider.fromAny(providerRaw)
            .orElseThrow(() -> new IllegalArgumentException("Unsupported provider: " + providerRaw));

        SmokeStepResult result = providerCheckInternal(user, provider, true);
        if (result.ok()) {
            TerminalUtils.printSuccess(result.step() + " ok: " + result.detail());
        } else {
            TerminalUtils.printError(result.step() + " failed: " + result.detail());
        }
    }

    private void providerSetup(User user, Map<String, String> flags) {
        String providerRaw = flags.getOrDefault("--provider", "GROQ");
        Provider provider = Provider.fromAny(providerRaw)
            .orElseThrow(() -> new IllegalArgumentException("Unsupported provider: " + providerRaw));

        String alias = flags.getOrDefault("--alias", provider.name().toLowerCase(Locale.ROOT) + "-default");
        String key = flags.get("--key");
        boolean fromEnv = parseBooleanFlag(flags, "--from-env", true);

        String detail = configureProviderKey(user, provider, alias, key, fromEnv);
        TerminalUtils.printSuccess("Provider key configured for " + provider.name() + ".");
        TerminalUtils.printInfo(detail);

        boolean runCheck = parseBooleanFlag(flags, "--check", true);
        if (runCheck) {
            SmokeStepResult result = providerCheckInternal(user, provider, true);
            if (result.ok()) {
                TerminalUtils.printSuccess(result.step() + " ok: " + result.detail());
            } else {
                TerminalUtils.printWarn(result.step() + " failed: " + result.detail());
            }
        }
    }

    private String configureProviderKey(User user, Provider provider, String alias, String directKey, boolean fromEnv) {
        String key = resolveProviderKey(provider, directKey, fromEnv);
        if (key == null || key.isBlank()) {
            String envName = providerKeyEnvVar(provider);
            throw new IllegalArgumentException(
                "No key value supplied. Pass --key <value> or set " + envName + " and use --from-env true.");
        }

        int rotated = rotateProviderKeys(user, provider);
        String effectiveAlias = alias == null || alias.isBlank()
            ? provider.name().toLowerCase(Locale.ROOT) + "-default"
            : alias.trim();
        ApiKey stored = apiKeyService.storeKey(user.getId(), provider, effectiveAlias, key.trim());

        StringBuilder detail = new StringBuilder();
        detail.append("Stored alias=").append(effectiveAlias)
            .append(", masked=").append(stored.getMaskedKey());
        if (rotated > 0) {
            detail.append(", rotated previous=").append(rotated);
        }
        return detail.toString();
    }

    private int rotateProviderKeys(User user, Provider provider) {
        int rotated = 0;
        for (ApiKey key : apiKeyService.listKeysForUser(user.getId())) {
            if (key.getProvider() != provider || key.getId() == null) continue;
            apiKeyService.deleteKey(user.getId(), key.getId());
            rotated++;
        }
        return rotated;
    }

    private String resolveProviderKey(Provider provider, String directKey, boolean fromEnv) {
        if (directKey != null && !directKey.isBlank()) {
            return directKey;
        }
        if (!fromEnv) {
            return "";
        }
        String envValue = System.getenv(providerKeyEnvVar(provider));
        return envValue == null ? "" : envValue;
    }

    private String providerKeyEnvVar(Provider provider) {
        return switch (provider) {
            case GROQ -> "GROQ_API_KEY";
            case OPENAI -> "OPENAI_API_KEY";
            case ANTHROPIC -> "ANTHROPIC_API_KEY";
            case GOOGLE_GEMINI -> "GOOGLE_API_KEY";
            case OPENROUTER -> "OPENROUTER_API_KEY";
            case CUSTOM -> "NEXUS_CUSTOM_API_KEY";
        };
    }

    private SmokeStepResult providerCheckInternal(User user, Provider provider, boolean verbose) {
        if (!apiKeyService.hasKeyForProvider(user.getId(), provider)) {
            return new SmokeStepResult("provider:" + provider.name(), false,
                "No API key configured for this user/provider.");
        }

        List<LlmModel> models = modelDao.findAll().stream()
            .filter(m -> Provider.fromAny(m.getProvider()).map(p -> p == provider).orElse(false))
            .toList();

        if (models.isEmpty()) {
            return new SmokeStepResult("provider:" + provider.name(), true,
                "API key exists, but no models are registered for this provider.");
        }

        LlmModel probeModel = models.get(0);
        LlmCallService.HealthReport report = llmCallService.checkHealth(user.getId(), probeModel);
        if (verbose) {
            TerminalUtils.printInfo("Provider check model: " + probeModel.getName());
        }
        if (report.reachable()) {
            return new SmokeStepResult("provider:" + provider.name(), true,
                "Healthy via " + probeModel.getName() + " (" + report.latencyMs() + "ms)");
        }
        return new SmokeStepResult("provider:" + provider.name(), false,
            "Health check failed via " + probeModel.getName() + ": " + report.status());
    }

    private boolean smokeRun(User user, Map<String, String> flags) {
        String providerRaw = flags.getOrDefault("--provider", "GROQ");
        Provider provider = Provider.fromAny(providerRaw)
            .orElseThrow(() -> new IllegalArgumentException("Unsupported --provider value: " + providerRaw));

        TerminalUtils.printSeparator("SMOKE CHECK");
        TerminalUtils.printInfo("Trace ID: " + activeTraceId);

        List<SmokeStepResult> results = new ArrayList<>();
        results.add(providerCheckInternal(user, provider, false));

        try {
            LlmModel model = routingEngine.selectOptimalModelForUser(TaskType.GENERAL_CHAT, Double.MAX_VALUE, user.getId());
            if (model == null) {
                results.add(new SmokeStepResult("call", false, "No routable model for GENERAL_CHAT."));
            } else {
                ModelExecutionResult modelExecution = executeWithFallback(user, TaskType.GENERAL_CHAT,
                    "Reply with exactly smoke-ok", model);
                String mode = modelExecution.execution().result().simulated() ? "SIMULATED" : "REAL";
                results.add(new SmokeStepResult("call", true,
                    "Model=" + modelExecution.model().getName() + " mode=" + mode));
            }
        } catch (Exception e) {
            results.add(new SmokeStepResult("call", false, e.getMessage()));
        }

        try {
            Map<String, String> codegenFlags = new LinkedHashMap<>();
            codegenFlags.put("--task", TaskType.CODE_GENERATION.name());
            codegenFlags.put("--prompt", "Create class SmokeRunner with method status returning ok");
            codegenFlags.put("--output", "target/smoke/SmokeRunner-" + activeTraceId + ".java");
            codegenFlags.put("--overwrite", "true");
            codegenFlags.put("--remember", "false");
            codegenFlags.put("--strict-code", "true");
            codegenFlags.put("--format", "java");
            codegenRun(user, codegenFlags);
            results.add(new SmokeStepResult("codegen", true, "Generated smoke file successfully."));
        } catch (Exception e) {
            results.add(new SmokeStepResult("codegen", false, e.getMessage()));
        }

        try {
            ToolExecutionService.ToolPolicy policy = buildToolPolicy(user, profileService.currentWorkspaceScope());
            ToolExecutionService.ToolResult toolResult = toolExecutionService.execute(
                "fs.read", Map.of("path", "README.md", "maxchars", "120"), policy);
            if (toolResult.success()) {
                results.add(new SmokeStepResult("tool", true, "fs.read executed."));
            } else {
                results.add(new SmokeStepResult("tool", false, toolResult.output()));
            }
        } catch (Exception e) {
            results.add(new SmokeStepResult("tool", false, e.getMessage()));
        }

        try {
            Path recipePath = resolvePath("target/smoke/smoke-" + activeTraceId + ".recipe");
            if (recipePath.getParent() != null && !Files.exists(recipePath.getParent())) {
                Files.createDirectories(recipePath.getParent());
            }
            String recipeContent = String.join("\n",
                "PROFILE|project|smoke_trace|" + activeTraceId,
                "TOOL|fs.read|path=README.md;maxchars=100",
                "CALL|GENERAL_CHAT|Reply with exactly recipe-smoke-ok"
            ) + "\n";
            Files.writeString(recipePath, recipeContent, StandardCharsets.UTF_8);

            List<String> recipeIssues = validateRecipeFile(recipePath);
            if (!recipeIssues.isEmpty()) {
                results.add(new SmokeStepResult("recipe", false, "Validation failed: " + recipeIssues.get(0)));
            } else {
                Map<String, String> recipeFlags = new LinkedHashMap<>();
                recipeFlags.put("--file", recipePath.toString());
                recipeRun(user, recipeFlags);
                results.add(new SmokeStepResult("recipe", true, "Recipe validation + run succeeded."));
            }
        } catch (Exception e) {
            results.add(new SmokeStepResult("recipe", false, e.getMessage()));
        }

        String[] headers = {"Step", "Status", "Details"};
        String[][] rows = new String[results.size()][3];
        boolean allOk = true;
        for (int i = 0; i < results.size(); i++) {
            SmokeStepResult r = results.get(i);
            allOk = allOk && r.ok();
            rows[i] = new String[] {
                r.step(),
                r.ok() ? TerminalUtils.GREEN + "PASS" + TerminalUtils.RESET : TerminalUtils.RED + "FAIL" + TerminalUtils.RESET,
                truncate(r.detail(), 90)
            };
        }
        TerminalUtils.printTable(headers, rows);

        try {
            writeSmokeReportArtifacts(provider, results, allOk);
        } catch (Exception e) {
            TerminalUtils.printWarn("Failed to write smoke artifacts: " + e.getMessage());
        }

        if (allOk) {
            TerminalUtils.printSuccess("Smoke run passed.");
        } else {
            TerminalUtils.printWarn("Smoke run completed with failures. See details above. Trace ID: " + activeTraceId);
        }
        return allOk;
    }

    private void writeSmokeReportArtifacts(Provider provider, List<SmokeStepResult> results, boolean allOk) throws IOException {
        Path dir = resolvePath("target/smoke");
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        String stamp = LocalDateTime.now().toString().replace(':', '-');
        String baseName = "smoke-" + stamp + "-" + activeTraceId;
        Path jsonPath = dir.resolve(baseName + ".json");
        Path mdPath = dir.resolve(baseName + ".md");

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"traceId\": \"").append(jsonEscape(activeTraceId)).append("\",\n");
        json.append("  \"timestamp\": \"").append(jsonEscape(LocalDateTime.now().toString())).append("\",\n");
        json.append("  \"provider\": \"").append(provider.name()).append("\",\n");
        json.append("  \"allPassed\": ").append(allOk).append(",\n");
        json.append("  \"steps\": [\n");
        for (int i = 0; i < results.size(); i++) {
            SmokeStepResult r = results.get(i);
            json.append("    {\"step\": \"").append(jsonEscape(r.step())).append("\", ");
            json.append("\"ok\": ").append(r.ok()).append(", ");
            json.append("\"detail\": \"").append(jsonEscape(r.detail())).append("\"}");
            if (i < results.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ]\n");
        json.append("}\n");

        StringBuilder md = new StringBuilder();
        md.append("# Nexus Smoke Report\n\n");
        md.append("- Trace ID: ").append(activeTraceId).append("\n");
        md.append("- Timestamp: ").append(LocalDateTime.now()).append("\n");
        md.append("- Provider: ").append(provider.name()).append("\n");
        md.append("- Overall: ").append(allOk ? "PASS" : "FAIL").append("\n\n");
        md.append("| Step | Status | Detail |\n");
        md.append("|---|---|---|\n");
        for (SmokeStepResult r : results) {
            md.append("|")
                .append(r.step())
                .append("|")
                .append(r.ok() ? "PASS" : "FAIL")
                .append("|")
                .append(truncate(r.detail().replace("|", "\\|"), 180))
                .append("|\n");
        }

        Files.writeString(jsonPath, json.toString(), StandardCharsets.UTF_8);
        Files.writeString(mdPath, md.toString(), StandardCharsets.UTF_8);
        TerminalUtils.printInfo("Smoke artifacts: " + jsonPath + " and " + mdPath);
    }

    private String jsonEscape(String value) {
        if (value == null) return "";
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }

    private record SmokeStepResult(String step, boolean ok, String detail) {}

    private void toolList() {
        String[] headers = {"Tool", "Purpose"};
        String[][] rows = {
            {"fs.read", "Read UTF-8 file content"},
            {"fs.write", "Write UTF-8 file content"},
            {"shell.exec", "Execute shell command (policy-gated)"},
            {"git.status", "Run git status --short"}
        };
        TerminalUtils.printTable(headers, rows);
    }

    private void toolRun(User user, Map<String, String> flags) {
        String toolName = require(flags, "--name", "Missing --name (tool id)");
        Map<String, String> params = collectToolParams(flags);
        String scope = profileService.currentWorkspaceScope();

        ToolExecutionService.ToolPolicy policy = buildToolPolicy(user, scope);
        ToolExecutionService.ToolResult result = toolExecutionService.execute(toolName, params, policy);

        if (result.success()) {
            TerminalUtils.printSuccess("Tool succeeded: " + toolName);
            if (result.output() != null && !result.output().isBlank()) {
                TerminalUtils.printBox("TOOL OUTPUT", truncate(result.output(), 5000));
            }
        } else {
            TerminalUtils.printError("Tool failed: " + toolName);
            if (result.output() != null && !result.output().isBlank()) {
                TerminalUtils.printError(result.output());
            }
        }
    }

    private ToolExecutionService.ToolPolicy buildToolPolicy(User user, String scope) {
        boolean fsRead = profileService.getBooleanSetting(user.getId(), scope, "policy.allow_tool_fs_read", true);
        boolean fsWrite = profileService.getBooleanSetting(user.getId(), scope, "policy.allow_tool_fs_write", true)
            && profileService.isActionAllowed(user.getId(), scope, "policy.allow_file_write");
        boolean shell = profileService.getBooleanSetting(user.getId(), scope, "policy.allow_tool_shell", false);
        boolean externalRead = profileService.getBooleanSetting(user.getId(), scope, "policy.allow_external_read", false);
        boolean externalWrite = profileService.getBooleanSetting(user.getId(), scope, "policy.allow_external_write", false);

        return new ToolExecutionService.ToolPolicy(fsRead, fsWrite, shell, externalRead, externalWrite);
    }

    private Map<String, String> collectToolParams(Map<String, String> flags) {
        Map<String, String> params = new LinkedHashMap<>();
        params.putAll(parseKeyValueParams(flags.get("--params")));

        for (var entry : flags.entrySet()) {
            String key = entry.getKey();
            if (key.equals("--user") || key.equals("--password") || key.equals("--name") || key.equals("--params")) {
                continue;
            }
            if (key.startsWith("--")) {
                params.put(key.substring(2), entry.getValue());
            }
        }
        return params;
    }

    private Map<String, String> parseKeyValueParams(String raw) {
        Map<String, String> params = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return params;

        String[] pairs = raw.split(";");
        for (String pair : pairs) {
            String p = pair.trim();
            if (p.isEmpty()) continue;

            int idx = p.indexOf('=');
            if (idx <= 0 || idx >= p.length() - 1) continue;
            String k = p.substring(0, idx).trim().toLowerCase(Locale.ROOT);
            String v = p.substring(idx + 1).trim();
            if (!k.isEmpty()) params.put(k, v);
        }
        return params;
    }

    private void printToolHelp() {
        System.out.println("Usage:");
        System.out.println("  nexus tool list --user <username>");
        System.out.println("  nexus tool run --user <username> --name <tool> [--params \"k=v;k2=v2\"] [--k value ...]");
        System.out.println("Examples:");
        System.out.println("  nexus tool run --user admin --name fs.read --path README.md --maxchars 1500");
        System.out.println("  nexus tool run --user admin --name shell.exec --command \"git status --short\" --timeoutseconds 10");
        System.out.println("Policy keys:");
        System.out.println("  policy.allow_tool_fs_read, policy.allow_tool_fs_write, policy.allow_tool_shell");
    }

    private void printProviderHelp() {
        System.out.println("Usage:");
        System.out.println("  nexus provider list --user <username>");
        System.out.println("  nexus provider check --user <username> --provider GROQ|OPENAI|ANTHROPIC|GOOGLE_GEMINI|OPENROUTER");
        System.out.println("  nexus provider setup --user <username> [--provider GROQ] [--alias groq-default] [--key <secret> | --from-env true] [--check true]");
    }

    private void printPolicyHelp() {
        System.out.println("Usage:");
        System.out.println("  nexus policy simulate --user <username> --command \"codegen run --output src/Foo.java\"");
        System.out.println("Examples:");
        System.out.println("  nexus policy simulate --user admin --command \"tool run --name shell.exec --command git status\"");
        System.out.println("  nexus policy simulate --user admin --command \"recipe run --file recipes/example.recipe\"");
    }

    private void printSmokeHelp() {
        System.out.println("Usage:");
        System.out.println("  nexus smoke run --user <username> [--provider GROQ]");
        System.out.println("  Artifacts are written to target/smoke as JSON and Markdown.");
    }

    private void printOnboardHelp() {
        System.out.println("Usage:");
        System.out.println("  nexus onboard --user <username> [--scope project|global|<scope>] [--mode safe|balanced|power-user] [--provider GROQ]");
        System.out.println("              [--provider-key <secret> | --from-env true] [--alias <provider-alias>]");
        System.out.println("  nexus onboard run --user <username> [--mode balanced] [--provider GROQ]");
        System.out.println("What it runs:");
        System.out.println("  provider key setup -> profile wizard -> profile doctor -> provider check -> smoke run");
        System.out.println("Output:");
        System.out.println("  Readiness score (0-100%) and smoke artifacts under target/smoke");
    }

    private void printFailureGuidance(String message) {
        String msg = message == null ? "" : message.toLowerCase(Locale.ROOT);

        if (msg.contains("no api key configured")) {
            TerminalUtils.printInfo("Guidance: Configure provider keys first, then rerun provider check.");
            return;
        }
        if (msg.contains("policy.allow_file_write")) {
            TerminalUtils.printInfo("Guidance: Enable file writes with profile policy or switch to read-only commands.");
            return;
        }
        if (msg.contains("policy.allow_recipe_run")) {
            TerminalUtils.printInfo("Guidance: Enable recipe run policy for this scope before executing recipes.");
            return;
        }
        if (msg.contains("recipe validation failed")) {
            TerminalUtils.printInfo("Guidance: Run recipe validate and fix the line/step shown in the error.");
            return;
        }
        if (msg.contains("no candidate models available")) {
            TerminalUtils.printInfo("Guidance: Verify provider keys/models or enable fallback candidates in profile settings.");
            return;
        }
        if (msg.contains("drifts from pinned intent")) {
            TerminalUtils.printInfo("Guidance: Re-run with --confirm-intent true for one-off overrides or --pin-intent true to repin workspace intent.");
            return;
        }
        if (msg.contains("missing output language")) {
            TerminalUtils.printInfo("Guidance: Pass --format <language> when your output file extension is ambiguous.");
            return;
        }
        if (msg.contains("outside workspace")) {
            TerminalUtils.printInfo("Guidance: Use an in-workspace output path or enable external write policy explicitly.");
        }
    }

    private String newTraceId() {
        return "nx-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void profileSet(User user, Map<String, String> flags) {
        String key = require(flags, "--key", "Missing --key");
        String value = require(flags, "--value", "Missing --value");
        String scope = resolveProfileScope(flags.getOrDefault("--scope", "project"), flags.get("--project"));

        profileService.setSetting(user.getId(), scope, key, value);
        TerminalUtils.printSuccess("Profile setting saved.");
        TerminalUtils.printInfo("Scope: " + scope + " | Key: " + key);
    }

    private void profileList(User user, Map<String, String> flags) {
        String scope = resolveProfileScope(flags.getOrDefault("--scope", "project"), flags.get("--project"));
        boolean merged = Boolean.parseBoolean(flags.getOrDefault("--merged", "true"));
        Map<String, String> settings = profileService.listSettings(user.getId(), scope, merged);

        if (settings.isEmpty()) {
            TerminalUtils.printInfo("No profile settings found.");
            return;
        }

        String[] headers = {"Section", "Key", "Value"};
        String[][] rows = new String[settings.size()][3];
        int i = 0;
        for (var entry : settings.entrySet()) {
            rows[i++] = new String[] {
                profileSection(entry.getKey()),
                entry.getKey(),
                truncate(entry.getValue(), 120)
            };
        }
        TerminalUtils.printTable(headers, rows);
        TerminalUtils.printInfo("Scope: " + scope + " | merged=" + merged);
    }

    private void profileDelete(User user, Map<String, String> flags) {
        String key = require(flags, "--key", "Missing --key");
        String scope = resolveProfileScope(flags.getOrDefault("--scope", "project"), flags.get("--project"));
        boolean deleted = profileService.deleteSetting(user.getId(), scope, key);

        if (deleted) TerminalUtils.printSuccess("Profile setting deleted.");
        else TerminalUtils.printInfo("No matching profile setting found.");
    }

    private void profilePreset(User user, Map<String, String> flags) {
        String preset = require(flags, "--name", "Missing --name (safe|balanced|power-user)")
            .trim().toLowerCase(Locale.ROOT);
        String scope = resolveProfileScope(flags.getOrDefault("--scope", "project"), flags.get("--project"));

        Map<String, String> values = switch (preset) {
            case "safe" -> Map.ofEntries(
                Map.entry("policy.allow_file_write", "false"),
                Map.entry("policy.allow_recipe_run", "false"),
                Map.entry("policy.allow_tool_fs_read", "true"),
                Map.entry("policy.allow_tool_fs_write", "false"),
                Map.entry("policy.allow_tool_shell", "false"),
                Map.entry("policy.allow_external_read", "false"),
                Map.entry("policy.allow_external_write", "false"),
                Map.entry("policy.enable_provider_fallback", "true"),
                Map.entry("routing.max_fallback_candidates", "2")
            );
            case "balanced" -> Map.ofEntries(
                Map.entry("policy.allow_file_write", "true"),
                Map.entry("policy.allow_recipe_run", "true"),
                Map.entry("policy.allow_tool_fs_read", "true"),
                Map.entry("policy.allow_tool_fs_write", "true"),
                Map.entry("policy.allow_tool_shell", "false"),
                Map.entry("policy.allow_external_read", "false"),
                Map.entry("policy.allow_external_write", "false"),
                Map.entry("policy.enable_provider_fallback", "true"),
                Map.entry("routing.max_fallback_candidates", "3")
            );
            case "power", "power-user", "poweruser" -> Map.ofEntries(
                Map.entry("policy.allow_file_write", "true"),
                Map.entry("policy.allow_recipe_run", "true"),
                Map.entry("policy.allow_tool_fs_read", "true"),
                Map.entry("policy.allow_tool_fs_write", "true"),
                Map.entry("policy.allow_tool_shell", "true"),
                Map.entry("policy.allow_external_read", "true"),
                Map.entry("policy.allow_external_write", "true"),
                Map.entry("policy.enable_provider_fallback", "true"),
                Map.entry("routing.max_fallback_candidates", "4")
            );
            default -> throw new IllegalArgumentException("Unknown preset: " + preset + " (use safe|balanced|power-user)");
        };

        for (var entry : values.entrySet()) {
            profileService.setSetting(user.getId(), scope, entry.getKey(), entry.getValue());
        }

        TerminalUtils.printSuccess("Applied profile preset: " + preset);
        TerminalUtils.printInfo("Scope: " + scope + " | Settings: " + values.size());
    }

    private void profileDoctor(User user, Map<String, String> flags) {
        String scope = resolveProfileScope(flags.getOrDefault("--scope", "project"), flags.get("--project"));
        Map<String, String> settings = profileService.listSettings(user.getId(), scope, true);

        String[] keys = {
            "policy.allow_file_write",
            "policy.allow_recipe_run",
            "policy.allow_tool_shell",
            "context.max_injection_tokens",
            "context.max_memories",
            "intent.require_confirmation_on_drift",
            "intent.min_alignment_percent",
            "routing.max_fallback_candidates"
        };

        String[] headers = {"Key", "Current", "Health", "Recommendation"};
        String[][] rows = new String[keys.length][4];

        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            String current = settings.getOrDefault(key, "<default>");
            String health = "OK";
            String recommendation = "";

            if (key.equals("policy.allow_tool_shell") && profileService.getBooleanSetting(user.getId(), scope, key, false)) {
                health = "RISK";
                recommendation = "Set false unless you need shell execution.";
            } else if (key.equals("context.max_injection_tokens")) {
                int val = profileService.getIntSetting(user.getId(), scope, key, 900);
                if (val > 1400) {
                    health = "HEAVY";
                    recommendation = "Reduce to 700-1200 to avoid bloated prompts.";
                } else {
                    recommendation = "700-1200 works well for most projects.";
                }
            } else if (key.equals("context.max_memories")) {
                int val = profileService.getIntSetting(user.getId(), scope, key, 6);
                if (val > 8) {
                    health = "HEAVY";
                    recommendation = "Use 4-8 memories to keep responses focused.";
                } else {
                    recommendation = "4-8 keeps context useful without noise.";
                }
            } else if (key.equals("intent.require_confirmation_on_drift") && !profileService.getBooleanSetting(user.getId(), scope, key, true)) {
                health = "RISK";
                recommendation = "Enable true for safer intent-locked generation.";
            } else if (key.equals("routing.max_fallback_candidates")) {
                int val = profileService.getIntSetting(user.getId(), scope, key, 3);
                if (val < 2) {
                    health = "LOW";
                    recommendation = "Use 2-4 for robust provider fallback.";
                } else {
                    recommendation = "2-4 candidates recommended.";
                }
            }

            String healthView = switch (health) {
                case "RISK" -> TerminalUtils.RED + health + TerminalUtils.RESET;
                case "HEAVY" -> TerminalUtils.YELLOW + health + TerminalUtils.RESET;
                case "LOW" -> TerminalUtils.YELLOW + health + TerminalUtils.RESET;
                default -> TerminalUtils.GREEN + health + TerminalUtils.RESET;
            };

            rows[i] = new String[] {key, current, healthView, recommendation};
        }

        TerminalUtils.printSeparator("PROFILE DOCTOR");
        TerminalUtils.printTable(headers, rows);
        TerminalUtils.printInfo("Scope: " + scope);
        TerminalUtils.printInfo("Fix quickly: nexus profile wizard --user " + user.getUsername() + " --scope project");
    }

    private void profileWizard(User user, Map<String, String> flags) {
        String scope = resolveProfileScope(flags.getOrDefault("--scope", "project"), flags.get("--project"));
        String mode = flags.get("--mode");
        if (mode == null || mode.isBlank()) {
            TerminalUtils.printSeparator("PROFILE WIZARD");
            System.out.println("Choose mode: safe | balanced | power-user");
            mode = readLine("Mode [balanced]: ");
        }

        String normalizedMode = (mode == null || mode.isBlank()) ? "balanced" : mode.trim().toLowerCase(Locale.ROOT);
        Map<String, String> values = wizardValuesForMode(normalizedMode);

        for (var entry : values.entrySet()) {
            profileService.setSetting(user.getId(), scope, entry.getKey(), entry.getValue());
        }

        TerminalUtils.printSuccess("Profile wizard applied mode: " + normalizedMode);
        TerminalUtils.printInfo("Scope: " + scope + " | Keys updated: " + values.size());
        TerminalUtils.printInfo("Run 'nexus profile doctor --user " + user.getUsername() + " --scope project' to verify.");
    }

    private Map<String, String> wizardValuesForMode(String mode) {
        String normalizedMode = mode == null ? "balanced" : mode.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedMode) {
            case "safe" -> Map.ofEntries(
                Map.entry("policy.allow_file_write", "false"),
                Map.entry("policy.allow_recipe_run", "false"),
                Map.entry("policy.allow_tool_shell", "false"),
                Map.entry("context.max_injection_tokens", "700"),
                Map.entry("context.max_memories", "4"),
                Map.entry("intent.require_confirmation_on_drift", "true"),
                Map.entry("intent.min_alignment_percent", "15")
            );
            case "balanced" -> Map.ofEntries(
                Map.entry("policy.allow_file_write", "true"),
                Map.entry("policy.allow_recipe_run", "true"),
                Map.entry("policy.allow_tool_shell", "false"),
                Map.entry("context.max_injection_tokens", "900"),
                Map.entry("context.max_memories", "6"),
                Map.entry("intent.require_confirmation_on_drift", "true"),
                Map.entry("intent.min_alignment_percent", "12")
            );
            case "power", "power-user", "poweruser" -> Map.ofEntries(
                Map.entry("policy.allow_file_write", "true"),
                Map.entry("policy.allow_recipe_run", "true"),
                Map.entry("policy.allow_tool_shell", "true"),
                Map.entry("context.max_injection_tokens", "1200"),
                Map.entry("context.max_memories", "8"),
                Map.entry("intent.require_confirmation_on_drift", "true"),
                Map.entry("intent.min_alignment_percent", "10")
            );
            default -> throw new IllegalArgumentException("Unknown mode: " + normalizedMode + " (use safe|balanced|power-user)");
        };
    }

    private String profileSection(String key) {
        if (key == null || key.isBlank()) return "general";
        if (key.startsWith("policy.")) return "policy";
        if (key.startsWith("context.")) return "context";
        if (key.startsWith("intent.")) return "intent";
        if (key.startsWith("routing.")) return "routing";
        if (key.startsWith("memory.")) return "memory";
        return "general";
    }

    private void printProfileHelp() {
        TerminalUtils.printSeparator("PROFILE COMMANDS");
        System.out.println("Usage:");
        System.out.println("  nexus profile set --user <username> --key <key> --value <value> [--scope project|global|<scope>] [--project <path>]");
        System.out.println("  nexus profile list --user <username> [--scope project|global|<scope>] [--project <path>] [--merged true|false]");
        System.out.println("  nexus profile delete --user <username> --key <key> [--scope project|global|<scope>] [--project <path>]");
        System.out.println("  nexus profile preset --user <username> --name safe|balanced|power-user [--scope project|global|<scope>]");
        System.out.println("  nexus profile doctor --user <username> [--scope project|global|<scope>]");
        System.out.println("  nexus profile wizard --user <username> [--scope project|global|<scope>] [--mode safe|balanced|power-user]");
        System.out.println();
        System.out.println("Recommended flow:");
        System.out.println("  1) nexus profile wizard --user <username> --mode balanced");
        System.out.println("  2) nexus profile doctor --user <username>");
        System.out.println("  3) nexus profile list --user <username>");
    }

    private String resolveProfileScope(String scopeRaw, String explicitProject) {
        String normalized = scopeRaw == null ? "project" : scopeRaw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "global" -> ProfileService.GLOBAL_SCOPE;
            case "project" -> {
                if (explicitProject != null && !explicitProject.isBlank()) {
                    yield explicitProject.trim().toLowerCase(Locale.ROOT);
                }
                yield profileService.currentWorkspaceScope();
            }
            default -> normalized;
        };
    }

    private LlmModel resolveModelForTask(User user, TaskType task, Map<String, String> flags) {
        String override = flags.get("--model");
        if (override != null && !override.isBlank()) {
            return modelDao.findAll().stream()
                .filter(m -> m.getName() != null && m.getName().equalsIgnoreCase(override.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + override));
        }
        return routingEngine.selectOptimalModelForUser(task, Double.MAX_VALUE, user.getId());
    }

    private void writeGeneratedToFile(User user, String scope, String outputRaw, String content, boolean overwrite) throws IOException {
        Path path = resolvePath(outputRaw);
        Path workspace = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path target = path.toAbsolutePath().normalize();

        boolean allowExternal = profileService.getBooleanSetting(user.getId(), scope, "policy.allow_external_write", false);
        if (!target.startsWith(workspace) && !allowExternal) {
            throw new IllegalArgumentException("Write target outside workspace is blocked. Set policy.allow_external_write=true to allow.");
        }

        Path parent = target.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        if (Files.exists(target) && !overwrite) {
            throw new IllegalArgumentException("Target file exists. Pass --overwrite to replace: " + target);
        }

        if (Files.exists(target) && overwrite) {
            Path packDir = resolvePath("target/change-packs").resolve(activeTraceId);
            if (!Files.exists(packDir)) {
                Files.createDirectories(packDir);
            }
            String safeName = target.getFileName() == null ? "file" : target.getFileName().toString();
            Path backupPath = packDir.resolve(safeName + ".bak");
            Files.writeString(backupPath, Files.readString(target, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            String meta = "traceId=" + activeTraceId + "\noriginal=" + target + "\ncreatedAt=" + LocalDateTime.now() + "\n";
            Files.writeString(packDir.resolve(safeName + ".meta"), meta, StandardCharsets.UTF_8);
            TerminalUtils.printInfo("Change pack backup created: " + backupPath);
        }

        Files.writeString(target, content == null ? "" : content, StandardCharsets.UTF_8);
    }

    private Path resolvePath(String rawPath) {
        Path path = Paths.get(rawPath);
        if (!path.isAbsolute()) {
            return Paths.get(System.getProperty("user.dir")).resolve(path).normalize();
        }
        return path.normalize();
    }

    private boolean parseBooleanFlag(Map<String, String> flags, String key, boolean fallback) {
        String raw = flags.get(key);
        if (raw == null || raw.isBlank()) return fallback;
        return parseBooleanToken(raw);
    }

    private boolean parseBooleanToken(String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("true") || normalized.equals("1") || normalized.equals("yes") || normalized.equals("on") || normalized.equals("allow");
    }

    private String joinParts(String[] parts, int startIdx) {
        if (startIdx >= parts.length) return "";
        StringBuilder sb = new StringBuilder(parts[startIdx]);
        for (int i = startIdx + 1; i < parts.length; i++) {
            sb.append("|").append(parts[i]);
        }
        return sb.toString().trim();
    }

    private String resolveRecipeScope(String raw) {
        String normalized = raw == null ? "project" : raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("project")) return profileService.currentWorkspaceScope();
        if (normalized.equals("global")) return ProfileService.GLOBAL_SCOPE;
        return normalized;
    }

    private com.nexus.domain.MemoryType parseMemoryType(String raw) {
        try {
            return com.nexus.domain.MemoryType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid --type. Use one of: " + Arrays.toString(com.nexus.domain.MemoryType.values()));
        }
    }

    private int parseIntOrDefault(String raw, int fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String resolveMemoryScope(com.nexus.service.MemoryService memoryService, Map<String, String> flags, String scopeRaw) {
        String normalized = scopeRaw == null ? "project" : scopeRaw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "global" -> com.nexus.service.MemoryService.GLOBAL_SCOPE;
            case "project" -> {
                String explicitProject = flags.get("--project");
                yield explicitProject == null || explicitProject.isBlank()
                    ? memoryService.currentWorkspaceScope()
                    : explicitProject.trim().toLowerCase(Locale.ROOT);
            }
            default -> normalized;
        };
    }

    private void printSessionHelp() {
        System.out.println("Usage:");
        System.out.println("  nexus session list --user <username>");
        System.out.println("  nexus session start --user <username> --task CODE_GENERATION [--note \"...\"]");
        System.out.println("  nexus session close --user <username> --id <sessionId> --input <n> --output <n> --quality <0..1> [--note \"...\"]");
    }

    private void printFinanceHelp() {
        System.out.println("Usage:");
        System.out.println("  nexus finance report --user <username> [--range all|7d|30d] [--quality-threshold 0.70]");
    }
}
