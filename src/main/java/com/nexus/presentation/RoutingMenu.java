package com.nexus.presentation;

import java.util.List;

import com.nexus.domain.AgentSession;
import com.nexus.domain.LlmModel;
import com.nexus.domain.Memory;
import com.nexus.domain.MemoryType;
import com.nexus.domain.OutcomeMemory;
import com.nexus.domain.TaskType;
import com.nexus.exception.DaoException;
import com.nexus.service.InteractiveChatService;
import com.nexus.service.RoutingEngine;
import com.nexus.util.TerminalUtils;

/**
 * Routing Engine menu — route tasks, explain decisions, what-if, record outcomes.
 * Includes the outcome→memory bridge (auto-EPISODE creation).
 */
public class RoutingMenu {
    private final MenuContext ctx;

    public RoutingMenu(MenuContext ctx) { this.ctx = ctx; }

    public void show() {
        while (true) {
            TerminalUtils.printHeader("Intelligent Routing Engine");
            System.out.println("  " + TerminalUtils.AMBER + "1" + TerminalUtils.RESET + "  Route a Task (select best model)");
            System.out.println("  " + TerminalUtils.AMBER + "2" + TerminalUtils.RESET + "  Explain Routing  " + TerminalUtils.GRAY + "(score breakdown per model)" + TerminalUtils.RESET);
            System.out.println("  " + TerminalUtils.AMBER + "3" + TerminalUtils.RESET + "  What-If Analysis  " + TerminalUtils.GRAY + "(different budget caps)" + TerminalUtils.RESET);
            System.out.println("  " + TerminalUtils.AMBER + "4" + TerminalUtils.RESET + "  Record Execution Outcome");
            System.out.println("  " + TerminalUtils.AMBER + "5" + TerminalUtils.RESET + "  Test Live LLM Call  " + TerminalUtils.GRAY + "(real HTTP when available)" + TerminalUtils.RESET);
            System.out.println("  " + TerminalUtils.AMBER + "6" + TerminalUtils.RESET + "  Start Coding Session  " + TerminalUtils.GRAY + "(interactive chat + file access)" + TerminalUtils.RESET);
            System.out.println("  " + TerminalUtils.AMBER + "7" + TerminalUtils.RESET + "  Close Active Session");
            System.out.println("  " + TerminalUtils.AMBER + "8" + TerminalUtils.RESET + "  View My Sessions");
            System.out.println("  " + TerminalUtils.AMBER + "9" + TerminalUtils.RESET + "  Decompose & Execute Plan  " + TerminalUtils.GRAY + "(agentic split)" + TerminalUtils.RESET);
            System.out.println("  " + TerminalUtils.AMBER + "C" + TerminalUtils.RESET + "  Manual Auto-calibrate Engine");
            System.out.println("  " + TerminalUtils.AMBER + "B" + TerminalUtils.RESET + "  Back");
            System.out.println();
            TerminalUtils.printPrompt(ctx.username());
            switch (ctx.scanner().nextLine().trim().toUpperCase()) {
                case "1" -> ctx.runWithDaoGuard("Unable to route task right now. Please try again.", this::routeTask);
                case "2" -> ctx.runWithDaoGuard("Unable to explain routing right now. Please try again.", this::explainRouting);
                case "3" -> ctx.runWithDaoGuard("Unable to run what-if analysis right now. Please try again.", this::whatIfAnalysis);
                case "4" -> ctx.runWithDaoGuard("Could not record outcome. Database operation failed; no changes were saved.", this::recordOutcome);
                case "5" -> ctx.runWithDaoGuard("Unable to complete live call flow right now due to a database issue.", this::testLiveCall);
                case "6" -> ctx.runWithDaoGuard("Could not start session. Database operation failed; no changes were saved.", this::startSession);
                case "7" -> ctx.runWithDaoGuard("Could not close session. Database operation failed; no changes were saved.", this::closeSession);
                case "8" -> ctx.runWithDaoGuard("Unable to load sessions right now. Please try again.", this::viewSessions);
                case "9" -> ctx.runWithDaoGuard("Failed to execute decomposed plan.", this::decomposeAndExecute);
                case "C" -> ctx.runWithDaoGuard("Calibration failed.", this::manualCalibrate);
                case "B" -> { return; }
                default  -> TerminalUtils.printError("Unknown option.");
            }
        }
    }

