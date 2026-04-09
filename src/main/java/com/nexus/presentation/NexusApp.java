package com.nexus.presentation;

import java.io.Console;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.nexus.domain.AuditLog;
import com.nexus.domain.LlmModel;
import com.nexus.domain.ModelSuitability;
import com.nexus.domain.Provider;
import com.nexus.domain.TaskType;
import com.nexus.domain.User;
import com.nexus.exception.DaoException;
import com.nexus.exception.NexusException;
import com.nexus.util.TerminalUtils;

/**
 * Nexus Application Bootstrapper and Main Event Loop.
 * Refactored (Phase 1.2) to delegate all sub-menus to modular classes, 
 * resolving the "God Class" issue and improving maintainability.
 */
public class NexusApp {
    private final MenuContext ctx;
    private final RoutingMenu routingMenu;
    private final MemoryMenu memoryMenu;
    private final ApiKeyMenu apiKeyMenu;
    private final ModelMenu modelMenu;
    private final FinanceMenu financeMenu;
    private final HistoryMenu historyMenu;
    private final AuditMenu auditMenu;
    private final AdminMenu adminMenu;
    private final IntelligenceMenu intelligenceMenu;

    public NexusApp() {
        this.ctx              = new MenuContext();
        this.routingMenu      = new RoutingMenu(ctx);
        this.memoryMenu       = new MemoryMenu(ctx);
        this.apiKeyMenu       = new ApiKeyMenu(ctx);
        this.modelMenu        = new ModelMenu(ctx);
        this.financeMenu      = new FinanceMenu(ctx);
        this.historyMenu      = new HistoryMenu(ctx);
        this.auditMenu        = new AuditMenu(ctx);
        this.adminMenu        = new AdminMenu(ctx);
        this.intelligenceMenu = new IntelligenceMenu(ctx);
        
        seedInitialData();
    }

    private void seedInitialData() {
        if (ctx.userService().getAllUsers().isEmpty()) {
            ctx.userService().registerUser("admin", "admin123", "ADMIN");
        }
        if (ctx.modelDao().findAll().isEmpty()) {
            LlmModel gpt4o   = new LlmModel(null, "gpt-4o",            Provider.OPENAI.getDisplayName(),         0.0050, null);
            LlmModel gpt4m   = new LlmModel(null, "gpt-4o-mini",       Provider.OPENAI.getDisplayName(),         0.00015, null);
            LlmModel claude  = new LlmModel(null, "claude-3-5-sonnet", Provider.ANTHROPIC.getDisplayName(),      0.0030, null);
            LlmModel gemini  = new LlmModel(null, "gemini-1.5-pro",    Provider.GOOGLE_GEMINI.getDisplayName(),  0.00125, null);
            LlmModel llama   = new LlmModel(null, "llama-3-70b",       Provider.GROQ.getDisplayName(),           0.0008, null);
            for (LlmModel m : List.of(gpt4o, gpt4m, claude, gemini, llama)) ctx.modelDao().create(m);

            Map<LlmModel, Map<TaskType, Double>> smap = new LinkedHashMap<>();
            smap.put(gpt4o,  Map.of(TaskType.CODE_GENERATION, 0.95, TaskType.DATA_EXTRACTION, 0.90, TaskType.SUMMARIZATION, 0.88));
            smap.put(gpt4m,  Map.of(TaskType.GENERAL_CHAT, 0.90, TaskType.SUMMARIZATION, 0.80));
            smap.put(claude, Map.of(TaskType.CREATIVE_WRITING, 0.97, TaskType.CODE_GENERATION, 0.88, TaskType.SUMMARIZATION, 0.92));
            smap.put(gemini, Map.of(TaskType.DATA_EXTRACTION, 0.92, TaskType.CREATIVE_WRITING, 0.85, TaskType.GENERAL_CHAT, 0.86));
            smap.put(llama,  Map.of(TaskType.CODE_GENERATION, 0.82, TaskType.GENERAL_CHAT, 0.85, TaskType.SUMMARIZATION, 0.78));

            for (var entry : smap.entrySet()) {
                for (var suit : entry.getValue().entrySet()) {
                    ctx.suitabilityDao().create(new ModelSuitability(null, entry.getKey().getId(), suit.getKey(), suit.getValue(), null));
                }
            }
        }
    }

    public void run() {
        TerminalUtils.clearScreen();
        TerminalUtils.printBanner();
        while (true) {
            try {
                if (ctx.loggedInUser() == null) showAuthMenu();
                else                            showMainMenu();
            } catch (NoSuchElementException e) {
                return;
            } catch (DaoException e) {
                TerminalUtils.printError("Database operation failed. Please try again. No changes were saved.");
            } catch (NexusException e) {
                TerminalUtils.printError(e.getMessage());
            } catch (Exception e) {
                TerminalUtils.printError("Unexpected error: " + e.getMessage());
            }
        }
    }

    private void showAuthMenu() {
        TerminalUtils.printBox("AUTHENTICATION", "1. Login to your account\n2. Register new developer\n3. Exit Nexus");
        TerminalUtils.printAuthPrompt();
        switch (ctx.scanner().nextLine().trim()) {
            case "1" -> login();
            case "2" -> register();
            case "3" -> { TerminalUtils.printInfo("Goodbye."); System.exit(0); }
            default  -> TerminalUtils.printError("Unknown option.");
        }
    }

