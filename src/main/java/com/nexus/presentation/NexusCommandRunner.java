package com.nexus.presentation;

import java.io.Console;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import com.nexus.dao.LlmModelDao;
import com.nexus.dao.OutcomeMemoryDao;
import com.nexus.dao.SuitabilityDao;
import com.nexus.domain.AgentSession;
import com.nexus.domain.LlmModel;
import com.nexus.domain.ModelSuitability;
import com.nexus.domain.OutcomeMemory;
import com.nexus.domain.TaskType;
import com.nexus.domain.User;
import com.nexus.exception.DaoException;
import com.nexus.service.RoutingEngine;
import com.nexus.service.SessionService;
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
    private final Scanner scanner;

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
        this.scanner = scanner;
    }

    public static boolean tryRun(String[] args) {
        if (args == null || args.length == 0) return false;

        String top = args[0].toLowerCase(Locale.ROOT);
        if ("start".equals(top)) return false; // interactive mode

        NexusCommandRunner runner = new NexusCommandRunner();
        return runner.dispatch(args);
    }

    private boolean dispatch(String[] args) {
        String top = args[0].toLowerCase(Locale.ROOT);
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
                case "help", "--help", "-h" -> {
                    printCommandHelp();
                    yield true;
                }
                default -> false;
            };
        } catch (DaoException e) {
            TerminalUtils.printError("Command failed due to a database issue. Database operation failed; no changes were saved.");
            return true;
        } catch (Exception e) {
            TerminalUtils.printError(e.getMessage());
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
            throw new IllegalArgumentException("No routable model found for task: " + task);
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
            pass = readPassword("Password: ");
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
        return readLine(prompt + " (visible): ");
    }

    private void printCommandHelp() {
        TerminalUtils.printHelp();
        System.out.println("  Additional command-mode flows:");
        System.out.println("    nexus session list --user <username>");
        System.out.println("    nexus session start --user <username> --task CODE_GENERATION [--note \"...\"]");
        System.out.println("    nexus session close --user <username> --id <sessionId> --input <n> --output <n> --quality <0..1>");
        System.out.println("    nexus finance report --user <username> [--range all|7d|30d]");
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