    private void routeTask() {
        TerminalUtils.printSeparator("TASK SELECTION");
        TaskType task = ctx.pickTask();
        TerminalUtils.spinner("Analysing " + task + " performance data...", 900);

        RoutingEngine.RoutingResult result = ctx.routingEngine().selectWithResult(task, Double.MAX_VALUE, ctx.userId());
        if (result.noModelAvailable()) { TerminalUtils.printError("No suitable model found."); return; }

        LlmModel best = result.recommended();

        System.out.println();
        TerminalUtils.printTopology();

        if (result.keyMissing()) {
            LlmModel optimal = result.optimalWithoutKey();
            TerminalUtils.printWarn("Optimal model is " + TerminalUtils.BOLD + optimal.getName()
                + TerminalUtils.RESET + " (" + optimal.getProvider() + ") but you have no API key for it.");
            if (best != null) {
                TerminalUtils.printInfo("Using best available model with a key: " + best.getName() + " (" + best.getProvider() + ")");
            } else {
                TerminalUtils.printWarn("No key-accessible model found. Add a key in the API Key Vault.");
                return;
            }
        }

        // best is guaranteed non-null here
        TerminalUtils.printBox("ROUTING VERDICT", String.join("\n",
            TerminalUtils.BOLD + "Model    " + TerminalUtils.RESET + best.getName(),
            TerminalUtils.BOLD + "Provider " + TerminalUtils.RESET + best.getProvider(),
            TerminalUtils.BOLD + "Task     " + TerminalUtils.RESET + task.name(),
            TerminalUtils.BOLD + "Cost     " + TerminalUtils.RESET + "$" + String.format("%.5f", best.getCostPer1kTokens()) + " / 1k tokens"
        ));
    }

    private void explainRouting() {
        TerminalUtils.printSeparator("ROUTING EXPLANATION");
        TaskType task = ctx.pickTask();
        TerminalUtils.spinner("Computing 4-signal composite scores...", 800);

        List<RoutingEngine.ModelScoreBreakdown> breakdown = ctx.routingEngine().explainRoutingForUser(task, ctx.userId());
        if (breakdown.isEmpty()) { TerminalUtils.printError("No models configured for " + task); return; }

        System.out.println();
        String[] headers = {"Rank", "Model", "Provider", "Suitability", "Quality", "Latency", "Cost Eff.", "Composite", "Key?"};
        String[][] rows  = new String[breakdown.size()][9];
        for (int i = 0; i < breakdown.size(); i++) {
            var b = breakdown.get(i);
            String rankMark = i == 0 ? TerminalUtils.GOLD + "★ 1" + TerminalUtils.RESET : String.valueOf(i + 1);
            String keyMark  = b.hasApiKey() ? TerminalUtils.GREEN + "✔" + TerminalUtils.RESET
                                            : TerminalUtils.RED   + "✖" + TerminalUtils.RESET;
            rows[i] = new String[]{
                rankMark, b.model().getName(), b.model().getProvider(),
                String.format("%.2f", b.suitabilityScore()),
                String.format("%.2f", b.qualityScore()),
                String.format("%.2f", b.latencyScore()),
                String.format("%.2f", b.costScore()),
                TerminalUtils.AMBER + String.format("%.3f", b.composite()) + TerminalUtils.RESET,
                keyMark
            };
        }
        TerminalUtils.printTable(headers, rows);
        System.out.println();
        
        // Show Visual Distribution
        java.util.LinkedHashMap<String, Double> chartData = new java.util.LinkedHashMap<>();
        for (var b : breakdown) chartData.put(b.model().getName(), b.composite());
        TerminalUtils.printHorizontalChart("Composite Score Distribution", chartData);
        
        System.out.println();
        TerminalUtils.printInfo("Weights: Suitability×35%  Quality×30%  Latency×15%  Cost×20%");
        TerminalUtils.printInfo("Key? = Whether you have an API key stored for that provider.");
    }

