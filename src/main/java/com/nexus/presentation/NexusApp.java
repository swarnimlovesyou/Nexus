package com.nexus.presentation;

import com.nexus.dao.AuditLogDao;
import com.nexus.dao.LlmModelDao;
import com.nexus.dao.OutcomeMemoryDao;
import com.nexus.dao.SuitabilityDao;
import com.nexus.domain.*;
import com.nexus.exception.NexusException;
import com.nexus.service.*;
import com.nexus.util.TerminalUtils;

import java.util.*;

public class NexusApp {
    private final Scanner        scanner;
    private final UserService    userService;
    private final RoutingEngine  routingEngine;
    private final MemoryService  memoryService;
    private final ApiKeyService  apiKeyService;
    private final LlmModelDao    modelDao;
    private final SuitabilityDao suitabilityDao;
    private final OutcomeMemoryDao outcomeDao;
    private final AuditLogDao    auditLogDao;

    private User loggedInUser = null;

    public NexusApp() {
        this.scanner        = new Scanner(System.in);
        this.userService    = new UserService();
        this.routingEngine  = new RoutingEngine();
        this.memoryService  = new MemoryService();
        this.apiKeyService  = new ApiKeyService();
        this.modelDao       = new LlmModelDao();
        this.suitabilityDao = new SuitabilityDao();
        this.outcomeDao     = new OutcomeMemoryDao();
        this.auditLogDao    = new AuditLogDao();
    }

