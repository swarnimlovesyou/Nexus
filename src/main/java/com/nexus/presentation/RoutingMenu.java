package com.nexus.presentation;

import com.nexus.domain.*;
import com.nexus.service.RoutingEngine;
import com.nexus.util.TerminalUtils;

import java.util.List;

/**
 * Routing Engine menu — route tasks, explain decisions, what-if, record outcomes.
 * Includes the outcome→memory bridge (auto-EPISODE creation).
 */
public class RoutingMenu {
    private final MenuContext ctx;

    public RoutingMenu(MenuContext ctx) { this.ctx = ctx; }

    public void show() {
        TerminalUtils.printHeader("Intelligent Routing Engine");
        System.out.println("  " + TerminalUtils.AMBER + "1" + TerminalUtils.RESET + "  Route a Task (select best model)");
        System.out.println("  " + TerminalUtils.AMBER + "2" + TerminalUtils.RESET + "  Explain Routing  " + TerminalUtils.GRAY + "(score breakdown per model)" + TerminalUtils.RESET);
        System.out.println("  " + TerminalUtils.AMBER + "3" + TerminalUtils.RESET + "  What-If Analysis  " + TerminalUtils.GRAY + "(different budget caps)" + TerminalUtils.RESET);
        System.out.println("  " + TerminalUtils.AMBER + "4" + TerminalUtils.RESET + "  Record Execution Outcome");
        System.out.println("  " + TerminalUtils.AMBER + "5" + TerminalUtils.RESET + "  Test Live LLM Call  " + TerminalUtils.GRAY + "(real HTTP when available)" + TerminalUtils.RESET);
        System.out.println("  " + TerminalUtils.AMBER + "6" + TerminalUtils.RESET + "  Start Session Context");
        System.out.println("  " + TerminalUtils.AMBER + "7" + TerminalUtils.RESET + "  Close Active Session");
        System.out.println("  " + TerminalUtils.AMBER + "8" + TerminalUtils.RESET + "  View My Sessions");
        System.out.println("  " + TerminalUtils.AMBER + "B" + TerminalUtils.RESET + "  Back");
        System.out.println();
        TerminalUtils.printPrompt(ctx.username());
        switch (ctx.scanner().nextLine().trim().toUpperCase()) {
            case "1" -> routeTask();
            case "2" -> explainRouting();
            case "3" -> whatIfAnalysis();
            case "4" -> recordOutcome();
            case "5" -> testLiveCall();
            case "6" -> startSession();
            case "7" -> closeSession();
            case "8" -> viewSessions();
        }
    }

    private void routeTask() {
        TerminalUtils.printSeparator("TASK SELECTION");
        TaskType task = ctx.pickTask();
        TerminalUtils.spinner("Analysing " + task + " performance data...", 900);

        LlmModel best = ctx.routingEngine().selectOptimalModelForUser(task, Double.MAX_VALUE, ctx.userId());
        if (best == null) { TerminalUtils.printError("No suitable model found."); return; }

        System.out.println();
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
            Memory episodeMem = ctx.memoryService().store(ctx.userId(), episodeContent, episodeTags, MemoryType.EPISODE, 90);
            TerminalUtils.printSuccess("EPISODE memory #" + episodeMem.getId() + " created in vault.");
        }
    }

    private void testLiveCall() {
        TerminalUtils.printSeparator("TEST LIVE LLM CALL");
        TaskType task = ctx.pickTask();
        System.out.print("  Enter prompt: ");
        String prompt = ctx.scanner().nextLine().trim();
        if (prompt.isEmpty()) { TerminalUtils.printError("Prompt cannot be empty."); return; }

        TerminalUtils.spinner("Routing prompt to optimal model...", 600);
        LlmModel best = ctx.routingEngine().selectOptimalModelForUser(task, Double.MAX_VALUE, ctx.userId());
        if (best == null) { TerminalUtils.printError("No suitable model found to route this task."); return; }
        
        System.out.println("  " + TerminalUtils.GOLD + "Router selected: " + TerminalUtils.RESET + best.getName() + " (" + best.getProvider() + ")");
        System.out.println();
        
        try {
            com.nexus.service.LlmCallService.LlmCallResult resp = ctx.llmCallService().executeCall(ctx.userId(), best, prompt);
            
            TerminalUtils.printBox("LLM RESPONSE", resp.content());
            
            System.out.println();
            TerminalUtils.printInfo(String.format("Latency: %dms | Tokens: In=%d, Out=%d | Actual Cost: $%.6f", 
                resp.latencyMs(), resp.inputTokens(), resp.outputTokens(), resp.costUsd()));
            TerminalUtils.printInfo("Execution mode: " + (resp.simulated() ? "SIMULATED" : "REAL") + "  (" + resp.mode() + ")");
                
            // Auto commit outcome
            OutcomeMemory rec = new OutcomeMemory(null, ctx.userId(), best.getId(),
                task, resp.costUsd(), (int)resp.latencyMs(), resp.simulated() ? 0.70 : 0.90, null);
            ctx.outcomeDao().create(rec);
            TerminalUtils.printSuccess("Outcome automatically recorded to telemetry loop.");
            
        } catch (Exception e) {
            TerminalUtils.printError("Call failed: " + e.getMessage());
        }
    }

    private void startSession() {
        TerminalUtils.printSeparator("START SESSION CONTEXT");
        TaskType task = ctx.pickTask();
        LlmModel best = ctx.routingEngine().selectOptimalModelForUser(task, Double.MAX_VALUE, ctx.userId());
        if (best == null) {
            TerminalUtils.printError("No routed model available for this task.");
            return;
        }

        System.out.println("  Routed model: " + TerminalUtils.GOLD + best.getName() + TerminalUtils.RESET + " (" + best.getProvider() + ")");
        System.out.print("  Session note (optional): ");
        String note = ctx.scanner().nextLine().trim();

        AgentSession session = ctx.sessionService().startSession(ctx.userId(), task, best.getId(), note);
        TerminalUtils.printSuccess("Session started: #" + session.getId() + "  task=" + task + "  model=" + best.getName());
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
        ctx.memoryService().store(ctx.userId(), episode, "session," + closed.getTaskType().name().toLowerCase(), MemoryType.EPISODE, 90);
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
}