    private void whatIfAnalysis() {
        TerminalUtils.printSeparator("WHAT-IF ANALYSIS");
        TaskType task = ctx.pickTask();
        double[] tiers = {0.00015, 0.001, 0.003, 0.005, Double.MAX_VALUE};
        TerminalUtils.spinner("Simulating budget tiers...", 700);
        System.out.println();
        List<String> results = ctx.routingEngine().whatIfBudget(task, tiers);
        String[] headers = {"Budget Cap / 1k", "Selected Model"};
        String[][] rows  = new String[results.size()][2];
        String[] labels  = {"$0.00015", "$0.001", "$0.003", "$0.005", "Unlimited"};
        for (int i = 0; i < results.size(); i++) {
            String[] parts = results.get(i).split("→");
            rows[i] = new String[]{ TerminalUtils.GRAY + labels[i] + TerminalUtils.RESET, parts.length > 1 ? parts[1].trim() : "N/A" };
        }
        TerminalUtils.printTable(headers, rows);
    }

    private void recordOutcome() {
        TerminalUtils.printSeparator("RECORD EXECUTION OUTCOME");
        List<LlmModel> models = ctx.modelDao().findAll();
        if (models.isEmpty()) { TerminalUtils.printError("No models registered."); return; }
        for (int i = 0; i < models.size(); i++)
            System.out.printf("  " + TerminalUtils.AMBER + "%d" + TerminalUtils.RESET + "  %s (%s) - $%.5f/1k%n",
                i+1, models.get(i).getName(), models.get(i).getProvider(), models.get(i).getCostPer1kTokens());
        System.out.print("  Model # (1-" + models.size() + "): ");
        int midx = ctx.safeInt(ctx.scanner().nextLine()) - 1;
        if (midx < 0 || midx >= models.size()) { TerminalUtils.printError("Invalid model selection."); return; }
        LlmModel model = models.get(midx);

        TaskType task = ctx.pickTask();
        System.out.print("  Quality score (0.0–1.0): ");
        double quality = ctx.safeDouble(ctx.scanner().nextLine());
        if (Double.isNaN(quality) || quality < 0 || quality > 1) {
            TerminalUtils.printError("Quality must be between 0.0 and 1.0."); return;
        }
        System.out.print("  Latency (ms): ");
        int latency = ctx.safeInt(ctx.scanner().nextLine());
        if (latency < 0) { TerminalUtils.printError("Latency must be a positive integer."); return; }
        System.out.print("  Approximate tokens used (e.g. 500): ");
        int tokens = ctx.safeInt(ctx.scanner().nextLine());
        if (tokens <= 0) tokens = 500;

        double actualCost = model.getCostPer1kTokens() * (tokens / 1000.0);
        OutcomeMemory rec = new OutcomeMemory(null, ctx.userId(), model.getId(),
            task, actualCost, latency, quality, null);
        ctx.outcomeDao().create(rec);
        TerminalUtils.printSuccess(String.format("Outcome committed. Actual cost: $%.7f  Router intelligence updated.", actualCost));

        // ── Outcome → Memory Bridge ──────────────────────────────────
        System.out.println();
        System.out.print("  Auto-create EPISODE memory from this outcome? (yes/no): ");
        if ("yes".equalsIgnoreCase(ctx.scanner().nextLine().trim())) {
            String episodeContent = String.format(
                "Used %s for %s: quality=%.2f, latency=%dms, cost=$%.7f",
                model.getName(), task.name(), quality, latency, actualCost
            );
            String episodeTags = model.getName().toLowerCase() + "," + task.name().toLowerCase();
            try {
                Memory episodeMem = ctx.memoryService().store(ctx.userId(), episodeContent, episodeTags, MemoryType.EPISODE, 90);
                TerminalUtils.printSuccess("EPISODE memory #" + episodeMem.getId() + " created in vault.");
            } catch (DaoException e) {
                TerminalUtils.printWarn("Outcome was recorded, but EPISODE memory could not be saved right now.");
            }
        }
    }