    private void login() {
        System.out.print("  Username: "); String username = ctx.scanner().nextLine();
        String password = readPassword("  Password: ");
        try {
            User user = ctx.userService().authenticate(username, password);
            ctx.setLoggedInUser(user);
            TerminalUtils.spinner("Authenticating...", 600);
            
            // Phase 2.2: Auto-decay on login
            int decayed = ctx.memoryService().runDecayPass(user.getId());
            TerminalUtils.printSuccess("Welcome back, " + user.getUsername() + " (" + user.getRole() + ")");
            if (decayed > 0) {
                TerminalUtils.printInfo(decayed + " memories naturally decayed in confidence since last login.");
            }
        } catch (DaoException e) {
            TerminalUtils.printError("Login is temporarily unavailable due to a database issue. Please try again.");
        } catch (NexusException e) {
            try {
                ctx.auditLogDao().create(new AuditLog(null, null, "LOGIN_FAIL", "username=" + username, "FAILURE", null));
            } catch (DaoException ignored) {
                // Best-effort only: failed login feedback should not be blocked by audit write failures.
            }
            throw e;
        }
    }

    private void register() {
        System.out.print("  Username: "); String username = ctx.scanner().nextLine();
        String password = readPassword("  Password: ");
        try {
            ctx.userService().registerUser(username, password, "USER");
            TerminalUtils.printSuccess("Account created. Please login.");
        } catch (DaoException e) {
            TerminalUtils.printError("Could not create account right now. Please try again.");
        }
    }

    private void showMainMenu() {
        System.out.println();
        TerminalUtils.printSeparator("DASHBOARD · " + ctx.loggedInUser().getEntityDisplayName());
        System.out.println();
        System.out.println("  " + TerminalUtils.AMBER + "1" + TerminalUtils.RESET + "  Intelligent Routing Engine");
        System.out.println("  " + TerminalUtils.AMBER + "2" + TerminalUtils.RESET + "  Memory Vault  " + TerminalUtils.GRAY + "(Contextd)" + TerminalUtils.RESET);
        System.out.println("  " + TerminalUtils.AMBER + "3" + TerminalUtils.RESET + "  API Key Vault");
        System.out.println("  " + TerminalUtils.AMBER + "4" + TerminalUtils.RESET + "  Model Discovery");
        System.out.println("  " + TerminalUtils.AMBER + "5" + TerminalUtils.RESET + "  Financial Intelligence");
        System.out.println("  " + TerminalUtils.AMBER + "6" + TerminalUtils.RESET + "  Execution History");
        System.out.println("  " + TerminalUtils.AMBER + "7" + TerminalUtils.RESET + "  Audit Log");
        System.out.println("  " + TerminalUtils.AMBER + "8" + TerminalUtils.RESET + "  Account Settings " + TerminalUtils.GRAY + "(Change Password)" + TerminalUtils.RESET);
        System.out.println("  " + TerminalUtils.GOLD + "I" + TerminalUtils.RESET + "  Intelligence Hub " + TerminalUtils.GRAY + "(God-Tier Features)" + TerminalUtils.RESET);
        if ("ADMIN".equals(ctx.loggedInUser().getRole())) {
            System.out.println("  " + TerminalUtils.AMBER + "9" + TerminalUtils.RESET + "  System Administration");
        }
        System.out.println("  " + TerminalUtils.AMBER + "0" + TerminalUtils.RESET + "  Logout");
        System.out.println();
        TerminalUtils.printPrompt(ctx.username());
        switch (ctx.scanner().nextLine().trim().toUpperCase()) {
            case "1" -> routingMenu.show();
            case "2" -> memoryMenu.show();
            case "3" -> apiKeyMenu.show();
            case "4" -> modelMenu.show();
            case "5" -> financeMenu.show();
            case "6" -> historyMenu.show();
            case "7" -> auditMenu.show();
            case "8" -> accountSettings();
            case "I" -> intelligenceMenu.show();
            case "9" -> { if ("ADMIN".equals(ctx.loggedInUser().getRole())) adminMenu.show(); }
            case "0" -> { ctx.setLoggedInUser(null); TerminalUtils.clearScreen(); TerminalUtils.printBanner(); }
            default  -> TerminalUtils.printError("Unknown option.");
        }
    }

    private void accountSettings() {
        // Phase 3.3: Self-service password change
        TerminalUtils.printSeparator("ACCOUNT SETTINGS");
        String current = readPassword("  Current Password: ");
        
        try {
            // Verify current password via authenticate (will throw if bad)
            ctx.userService().authenticate(ctx.username(), current);
            
            String np = readPassword("  New Password: ");
            if (np.isEmpty()) {
                TerminalUtils.printInfo("Password change cancelled.");
            } else {
                ctx.userService().updateUser(ctx.userId(), null, np);
                TerminalUtils.printSuccess("Password updated successfully.");
            }
        } catch (DaoException e) {
            TerminalUtils.printError("Could not update account settings right now. Database operation failed; no changes were saved.");
        } catch (NexusException e) {
            TerminalUtils.printError("Authentication failed: " + e.getMessage());
        }
    }

    private String readPassword(String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] raw = console.readPassword(prompt);
            return raw == null ? "" : new String(raw);
        }

        // IDE terminals may not expose Console; we keep a clear warning in fallback mode.
        System.out.print(prompt + " (visible): ");
        return ctx.scanner().nextLine();
    }

    public static void main(String[] args) {
        NexusApp app = new NexusApp();
        if (NexusCommandRunner.tryRun(args)) {
            return;
        }
        app.run();
    }
}