    public static void main(String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("start")) {
            TerminalUtils.printHelp();
            return;
        }
        NexusApp app = new NexusApp();
        app.seedInitialData();
        app.run();
    }

    /** Safe integer parse — returns -1 on failure */
    private int safeInt(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return -1; }
    }

    /** Safe double parse — returns NaN on failure */
    private double safeDouble(String s) {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception e) { return Double.NaN; }
    }

    // ── Seeding ─────────────────────────────────────────────────────────────────

    private void seedInitialData() {
        if (userService.getAllUsers().isEmpty()) {
            userService.registerUser("admin", "admin123", "ADMIN");
        }
        if (modelDao.findAll().isEmpty()) {
            LlmModel gpt4o   = new LlmModel(null, "gpt-4o",           "OpenAI",    0.0050, null);
            LlmModel gpt4m   = new LlmModel(null, "gpt-4o-mini",      "OpenAI",    0.00015, null);
            LlmModel claude  = new LlmModel(null, "claude-3-5-sonnet", "Anthropic", 0.0030, null);
            LlmModel gemini  = new LlmModel(null, "gemini-1.5-pro",   "Google",    0.00125, null);
            LlmModel llama   = new LlmModel(null, "llama-3-70b",      "Groq",      0.0008, null);
            for (LlmModel m : List.of(gpt4o, gpt4m, claude, gemini, llama)) modelDao.create(m);

            // Explicit suitability mappings
            Map<LlmModel, Map<TaskType, Double>> smap = new LinkedHashMap<>();
            smap.put(gpt4o,  Map.of(TaskType.CODE_GENERATION, 0.95, TaskType.DATA_EXTRACTION, 0.90, TaskType.SUMMARIZATION, 0.88));
            smap.put(gpt4m,  Map.of(TaskType.GENERAL_CHAT, 0.90, TaskType.SUMMARIZATION, 0.80));
            smap.put(claude, Map.of(TaskType.CREATIVE_WRITING, 0.97, TaskType.CODE_GENERATION, 0.88, TaskType.SUMMARIZATION, 0.92));
            smap.put(gemini, Map.of(TaskType.DATA_EXTRACTION, 0.92, TaskType.CREATIVE_WRITING, 0.85, TaskType.GENERAL_CHAT, 0.86));
            smap.put(llama,  Map.of(TaskType.CODE_GENERATION, 0.82, TaskType.GENERAL_CHAT, 0.85, TaskType.SUMMARIZATION, 0.78));

            for (var entry : smap.entrySet()) {
                for (var suit : entry.getValue().entrySet()) {
                    suitabilityDao.create(new ModelSuitability(null, entry.getKey().getId(), suit.getKey(), suit.getValue(), null));
                }
            }
        }
    }

    // ── Main loop ────────────────────────────────────────────────────────────────

    public void run() {
        TerminalUtils.clearScreen();
        TerminalUtils.printBanner();
        while (true) {
            try {
                if (loggedInUser == null) showAuthMenu();
                else                     showMainMenu();
            } catch (NoSuchElementException e) {
                return;
            } catch (NexusException e) {
                TerminalUtils.printError(e.getMessage());
            } catch (Exception e) {
                TerminalUtils.printError("Unexpected error: " + e.getMessage());
            }
        }
    }

    // ── Auth ─────────────────────────────────────────────────────────────────────

    private void showAuthMenu() {
        TerminalUtils.printBox("AUTHENTICATION", "1. Login to your account\n2. Register new developer\n3. Exit Nexus");
        TerminalUtils.printAuthPrompt();
        switch (scanner.nextLine().trim()) {
            case "1" -> login();
            case "2" -> register();
            case "3" -> { TerminalUtils.printInfo("Goodbye."); System.exit(0); }
            default  -> TerminalUtils.printError("Unknown option.");
        }
    }

    private void login() {
        System.out.print("  Username: "); String username = scanner.nextLine();
        System.out.print("  Password: "); String password = scanner.nextLine();
        try {
            loggedInUser = userService.authenticate(username, password);
            TerminalUtils.spinner("Authenticating...", 600);
            TerminalUtils.printSuccess("Welcome back, " + loggedInUser.getUsername() + " (" + loggedInUser.getRole() + ")");
        } catch (NexusException e) {
            auditLogDao.create(new AuditLog(null, null, "LOGIN_FAIL", "username=" + username, "FAILURE", null));
            throw e;
        }
    }

    private void register() {
        System.out.print("  Username: "); String username = scanner.nextLine();
        System.out.print("  Password: "); String password = scanner.nextLine();
        userService.registerUser(username, password, "USER");
        TerminalUtils.printSuccess("Account created. Please login.");
    }

    // ── Main menu ────────────────────────────────────────────────────────────────

    private void showMainMenu() {
        System.out.println();
        TerminalUtils.printSeparator("DASHBOARD · " + loggedInUser.getEntityDisplayName());
        System.out.println();
        System.out.println("  " + TerminalUtils.AMBER + "1" + TerminalUtils.RESET + "  Intelligent Routing Engine");
        System.out.println("  " + TerminalUtils.AMBER + "2" + TerminalUtils.RESET + "  Memory Vault  " + TerminalUtils.GRAY + "(Contextd)" + TerminalUtils.RESET);
        System.out.println("  " + TerminalUtils.AMBER + "3" + TerminalUtils.RESET + "  API Key Vault");
        System.out.println("  " + TerminalUtils.AMBER + "4" + TerminalUtils.RESET + "  Model Discovery");
        System.out.println("  " + TerminalUtils.AMBER + "5" + TerminalUtils.RESET + "  Financial Intelligence");
        System.out.println("  " + TerminalUtils.AMBER + "6" + TerminalUtils.RESET + "  Execution History");
        System.out.println("  " + TerminalUtils.AMBER + "7" + TerminalUtils.RESET + "  Audit Log");
        if ("ADMIN".equals(loggedInUser.getRole())) {
            System.out.println("  " + TerminalUtils.AMBER + "8" + TerminalUtils.RESET + "  System Administration");
        }
        System.out.println("  " + TerminalUtils.AMBER + "0" + TerminalUtils.RESET + "  Logout");
        System.out.println();
        TerminalUtils.printPrompt(loggedInUser.getUsername());
        switch (scanner.nextLine().trim()) {
            case "1" -> routingMenu();
            case "2" -> memoryMenu();
            case "3" -> apiKeyMenu();
            case "4" -> modelDiscovery();
            case "5" -> financialDashboard();
            case "6" -> executionHistory();
            case "7" -> viewAuditLog();
            case "8" -> { if ("ADMIN".equals(loggedInUser.getRole())) adminMenu(); }
            case "0" -> { loggedInUser = null; TerminalUtils.clearScreen(); TerminalUtils.printBanner(); }
            default  -> TerminalUtils.printError("Unknown option.");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // 1. INTELLIGENT ROUTING ENGINE
    // ══════════════════════════════════════════════════════════════════════════════

    private void routingMenu() {
        TerminalUtils.printHeader("Intelligent Routing Engine");
        System.out.println("  " + TerminalUtils.AMBER + "1" + TerminalUtils.RESET + "  Route a Task (select best model)");
        System.out.println("  " + TerminalUtils.AMBER + "2" + TerminalUtils.RESET + "  Explain Routing  " + TerminalUtils.GRAY + "(score breakdown per model)" + TerminalUtils.RESET);
        System.out.println("  " + TerminalUtils.AMBER + "3" + TerminalUtils.RESET + "  What-If Analysis  " + TerminalUtils.GRAY + "(different budget caps)" + TerminalUtils.RESET);
        System.out.println("  " + TerminalUtils.AMBER + "4" + TerminalUtils.RESET + "  Record Execution Outcome");
        System.out.println("  " + TerminalUtils.AMBER + "B" + TerminalUtils.RESET + "  Back");
        System.out.println();
        TerminalUtils.printPrompt(loggedInUser.getUsername());
        switch (scanner.nextLine().trim().toUpperCase()) {
            case "1" -> routeTask();
            case "2" -> explainRouting();
            case "3" -> whatIfAnalysis();
            case "4" -> recordOutcome();
        }
    }

    private TaskType pickTask() {
        TaskType[] tasks = TaskType.values();
        System.out.println();
        for (int i = 0; i < tasks.length; i++) {
            System.out.printf("  " + TerminalUtils.AMBER + "%-2d" + TerminalUtils.RESET + " %s%n", i + 1, tasks[i].name());
        }
        System.out.print("  Task (1-" + tasks.length + "): ");
        int idx = safeInt(scanner.nextLine()) - 1;
        if (idx < 0 || idx >= tasks.length) throw new NexusException("Invalid task selection (1-" + tasks.length + ").");
        return tasks[idx];
    }

    private void routeTask() {
        TerminalUtils.printSeparator("TASK SELECTION");
        TaskType task = pickTask();
        TerminalUtils.spinner("Analysing " + task + " performance data...", 900);

        LlmModel best = routingEngine.selectOptimalModelForUser(task, Double.MAX_VALUE, loggedInUser.getId());
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
        TaskType task = pickTask();
        TerminalUtils.spinner("Computing 4-signal composite scores...", 800);

        List<RoutingEngine.ModelScoreBreakdown> breakdown = routingEngine.explainRoutingForUser(task, loggedInUser.getId());
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
                rankMark,
                b.model().getName(),
                b.model().getProvider(),
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
        TaskType task = pickTask();
        double[] tiers = {0.00015, 0.001, 0.003, 0.005, Double.MAX_VALUE};
        TerminalUtils.spinner("Simulating budget tiers...", 700);
        System.out.println();
        List<String> results = routingEngine.whatIfBudget(task, tiers);
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
        List<LlmModel> models = modelDao.findAll();
        if (models.isEmpty()) { TerminalUtils.printError("No models registered."); return; }
        for (int i = 0; i < models.size(); i++)
            System.out.printf("  " + TerminalUtils.AMBER + "%d" + TerminalUtils.RESET + "  %s (%s) - $%.5f/1k%n",
                i+1, models.get(i).getName(), models.get(i).getProvider(), models.get(i).getCostPer1kTokens());
        System.out.print("  Model # (1-" + models.size() + "): ");
        int midx = safeInt(scanner.nextLine()) - 1;
        if (midx < 0 || midx >= models.size()) { TerminalUtils.printError("Invalid model selection."); return; }
        LlmModel model = models.get(midx);

        TaskType task = pickTask();
        System.out.print("  Quality score (0.0–1.0): ");
        double quality = safeDouble(scanner.nextLine());
        if (Double.isNaN(quality) || quality < 0 || quality > 1) {
            TerminalUtils.printError("Quality must be between 0.0 and 1.0."); return;
        }
        System.out.print("  Latency (ms): ");
        int latency = safeInt(scanner.nextLine());
        if (latency < 0) { TerminalUtils.printError("Latency must be a positive integer."); return; }
        System.out.print("  Approximate tokens used (e.g. 500): ");
        int tokens = safeInt(scanner.nextLine());
        if (tokens <= 0) tokens = 500; // sensible fallback

        // Compute actual cost from real token count
        double actualCost = model.getCostPer1kTokens() * (tokens / 1000.0);

        OutcomeMemory rec = new OutcomeMemory(null, loggedInUser.getId(), model.getId(),
            task, actualCost, latency, quality, null);
        outcomeDao.create(rec);
        TerminalUtils.printSuccess(String.format("Outcome committed. Actual cost: $%.7f  Router intelligence updated.", actualCost));
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // 2. MEMORY VAULT (CONTEXTD)
    // ══════════════════════════════════════════════════════════════════════════════

    private void memoryMenu() {
        TerminalUtils.printHeader("Memory Vault  ·  Contextd Layer");
        System.out.println("  " + TerminalUtils.AMBER + "1" + TerminalUtils.RESET + "  Store new memory");
        System.out.println("  " + TerminalUtils.AMBER + "2" + TerminalUtils.RESET + "  Recall memories  " + TerminalUtils.GRAY + "(keyword search, ranked by confidence × recency)" + TerminalUtils.RESET);
        System.out.println("  " + TerminalUtils.AMBER + "3" + TerminalUtils.RESET + "  View full vault");
        System.out.println("  " + TerminalUtils.AMBER + "4" + TerminalUtils.RESET + "  Forget memory  " + TerminalUtils.GRAY + "(hard delete by ID)" + TerminalUtils.RESET);
        System.out.println("  " + TerminalUtils.AMBER + "5" + TerminalUtils.RESET + "  Contradiction graph");
        System.out.println("  " + TerminalUtils.AMBER + "6" + TerminalUtils.RESET + "  Run decay pass  " + TerminalUtils.GRAY + "(-5% confidence on stale memories)" + TerminalUtils.RESET);
        System.out.println("  " + TerminalUtils.AMBER + "7" + TerminalUtils.RESET + "  Prune expired / low-confidence");
        System.out.println("  " + TerminalUtils.AMBER + "B" + TerminalUtils.RESET + "  Back");
        System.out.println();
        TerminalUtils.printPrompt(loggedInUser.getUsername());
        switch (scanner.nextLine().trim().toUpperCase()) {
            case "1" -> storeMemory();
            case "2" -> recallMemories();
            case "3" -> viewVault();
            case "4" -> forgetMemory();
            case "5" -> contradictionGraph();
            case "6" -> runDecay();
            case "7" -> pruneMemories();
        }
    }

    private void storeMemory() {
        TerminalUtils.printSeparator("STORE MEMORY");
        System.out.print("  Content: "); String content = scanner.nextLine();
        System.out.print("  Tags (comma-separated): "); String tags = scanner.nextLine();
        System.out.println("  Type: 1=FACT  2=PREFERENCE  3=EPISODE  4=SKILL");
        System.out.print("  Type #: ");
        MemoryType[] types = {MemoryType.FACT, MemoryType.PREFERENCE, MemoryType.EPISODE, MemoryType.SKILL};
        MemoryType type = types[Math.max(0, Math.min(3, Integer.parseInt(scanner.nextLine().trim()) - 1))];

        TerminalUtils.spinner("Encoding and persisting memory...", 500);
        Memory mem = memoryService.store(loggedInUser.getId(), content, tags, type);

        if (mem.getType() == MemoryType.CONTRADICTION) {
            TerminalUtils.printWarn("Contradiction detected with existing FACT. Stored as CONTRADICTION type.");
        } else {
            TerminalUtils.printSuccess("Memory stored. ID: " + mem.getId() + "  TTL: " + mem.getType().getDefaultTtlDays() + " days");
        }
    }

    private void recallMemories() {
        TerminalUtils.printSeparator("RECALL MEMORIES");
        System.out.print("  Search query: "); String query = scanner.nextLine();
        TerminalUtils.spinner("Executing hybrid retrieval...", 600);

        List<Memory> results = memoryService.recall(loggedInUser.getId(), query);
        if (results.isEmpty()) { TerminalUtils.printInfo("No memories matched."); return; }

        System.out.println();
        String[] headers = {"ID", "Type", "Confidence", "Content", "Tags", "TTL"};
        String[][] rows  = new String[Math.min(results.size(), 10)][6];
        for (int i = 0; i < rows.length; i++) {
            Memory m = results.get(i);
            rows[i] = new String[]{
                String.valueOf(m.getId()),
                TerminalUtils.AMBER + m.getType().name() + TerminalUtils.RESET,
                TerminalUtils.confidenceBar(m.getConfidence()),
                m.getContent().length() > 45 ? m.getContent().substring(0, 42) + "..." : m.getContent(),
                m.getTags() != null ? m.getTags() : "",
                m.daysUntilExpiry() == Long.MAX_VALUE ? "∞" : m.daysUntilExpiry() + "d"
            };
        }
        TerminalUtils.printTable(headers, rows);
        if (results.size() > 10) TerminalUtils.printInfo("Showing top 10 of " + results.size() + " results.");
    }

    private void viewVault() {
        TerminalUtils.printSeparator("FULL MEMORY VAULT");
        List<Memory> all = memoryService.getAllMemories(loggedInUser.getId());
        if (all.isEmpty()) { TerminalUtils.printInfo("Vault is empty. Use 'Store memory' to add entries."); return; }

        // Group by type
        System.out.println();
        for (MemoryType t : MemoryType.values()) {
            List<Memory> typed = all.stream().filter(m -> m.getType() == t).toList();
            if (typed.isEmpty()) continue;
            TerminalUtils.printSeparator(t.name() + " · " + t.getDescription());
            for (Memory m : typed) {
                String bar = TerminalUtils.confidenceBar(m.getConfidence());
                System.out.printf("  " + TerminalUtils.GRAY + "#%-4d" + TerminalUtils.RESET + " %s  %s  | %s%n",
                    m.getId(), bar, m.getContent().length() > 50 ? m.getContent().substring(0, 47) + "..." : m.getContent(),
                    m.getTags() != null ? m.getTags() : "");
            }
        }
        System.out.println();
        TerminalUtils.printInfo("Total memories: " + all.size());
    }

    private void forgetMemory() {
        TerminalUtils.printSeparator("FORGET MEMORY");
        System.out.print("  Memory ID to forget: ");
        int id = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("  Confirm delete? (yes/no): ");
        if ("yes".equalsIgnoreCase(scanner.nextLine().trim())) {
            memoryService.forget(loggedInUser.getId(), id);
            TerminalUtils.printSuccess("Memory #" + id + " erased from vault.");
        } else {
            TerminalUtils.printInfo("Cancelled.");
        }
    }

    private void contradictionGraph() {
        TerminalUtils.printSeparator("CONTRADICTION GRAPH");
        List<Memory> contradictions = memoryService.getContradictions(loggedInUser.getId());
        if (contradictions.isEmpty()) {
            TerminalUtils.printSuccess("No contradictions found. Memory graph is clean.");
            return;
        }
        System.out.println();
        for (Memory c : contradictions) {
            System.out.println("  " + TerminalUtils.RED + "⚡ CONTRADICTION #" + c.getId() + TerminalUtils.RESET);
            System.out.println("  " + TerminalUtils.GRAY + "Content: " + TerminalUtils.RESET + c.getContent());
            System.out.println("  " + TerminalUtils.GRAY + "Tags: " + TerminalUtils.RESET + c.getTags());
            System.out.println("  " + TerminalUtils.GRAY + "Confidence: " + TerminalUtils.RESET + TerminalUtils.confidenceBar(c.getConfidence()));
            System.out.println();
        }
    }

    private void runDecay() {
        TerminalUtils.spinner("Running confidence decay pass...", 800);
        int decayed = memoryService.runDecayPass(loggedInUser.getId());
        TerminalUtils.printSuccess(decayed + " memories decayed by 5% confidence.");
    }

    private void pruneMemories() {
        System.out.print("  Prune all expired / low-confidence memories? (yes/no): ");
        if ("yes".equalsIgnoreCase(scanner.nextLine().trim())) {
            TerminalUtils.spinner("Pruning stale memories...", 600);
            int pruned = memoryService.pruneExpired(loggedInUser.getId());
            TerminalUtils.printSuccess(pruned + " memories pruned.");
        } else {
            TerminalUtils.printInfo("Cancelled.");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // 3. API KEY VAULT
    // ══════════════════════════════════════════════════════════════════════════════

    private void apiKeyMenu() {
        TerminalUtils.printHeader("API Key Vault");
        TerminalUtils.printInfo("Keys are XOR-encoded locally. Never sent to any server.");
        System.out.println();
        System.out.println("  " + TerminalUtils.AMBER + "1" + TerminalUtils.RESET + "  Add new API key");
        System.out.println("  " + TerminalUtils.AMBER + "2" + TerminalUtils.RESET + "  View my keys");
        System.out.println("  " + TerminalUtils.AMBER + "3" + TerminalUtils.RESET + "  Delete a key");
        System.out.println("  " + TerminalUtils.AMBER + "B" + TerminalUtils.RESET + "  Back");
        System.out.println();
        TerminalUtils.printPrompt(loggedInUser.getUsername());
        switch (scanner.nextLine().trim().toUpperCase()) {
            case "1" -> addApiKey();
            case "2" -> viewApiKeys();
            case "3" -> deleteApiKey();
        }
    }

    private void addApiKey() {
        TerminalUtils.printSeparator("ADD API KEY");
        Provider[] providers = Provider.values();
        for (int i = 0; i < providers.length; i++)
            System.out.printf("  " + TerminalUtils.AMBER + "%d" + TerminalUtils.RESET + "  %-20s %s%n",
                i + 1, providers[i].getDisplayName(), TerminalUtils.GRAY + providers[i].getBaseUrl() + TerminalUtils.RESET);
        System.out.print("  Provider (1-" + providers.length + "): ");
        int pidx = safeInt(scanner.nextLine()) - 1;
        if (pidx < 0 || pidx >= providers.length) { TerminalUtils.printError("Invalid provider selection."); return; }
        Provider p = providers[pidx];
        System.out.print("  Alias (e.g. my-work-key): ");
        String alias = scanner.nextLine().trim();
        System.out.print("  API Key: ");
        String rawKey = scanner.nextLine().trim();
        if (rawKey.isEmpty()) { TerminalUtils.printError("API key cannot be empty."); return; }

        TerminalUtils.spinner("Encoding and storing key...", 500);
        ApiKey stored = apiKeyService.storeKey(loggedInUser.getId(), p, alias, rawKey);
        TerminalUtils.printSuccess("Key stored: " + stored.getMaskedKey() + "  provider=" + p.getDisplayName());
    }

    private void viewApiKeys() {
        TerminalUtils.printSeparator("STORED API KEYS");
        List<ApiKey> keys = apiKeyService.listKeysForUser(loggedInUser.getId());
        if (keys.isEmpty()) { TerminalUtils.printInfo("No keys stored yet."); return; }

        String[] headers = {"ID", "Provider", "Alias", "Masked Key", "Added"};
        String[][] rows  = new String[keys.size()][5];
        for (int i = 0; i < keys.size(); i++) {
            ApiKey k = keys.get(i);
            rows[i] = new String[]{
                String.valueOf(k.getId()),
                TerminalUtils.AMBER + k.getProvider().getDisplayName() + TerminalUtils.RESET,
                k.getAlias(),
                TerminalUtils.GOLD + k.getMaskedKey() + TerminalUtils.RESET,
                k.getCreatedAt() != null ? k.getCreatedAt().toLocalDate().toString() : "—"
            };
        }
        System.out.println();
        TerminalUtils.printTable(headers, rows);
    }

    private void deleteApiKey() {
        viewApiKeys();
        System.out.print("  Key ID to delete: ");
        int id = safeInt(scanner.nextLine());
        if (id <= 0) { TerminalUtils.printError("Invalid key ID."); return; }
        System.out.print("  Confirm? (yes/no): ");
        if ("yes".equalsIgnoreCase(scanner.nextLine().trim())) {
            apiKeyService.deleteKey(loggedInUser.getId(), id);
            TerminalUtils.printSuccess("Key deleted.");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // 4. MODEL DISCOVERY
    // ══════════════════════════════════════════════════════════════════════════════

    private void modelDiscovery() {
        TerminalUtils.printHeader("Model Discovery");
        System.out.println("  " + TerminalUtils.AMBER + "1" + TerminalUtils.RESET + "  List all models");
        System.out.println("  " + TerminalUtils.AMBER + "2" + TerminalUtils.RESET + "  Filter by provider");
        System.out.println("  " + TerminalUtils.AMBER + "3" + TerminalUtils.RESET + "  View suitability matrix");
        System.out.println("  " + TerminalUtils.AMBER + "B" + TerminalUtils.RESET + "  Back");
        System.out.println();
        TerminalUtils.printPrompt(loggedInUser.getUsername());
        switch (scanner.nextLine().trim().toUpperCase()) {
            case "1" -> listAllModels();
            case "2" -> filterByProvider();
            case "3" -> suitabilityMatrix();
        }
    }

    private void listAllModels() {
        List<LlmModel> models = modelDao.findAll();
        if (models.isEmpty()) { TerminalUtils.printInfo("No models registered."); return; }
        System.out.println();
        String[] headers = {"ID", "Model", "Provider", "Cost / 1k tokens"};
        String[][] rows = new String[models.size()][4];
        for (int i = 0; i < models.size(); i++) {
            LlmModel m = models.get(i);
            rows[i] = new String[]{
                String.valueOf(m.getId()),
                TerminalUtils.BOLD + m.getName() + TerminalUtils.RESET,
                m.getProvider(),
                TerminalUtils.GOLD + "$" + String.format("%.5f", m.getCostPer1kTokens()) + TerminalUtils.RESET
            };
        }
        TerminalUtils.printTable(headers, rows);
    }

    private void filterByProvider() {
        System.out.print("  Provider name: "); String q = scanner.nextLine().trim();
        List<LlmModel> filtered = modelDao.findByProvider(q);
        if (filtered.isEmpty()) { TerminalUtils.printInfo("No models found for provider: " + q); return; }
        System.out.println();
        for (LlmModel m : filtered)
            System.out.printf("  [%d] %s — $%.5f/1k%n", m.getId(), m.getName(), m.getCostPer1kTokens());
    }

    private void suitabilityMatrix() {
        TerminalUtils.printSeparator("SUITABILITY MATRIX");
        List<LlmModel>       models = modelDao.findAll();
        TaskType[]           tasks  = TaskType.values();
        String[] headers = new String[tasks.length + 1];
        headers[0] = "Model";
        for (int i = 0; i < tasks.length; i++) headers[i + 1] = tasks[i].name().substring(0, Math.min(5, tasks[i].name().length()));
        String[][] rows = new String[models.size()][headers.length];
        for (int r = 0; r < models.size(); r++) {
            LlmModel m = models.get(r);
            rows[r][0] = m.getName();
            for (int c = 0; c < tasks.length; c++) {
                List<ModelSuitability> suits = suitabilityDao.findByTaskType(tasks[c]);
                double score = suits.stream().filter(s -> s.getModelId().equals(m.getId()))
                    .mapToDouble(ModelSuitability::getBaseScore).max().orElse(0.0);
                rows[r][c + 1] = score == 0 ? TerminalUtils.GRAY + "  ─  " + TerminalUtils.RESET :
                    TerminalUtils.AMBER + String.format("%.2f", score) + TerminalUtils.RESET;
            }
        }
        System.out.println();
        TerminalUtils.printTable(headers, rows);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // 5. FINANCIAL INTELLIGENCE
    // ══════════════════════════════════════════════════════════════════════════════

    private void financialDashboard() {
        TerminalUtils.printHeader("Financial Intelligence");
        System.out.println("  " + TerminalUtils.AMBER + "1" + TerminalUtils.RESET + "  Full report  (all time)");
        System.out.println("  " + TerminalUtils.AMBER + "2" + TerminalUtils.RESET + "  Last 7 days");
        System.out.println("  " + TerminalUtils.AMBER + "3" + TerminalUtils.RESET + "  Last 30 days");
        System.out.println("  " + TerminalUtils.AMBER + "B" + TerminalUtils.RESET + "  Back");
        System.out.println();
        TerminalUtils.printPrompt(loggedInUser.getUsername());
        String opt = scanner.nextLine().trim().toUpperCase();
        if (!opt.equals("B")) generateFinancialReport(opt);
    }

    private void generateFinancialReport(String range) {
        TerminalUtils.spinner("Aggregating execution data...", 800);
        List<OutcomeMemory> all = outcomeDao.findAll();

        // Filter by range (time-based)
        java.time.LocalDateTime cutoff = range.equals("2") ?
            java.time.LocalDateTime.now().minusDays(7) :
            range.equals("3") ? java.time.LocalDateTime.now().minusDays(30) : null;
        if (cutoff != null) all = all.stream().filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(cutoff)).toList();

        if (all.isEmpty()) { TerminalUtils.printInfo("No execution data for this period."); return; }

        double totalCost = all.stream().mapToDouble(OutcomeMemory::getCost).sum();
        int    totalExec = all.size();
        double avgQuality = all.stream().mapToDouble(OutcomeMemory::getQualityScore).average().orElse(0);

        // Per-model breakdown
        Map<Integer, List<OutcomeMemory>> byModel = new LinkedHashMap<>();
        for (OutcomeMemory o : all) byModel.computeIfAbsent(o.getModelId(), k -> new ArrayList<>()).add(o);

        // Per-task breakdown
        Map<TaskType, List<OutcomeMemory>> byTask = new LinkedHashMap<>();
        for (OutcomeMemory o : all) byTask.computeIfAbsent(o.getTaskType(), k -> new ArrayList<>()).add(o);

        // Savings analysis — optimal = route everything to cheapest model with quality >= avg
        double optimalCost = all.stream().mapToDouble(o -> {
            // Find cheapest model that has accuracy >= 0.7
            return modelDao.findAll().stream()
                .filter(m -> m.getCostPer1kTokens() < o.getCost())
                .mapToDouble(LlmModel::getCostPer1kTokens)
                .min().orElse(o.getCost());
        }).sum();

        System.out.println();

        // Summary
        TerminalUtils.printSeparator("SUMMARY");
        TerminalUtils.printKeyValue("Total Executions", String.valueOf(totalExec));
        TerminalUtils.printKeyValue("Total Spend", TerminalUtils.GOLD + "$" + String.format("%.6f", totalCost) + TerminalUtils.RESET);
        TerminalUtils.printKeyValue("Avg Quality Score", String.format("%.2f", avgQuality));
        TerminalUtils.printKeyValue("Avg Cost / Call", "$" + String.format("%.7f", totalCost / totalExec));

        // Per-model table
        System.out.println();
        TerminalUtils.printSeparator("MODEL BREAKDOWN");
        String[] mHeaders = {"Model", "Calls", "Total Cost", "Avg Quality", "Avg Latency", "Share"};
        String[][] mRows = new String[byModel.size()][6];
        int mi = 0;
        for (var entry : byModel.entrySet()) {
            LlmModel m = modelDao.read(entry.getKey()).orElse(null);
            List<OutcomeMemory> oms = entry.getValue();
            double mCost = oms.stream().mapToDouble(OutcomeMemory::getCost).sum();
            double mQ = oms.stream().mapToDouble(OutcomeMemory::getQualityScore).average().orElse(0);
            double mLat = oms.stream().mapToDouble(OutcomeMemory::getLatencyMs).average().orElse(0);
            double share = totalCost == 0 ? 0 : mCost / totalCost;
            mRows[mi++] = new String[]{
                m != null ? m.getName() : "Unknown",
                String.valueOf(oms.size()),
                TerminalUtils.GOLD + "$" + String.format("%.6f", mCost) + TerminalUtils.RESET,
                String.format("%.2f", mQ),
                String.format("%.0f ms", mLat),
                TerminalUtils.progressBar(share, 1.0, 10)
            };
        }
        TerminalUtils.printTable(mHeaders, mRows);

        // Per-task table
        System.out.println();
        TerminalUtils.printSeparator("TASK BREAKDOWN");
        String[] tHeaders = {"Task Type", "Calls", "Total Cost", "Avg Quality"};
        String[][] tRows = new String[byTask.size()][4];
        int ti = 0;
        for (var entry : byTask.entrySet()) {
            List<OutcomeMemory> oms = entry.getValue();
            tRows[ti++] = new String[]{
                TerminalUtils.AMBER + entry.getKey().name() + TerminalUtils.RESET,
                String.valueOf(oms.size()),
                "$" + String.format("%.6f", oms.stream().mapToDouble(OutcomeMemory::getCost).sum()),
                String.format("%.2f", oms.stream().mapToDouble(OutcomeMemory::getQualityScore).average().orElse(0))
            };
        }
        TerminalUtils.printTable(tHeaders, tRows);

        // Savings
        System.out.println();
        TerminalUtils.printSeparator("SAVINGS ANALYSIS");
        double savings = Math.max(0, totalCost - optimalCost);
        double pct     = totalCost == 0 ? 0 : (savings / totalCost) * 100;
        TerminalUtils.printKeyValue("Actual Spend",  TerminalUtils.RED  + "$" + String.format("%.6f", totalCost)  + TerminalUtils.RESET);
        TerminalUtils.printKeyValue("Optimal Spend", TerminalUtils.GREEN + "$" + String.format("%.6f", optimalCost) + TerminalUtils.RESET);
        TerminalUtils.printKeyValue("Savings on Table", TerminalUtils.GOLD + "$" + String.format("%.6f", savings) + " (" + String.format("%.1f", pct) + "% savings)" + TerminalUtils.RESET);
        System.out.println();

        auditLogDao.create(new AuditLog(null, loggedInUser.getId(), "FINANCIAL_REPORT",
            "range=" + range + " executions=" + totalExec, "SUCCESS", null));
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // 6. EXECUTION HISTORY
    // ══════════════════════════════════════════════════════════════════════════════

    private void executionHistory() {
        TerminalUtils.printHeader("Execution History");
        System.out.println("  " + TerminalUtils.AMBER + "1" + TerminalUtils.RESET + "  All executions");
        System.out.println("  " + TerminalUtils.AMBER + "2" + TerminalUtils.RESET + "  Filter by task type");
        System.out.println("  " + TerminalUtils.AMBER + "3" + TerminalUtils.RESET + "  Filter by model");
        System.out.println("  " + TerminalUtils.AMBER + "B" + TerminalUtils.RESET + "  Back");
        System.out.println();
        TerminalUtils.printPrompt(loggedInUser.getUsername());

        List<OutcomeMemory> history;
        switch (scanner.nextLine().trim().toUpperCase()) {
            case "2" -> {
                TaskType task = pickTask();
                history = outcomeDao.findByTaskType(task);
            }
            case "3" -> {
                System.out.print("  Model ID: ");
                history = outcomeDao.findByModelId(Integer.parseInt(scanner.nextLine().trim()));
            }
            default -> history = outcomeDao.findAll();
        }

        if (history.isEmpty()) { TerminalUtils.printInfo("No execution records."); return; }

        String[] headers = {"ID", "Task", "Model", "Quality", "Cost", "Latency", "Timestamp"};
        String[][] rows = new String[Math.min(history.size(), 20)][7];
        for (int i = 0; i < rows.length; i++) {
            OutcomeMemory o = history.get(i);
            String modelName = modelDao.read(o.getModelId()).map(LlmModel::getName).orElse("?");
            rows[i] = new String[]{
                String.valueOf(o.getId()),
                TerminalUtils.AMBER + o.getTaskType().name() + TerminalUtils.RESET,
                modelName,
                TerminalUtils.confidenceBar(o.getQualityScore()),
                "$" + String.format("%.7f", o.getCost()),
                o.getLatencyMs() + "ms",
                o.getCreatedAt() != null ? o.getCreatedAt().toLocalDate().toString() : "—"
            };
        }
        System.out.println();
        TerminalUtils.printTable(headers, rows);
        if (history.size() > 20) TerminalUtils.printInfo("Showing latest 20 of " + history.size() + " records.");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // 7. AUDIT LOG
    // ══════════════════════════════════════════════════════════════════════════════

    private void viewAuditLog() {
        TerminalUtils.printHeader("Audit Log");
        List<AuditLog> logs = auditLogDao.findByUserId(loggedInUser.getId());
        if (logs.isEmpty()) { TerminalUtils.printInfo("No audit entries."); return; }

        String[] headers = {"#", "Action", "Details", "Outcome", "Time"};
        String[][] rows  = new String[Math.min(logs.size(), 25)][5];
        for (int i = 0; i < rows.length; i++) {
            AuditLog l = logs.get(i);
            String outcomeColor = "SUCCESS".equals(l.getOutcome()) ? TerminalUtils.GREEN : TerminalUtils.RED;
            rows[i] = new String[]{
                String.valueOf(l.getId()),
                TerminalUtils.AMBER + l.getAction() + TerminalUtils.RESET,
                l.getDetails() != null && l.getDetails().length() > 40 ? l.getDetails().substring(0, 37) + "..." : l.getDetails(),
                outcomeColor + l.getOutcome() + TerminalUtils.RESET,
                l.getCreatedAt() != null ? l.getCreatedAt().toString().substring(0, 16) : "—"
            };
        }
        System.out.println();
        TerminalUtils.printTable(headers, rows);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // 8. ADMIN
    // ══════════════════════════════════════════════════════════════════════════════

    private void adminMenu() {
        TerminalUtils.printHeader("System Administration");
        System.out.println("  " + TerminalUtils.AMBER + "1" + TerminalUtils.RESET + "  List all users");
        System.out.println("  " + TerminalUtils.AMBER + "2" + TerminalUtils.RESET + "  Search user by username");
        System.out.println("  " + TerminalUtils.AMBER + "3" + TerminalUtils.RESET + "  Delete user");
        System.out.println("  " + TerminalUtils.AMBER + "4" + TerminalUtils.RESET + "  Register new model");
        System.out.println("  " + TerminalUtils.AMBER + "5" + TerminalUtils.RESET + "  Decommission model");
        System.out.println("  " + TerminalUtils.AMBER + "B" + TerminalUtils.RESET + "  Back");
        System.out.println();
        TerminalUtils.printPrompt(loggedInUser.getUsername());
        switch (scanner.nextLine().trim().toUpperCase()) {
            case "1" -> {
                List<User> users = userService.getAllUsers();
                String[] h = {"ID", "Username", "Role", "Joined"};
                String[][] r = new String[users.size()][4];
                for (int i = 0; i < users.size(); i++) {
                    User u = users.get(i);
                    r[i] = new String[]{ String.valueOf(u.getId()), u.getUsername(),
                        ("ADMIN".equals(u.getRole()) ? TerminalUtils.RED : TerminalUtils.CYAN) + u.getRole() + TerminalUtils.RESET,
                        u.getCreatedAt() != null ? u.getCreatedAt().toLocalDate().toString() : "—" };
                }
                System.out.println(); TerminalUtils.printTable(h, r);
            }
            case "2" -> {
                System.out.print("  Search term: ");
                userService.searchUsers(scanner.nextLine().trim()).forEach(u -> System.out.println("  " + u));
            }
            case "3" -> {
                System.out.print("  User ID to delete: ");
                int uid = safeInt(scanner.nextLine());
                if (uid <= 0) { TerminalUtils.printError("Invalid user ID."); break; }
                if (uid == loggedInUser.getId()) { TerminalUtils.printError("Cannot delete your own account."); break; }
                System.out.print("  Confirm? (yes/no): ");
                if ("yes".equalsIgnoreCase(scanner.nextLine().trim())) {
                    userService.deleteUser(uid);
                    auditLogDao.create(new AuditLog(null, loggedInUser.getId(), "USER_DELETE", "targetId=" + uid, "SUCCESS", null));
                    TerminalUtils.printSuccess("User deleted.");
                }
            }
            case "4" -> {
                System.out.print("  Model name: "); String name = scanner.nextLine().trim();
                System.out.print("  Provider: ");   String prov = scanner.nextLine().trim();
                System.out.print("  Cost / 1k tokens: ");
                double cost = safeDouble(scanner.nextLine());
                if (Double.isNaN(cost) || cost < 0) { TerminalUtils.printError("Invalid cost value."); break; }
                modelDao.create(new LlmModel(null, name, prov, cost, null));
                TerminalUtils.printSuccess("Model registered: " + name);
            }
            case "5" -> {
                listAllModels();
                System.out.print("  Model ID to decommission: ");
                int mid = Integer.parseInt(scanner.nextLine().trim());
                modelDao.delete(mid);
                TerminalUtils.printSuccess("Model decommissioned.");
            }
        }
    }
}