    private void testLiveCall() {
        TerminalUtils.printSeparator("TEST LIVE LLM CALL");
        TaskType task = ctx.pickTask();
        System.out.print("  Enter prompt: ");
        String prompt = ctx.scanner().nextLine().trim();
        if (prompt.isEmpty()) { TerminalUtils.printError("Prompt cannot be empty."); return; }

        TerminalUtils.spinner("Routing prompt to optimal model...", 600);
        RoutingEngine.RoutingResult result = ctx.routingEngine().selectWithResult(task, Double.MAX_VALUE, ctx.userId());

        if (result.noModelAvailable()) { TerminalUtils.printError("No suitable model found to route this task."); return; }

        LlmModel best = result.recommended();

        if (result.keyMissing()) {
            LlmModel optimal = result.optimalWithoutKey();
            TerminalUtils.printWarn("Optimal model " + TerminalUtils.BOLD + optimal.getName()
                + TerminalUtils.RESET + " (" + optimal.getProvider() + ") has no API key configured.");

            if (best != null) {
                // We have a key-accessible fallback — use it silently
                TerminalUtils.printInfo("Switching to: " + best.getName() + " (" + best.getProvider() + ")");
            } else {
                // No key for any model — offer simulation on the optimal model
                System.out.println();
                System.out.print("  No keys configured. Run in SIMULATION mode using "
                    + optimal.getName() + "? (yes/no): ");
                String choice = ctx.scanner().nextLine().trim();
                if (!"yes".equalsIgnoreCase(choice)) {
                    TerminalUtils.printInfo("Cancelled. Add a key via option 3 in the API Key Vault.");
                    return;
                }
                // Run simulation on the optimal (no-key) model
                runSimulatedCall(task, optimal, prompt);
                return;
            }
        }

        // best is non-null here — attempt a real call with key-accessible model
        System.out.println("  " + TerminalUtils.GOLD + "Router selected: " + TerminalUtils.RESET + best.getName() + " (" + best.getProvider() + ")");
        System.out.println();

        try {
            com.nexus.service.LlmCallService.LlmCallResult resp = ctx.llmCallService().executeCall(ctx.userId(), best, prompt);
            TerminalUtils.printBox("LLM RESPONSE", resp.content());
            System.out.println();
            TerminalUtils.printInfo(String.format("Latency: %dms | Tokens: In=%d, Out=%d | Actual Cost: $%.6f",
                resp.latencyMs(), resp.inputTokens(), resp.outputTokens(), resp.costUsd()));
            TerminalUtils.printInfo("Execution mode: " + (resp.simulated() ? "SIMULATED" : "REAL") + "  (" + resp.mode() + ")");
            OutcomeMemory rec = new OutcomeMemory(null, ctx.userId(), best.getId(),
                task, resp.costUsd(), (int)resp.latencyMs(), resp.simulated() ? 0.70 : 0.90, null);
            ctx.outcomeDao().create(rec);
            TerminalUtils.printSuccess("Outcome automatically recorded to telemetry loop.");
        } catch (DaoException e) {
            TerminalUtils.printError("Live call finished, but telemetry could not be saved. Please retry later.");
        } catch (Exception e) {
            TerminalUtils.printError("Call failed: " + e.getMessage());
        }
    }

    /** Runs a simulation call and records the outcome — used when no API key is available. */
    private void runSimulatedCall(TaskType task, LlmModel model, String prompt) {
        TerminalUtils.spinner("Simulating call to " + model.getName() + "...", 700);
        try {
            com.nexus.service.LlmCallService.LlmCallResult resp = ctx.llmCallService().executeSimulated(model, prompt);
            TerminalUtils.printBox("SIMULATED RESPONSE", resp.content());
            System.out.println();
            TerminalUtils.printInfo(String.format("Latency: %dms | Est. Tokens: In=%d, Out=%d | Est. Cost: $%.6f",
                resp.latencyMs(), resp.inputTokens(), resp.outputTokens(), resp.costUsd()));
            TerminalUtils.printInfo("Execution mode: SIMULATED  (no API key — add one via option 3)");
            OutcomeMemory rec = new OutcomeMemory(null, ctx.userId(), model.getId(),
                task, resp.costUsd(), (int)resp.latencyMs(), 0.50, null);
            ctx.outcomeDao().create(rec);
            TerminalUtils.printSuccess("Simulated outcome recorded. Quality capped at 0.50 to mark synthetic telemetry.");
        } catch (Exception e) {
            TerminalUtils.printError("Simulation failed: " + e.getMessage());
        }
    }

    private void startSession() {
        TerminalUtils.printSeparator("START CODING SESSION");
        TaskType task = ctx.pickTask();
        RoutingEngine.RoutingResult result = ctx.routingEngine().selectWithResult(task, Double.MAX_VALUE, ctx.userId());

        if (result.noModelAvailable()) {
            TerminalUtils.printError("No routed model available for this task.");
            return;
        }

        LlmModel best = result.recommended();

        if (result.keyMissing()) {
            LlmModel optimal = result.optimalWithoutKey();
            TerminalUtils.printWarn("Optimal model " + optimal.getName() + " (" + optimal.getProvider() + ") has no API key.");
            if (best == null) {
                // Offer simulation session — no real API call possible
                System.out.print("  No key-accessible model found. Start a SIMULATION session with "
                    + optimal.getName() + "? (yes/no): ");
                if (!"yes".equalsIgnoreCase(ctx.scanner().nextLine().trim())) {
                    TerminalUtils.printInfo("Cancelled. Add a key via the API Key Vault first.");
                    return;
                }
                best = optimal; // Use optimal model in simulation
            } else {
                TerminalUtils.printInfo("Session will use: " + best.getName() + " (" + best.getProvider() + ")");
            }
        }

        // Create session record
        AgentSession session = ctx.sessionService().startSession(ctx.userId(), task, best.getId(), "");
        TerminalUtils.printSuccess("Session #" + session.getId() + " opened. Chat started.");

        // Launch interactive chat — blocks until user types /exit
        InteractiveChatService chat = new InteractiveChatService(
            ctx.llmCallService(), ctx.profileService(), ctx.scanner());
        InteractiveChatService.ChatResult chatResult = chat.run(
            ctx.userId(), session.getId(), best, task);

        // Auto-close session with accumulated data
        try {
            AgentSession closed = ctx.sessionService().closeSession(
                ctx.userId(), session.getId(),
                chatResult.totalInputTokens(), chatResult.totalOutputTokens(),
                chatResult.qualityScore(),
                "Interactive chat — " + chatResult.turns() + " turn(s)");
            System.out.println();
            TerminalUtils.printSuccess(String.format(
                "Session #%d closed and saved. Total cost: $%.7f | Quality: %.2f | Turns: %d",
                session.getId(), closed.getTotalCost(),
                closed.getQualityScore(), chatResult.turns()));

            // Auto-create EPISODE memory for significant sessions (≥2 turns)
            if (chatResult.turns() >= 2) {
                String content = String.format(
                    "Coding session with %s on %s: %d turns, cost=$%.7f, quality=%.2f",
                    best.getName(), task.name(), chatResult.turns(),
                    closed.getTotalCost(), closed.getQualityScore());
                String tags = best.getName().toLowerCase() + "," + task.name().toLowerCase() + ",session";
                try {
                    Memory mem = ctx.memoryService().store(ctx.userId(), content, tags, MemoryType.EPISODE, 90);
                    TerminalUtils.printSuccess("EPISODE memory #" + mem.getId() + " created in vault.");
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            TerminalUtils.printError("Session record could not be saved: " + e.getMessage());
        }
    }

    private void closeSession() {
        TerminalUtils.printSeparator("CLOSE ACTIVE SESSION");
        List<AgentSession> active = ctx.sessionService().listActiveSessions(ctx.userId());
        if (active.isEmpty()) {
            TerminalUtils.printInfo("No active session found. Start one first.");
            return;
        }

        List<LlmModel> models = ctx.modelDao().findAll();
        for (AgentSession s : active) {
            String modelName = models.stream()
                .filter(m -> m.getId().equals(s.getModelId()))
                .map(LlmModel::getName)
                .findFirst().orElse("model#" + s.getModelId());
            System.out.printf("  %s#%d%s  task=%s  model=%s  started=%s%n",
                TerminalUtils.AMBER, s.getId(), TerminalUtils.RESET,
                s.getTaskType(), modelName, s.getCreatedAt().toLocalTime());
        }

        System.out.print("  Session ID to close: ");
        int sid = ctx.safeInt(ctx.scanner().nextLine());
        if (sid <= 0) { TerminalUtils.printError("Invalid session id."); return; }

        System.out.print("  Total input tokens: ");
        int input = ctx.safeInt(ctx.scanner().nextLine());
        System.out.print("  Total output tokens: ");
        int output = ctx.safeInt(ctx.scanner().nextLine());
        System.out.print("  Quality score (0.0-1.0): ");
        double quality = ctx.safeDouble(ctx.scanner().nextLine());
        System.out.print("  Closing notes (optional): ");
        String closeNotes = ctx.scanner().nextLine().trim();

        AgentSession closed = ctx.sessionService().closeSession(ctx.userId(), sid, input, output, quality, closeNotes);
        TerminalUtils.printSuccess(String.format("Session #%d closed. Cost=$%.6f, quality=%.2f", sid, closed.getTotalCost(), closed.getQualityScore()));

        String episode = String.format(
            "Session #%d closed for %s using modelId=%d. tokens_in=%d tokens_out=%d quality=%.2f cost=$%.6f",
            sid, closed.getTaskType(), closed.getModelId(),
            closed.getInputTokens(), closed.getOutputTokens(), closed.getQualityScore(), closed.getTotalCost()
        );
        try {
            ctx.memoryService().store(ctx.userId(), episode, "session," + closed.getTaskType().name().toLowerCase(), MemoryType.EPISODE, 90);
        } catch (DaoException e) {
            TerminalUtils.printWarn("Session was closed, but EPISODE memory could not be saved right now.");
        }
    }

    private void viewSessions() {
        TerminalUtils.printSeparator("MY SESSIONS");
        List<AgentSession> sessions = ctx.sessionService().listUserSessions(ctx.userId());
        if (sessions.isEmpty()) {
            TerminalUtils.printInfo("No sessions yet.");
            return;
        }

        List<LlmModel> models = ctx.modelDao().findAll();
        int limit = Math.min(15, sessions.size());
        String[] headers = {"ID", "Status", "Task", "Model", "Cost", "Quality", "Started", "Ended"};
        String[][] rows = new String[limit][8];
        for (int i = 0; i < limit; i++) {
            AgentSession s = sessions.get(i);
            String modelName = models.stream()
                .filter(m -> m.getId().equals(s.getModelId()))
                .map(LlmModel::getName)
                .findFirst().orElse("model#" + s.getModelId());

            rows[i] = new String[]{
                String.valueOf(s.getId()),
                s.isActive() ? TerminalUtils.YELLOW + "ACTIVE" + TerminalUtils.RESET : TerminalUtils.GREEN + "CLOSED" + TerminalUtils.RESET,
                s.getTaskType().name(),
                modelName,
                s.getTotalCost() == null ? "-" : String.format("$%.6f", s.getTotalCost()),
                s.getQualityScore() == null ? "-" : String.format("%.2f", s.getQualityScore()),
                s.getCreatedAt().toLocalDate().toString(),
                s.getEndedAt() == null ? "-" : s.getEndedAt().toLocalDate().toString()
            };
        }
        TerminalUtils.printTable(headers, rows);
        if (sessions.size() > limit) TerminalUtils.printInfo("Showing latest " + limit + " of " + sessions.size() + " sessions.");
    }

    private void decomposeAndExecute() {
        TerminalUtils.printSeparator("AGENTIC DECOMPOSITION & EXECUTION");
        System.out.print("  Enter complex prompt: ");
        String prompt = ctx.scanner().nextLine().trim();
        if (prompt.isEmpty()) return;

        TerminalUtils.spinner("Planning task decomposition...", 700);
        com.nexus.service.TaskPlannerService planner = new com.nexus.service.TaskPlannerService();
        List<com.nexus.service.TaskPlannerService.PlannedTask> plan = planner.plan(prompt);

        System.out.println("  " + TerminalUtils.GOLD + "Decomposition Plan:" + TerminalUtils.RESET);
        for (int i = 0; i < plan.size(); i++) {
            System.out.printf("    %d. [%s] %s%n", i + 1, plan.get(i).type(), plan.get(i).prompt().substring(0, Math.min(40, plan.get(i).prompt().length())) + "...");
        }
        System.out.println();
        System.out.print("  Execute this plan? (yes/no): ");
        if (!"yes".equalsIgnoreCase(ctx.scanner().nextLine().trim())) return;

        int step = 1;
        for (var task : plan) {
            TerminalUtils.printSeparator("STEP " + step + ": " + task.type());
            RoutingEngine.RoutingResult result = ctx.routingEngine().selectWithResult(task.type(), Double.MAX_VALUE, ctx.userId());
            if (result.noModelAvailable()) {
                TerminalUtils.printError("Routing failed for " + task.type());
                continue;
            }
            LlmModel best = result.recommended();
            if (result.keyMissing()) {
                TerminalUtils.printWarn("Optimal model " + result.optimalWithoutKey().getName() + " has no key. Using " + (best != null ? best.getName() : "none") + " instead.");
            }
            if (best == null) {
                TerminalUtils.printError("No key-accessible model for step " + step + ". Skipping.");
                step++;
                continue;
            }
            System.out.println("  " + TerminalUtils.GRAY + "Routed to: " + TerminalUtils.RESET + best.getName() + " (" + best.getProvider() + ")");
            
            try {
                var resp = ctx.llmCallService().executeCall(ctx.userId(), best, task.prompt());
                TerminalUtils.printBox("STEP " + step + " OUTPUT", resp.content());
                
                // Track outcome
                OutcomeMemory rec = new OutcomeMemory(null, ctx.userId(), best.getId(),
                    task.type(), resp.costUsd(), (int)resp.latencyMs(), 0.9, null);
                ctx.outcomeDao().create(rec);
            } catch (Exception e) {
                TerminalUtils.printError("Execution failed: " + e.getMessage());
            }
            step++;
        }
        TerminalUtils.printSuccess("Agentic plan execution completed.");
    }

    private void manualCalibrate() {
        TerminalUtils.printSeparator("MANUAL AUTO-CALIBRATION");
        TerminalUtils.spinner("Analyzing performance history and updating suitability weights...", 1200);
        int updated = ctx.routingEngine().recalibrateScores(ctx.userId());
        if (updated > 0) {
            TerminalUtils.printSuccess("Recalibration complete. " + updated + " model suitability profiles optimized.");
        } else {
            TerminalUtils.printInfo("Calibration finished. All weights are currently optimal for your history.");
        }
    }
}
