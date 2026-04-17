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
        TerminalUtils.printSeparator("ACCESS GATE");
        String[] headers = {"Action", "Alias", "Legacy"};
        String[][] rows = {
            {"Login", "login", "1"},
            {"Register", "register", "2"},
            {"Exit", "exit", "3"}
        };
        TerminalUtils.printTable(headers, rows);
        TerminalUtils.printAuthPrompt();
        switch (ctx.scanner().nextLine().trim().toUpperCase()) {
            case "1", "LOGIN", "L" -> login();
            case "2", "REGISTER", "R" -> register();
            case "3", "EXIT", "QUIT", "Q" -> { TerminalUtils.printInfo("Goodbye."); System.exit(0); }
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
        List<String[]> commandRows = new java.util.ArrayList<>();
        commandRows.add(new String[] {"Build", "route", "1", "Routing Studio"});
        commandRows.add(new String[] {"Build", "memory", "2", "Memory Vault"});
        commandRows.add(new String[] {"Build", "keys", "3", "API Key Vault"});
        commandRows.add(new String[] {"Build", "models", "4", "Model Discovery"});
        commandRows.add(new String[] {"Operate", "finance", "5", "Financial Intelligence"});
        commandRows.add(new String[] {"Operate", "history", "6", "Execution History"});
        commandRows.add(new String[] {"Operate", "audit", "7", "Audit Log"});
        commandRows.add(new String[] {"Operate", "profile", "8", "Account + Profile"});
        commandRows.add(new String[] {"Advanced", "compat", "10", "Compatibility Features Hub"});
        commandRows.add(new String[] {"Advanced", "session-tools", "11", "Session Power Tools"});
        commandRows.add(new String[] {"Advanced", "intel", "I", "Intelligence Hub"});
        if ("ADMIN".equals(ctx.loggedInUser().getRole())) {
            commandRows.add(new String[] {"Advanced", "admin", "9", "System Administration"});
        }
        commandRows.add(new String[] {"System", "logout", "0", "Sign out"});

        TerminalUtils.printTable(
            new String[] {"Zone", "Alias", "Legacy", "Destination"},
            commandRows.toArray(String[][]::new)
        );
        TerminalUtils.printSeparator("QUICK FLOW");
        System.out.println("  Command mode fast path: nexus onboard --user " + ctx.username() + " --mode balanced --provider GROQ");
        System.out.println("  Suggested flow: route -> memory -> profile -> onboard");
        System.out.println();
        TerminalUtils.printInfo("Use aliases (route, memory, profile...) or legacy keys (1..11, I, 0).");
        TerminalUtils.printPrompt(ctx.username());
        String input = ctx.scanner().nextLine().trim().toUpperCase();
        switch (input) {
            case "1", "ROUTE", "ROUTING", "R" -> routingMenu.show();
            case "2", "MEMORY", "MEM", "M" -> memoryMenu.show();
            case "3", "KEYS", "KEY", "API", "K" -> apiKeyMenu.show();
            case "4", "MODELS", "MODEL", "MOD", "D" -> modelMenu.show();
            case "5", "FINANCE", "COST", "F" -> financeMenu.show();
            case "6", "HISTORY", "EXEC", "H" -> historyMenu.show();
            case "7", "AUDIT", "A" -> auditMenu.show();
            case "8", "PROFILE", "ACCOUNT", "P" -> accountSettings();
            case "10", "COMPAT", "COMPATIBILITY" -> routingMenu.showCompatibilityHub();
            case "11", "SESSION-TOOLS", "SESSIONTOOLS", "POWERTOOLS" -> routingMenu.showSessionPowerToolsHub();
            case "I", "INTEL", "INTELLIGENCE" -> intelligenceMenu.show();
            case "9", "ADMIN" -> { if ("ADMIN".equals(ctx.loggedInUser().getRole())) adminMenu.show(); }
            case "0", "LOGOUT", "EXIT", "Q" -> { ctx.setLoggedInUser(null); TerminalUtils.clearScreen(); TerminalUtils.printBanner(); }
            default  -> TerminalUtils.printError("Unknown option.");
        }
    }

    private void accountSettings() {
        while (true) {
            TerminalUtils.printSeparator("ACCOUNT SETTINGS");
            String[] headers = {"Action", "Alias", "Legacy", "Purpose"};
            String[][] rows = {
                {"Change Password", "password", "1", "Update account credential"},
                {"Profile Quick Setup", "quick", "2", "Apply safe/balanced/power-user"},
                {"Set Profile Setting", "set", "3", "Store a custom key/value"},
                {"View Profile Settings", "list", "4", "Inspect effective profile state"},
                {"Delete Profile Setting", "delete", "5", "Remove a key from scope"},
                {"Back", "back", "B", "Return to dashboard"}
            };
            TerminalUtils.printTable(headers, rows);
            System.out.println();
            TerminalUtils.printPrompt(ctx.username());

            String choice = ctx.scanner().nextLine().trim().toUpperCase();
            switch (choice) {
                case "1", "PASSWORD", "PASS" -> changePassword();
                case "2", "QUICK", "SETUP" -> profileQuickSetup();
                case "3", "SET" -> upsertProfileSetting();
                case "4", "LIST", "VIEW" -> listProfileSettings();
                case "5", "DELETE", "DEL" -> deleteProfileSetting();
                case "B", "BACK" -> { return; }
                default -> TerminalUtils.printError("Unknown option.");
            }
        }
    }

    private void profileQuickSetup() {
        try {
            String scope = askProfileScope();
            TerminalUtils.printSeparator("PROFILE QUICK SETUP");
            System.out.println("  Mode: 1=Safe  2=Balanced  3=Power-user");
            System.out.print("  Mode #: ");
            int mode = ctx.safeInt(ctx.scanner().nextLine());

            String selected = switch (mode) {
                case 1 -> "safe";
                case 3 -> "power-user";
                default -> "balanced";
            };

            switch (selected) {
                case "safe" -> {
                    ctx.profileService().setSetting(ctx.userId(), scope, "policy.allow_file_write", "false");
                    ctx.profileService().setSetting(ctx.userId(), scope, "policy.allow_recipe_run", "false");
                    ctx.profileService().setSetting(ctx.userId(), scope, "policy.allow_tool_shell", "false");
                    ctx.profileService().setSetting(ctx.userId(), scope, "context.max_injection_tokens", "700");
                    ctx.profileService().setSetting(ctx.userId(), scope, "context.max_memories", "4");
                }
                case "power-user" -> {
                    ctx.profileService().setSetting(ctx.userId(), scope, "policy.allow_file_write", "true");
                    ctx.profileService().setSetting(ctx.userId(), scope, "policy.allow_recipe_run", "true");
                    ctx.profileService().setSetting(ctx.userId(), scope, "policy.allow_tool_shell", "true");
                    ctx.profileService().setSetting(ctx.userId(), scope, "context.max_injection_tokens", "1200");
                    ctx.profileService().setSetting(ctx.userId(), scope, "context.max_memories", "8");
                }
                default -> {
                    ctx.profileService().setSetting(ctx.userId(), scope, "policy.allow_file_write", "true");
                    ctx.profileService().setSetting(ctx.userId(), scope, "policy.allow_recipe_run", "true");
                    ctx.profileService().setSetting(ctx.userId(), scope, "policy.allow_tool_shell", "false");
                    ctx.profileService().setSetting(ctx.userId(), scope, "context.max_injection_tokens", "900");
                    ctx.profileService().setSetting(ctx.userId(), scope, "context.max_memories", "6");
                }
            }

            ctx.profileService().setSetting(ctx.userId(), scope, "intent.require_confirmation_on_drift", "true");
            ctx.profileService().setSetting(ctx.userId(), scope, "intent.min_alignment_percent", "12");

            TerminalUtils.printSuccess("Profile quick setup applied: " + selected + " (scope=" + scope + ")");
        } catch (DaoException e) {
            TerminalUtils.printError("Could not apply quick setup due to a database issue.");
        } catch (NexusException e) {
            TerminalUtils.printError(e.getMessage());
        }
    }

    private void changePassword() {
        String current = readPassword("  Current Password: ");
        try {
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

    private void upsertProfileSetting() {
        try {
            String scope = askProfileScope();
            System.out.print("  Key (e.g. code_style, response_tone): ");
            String key = ctx.scanner().nextLine();
            System.out.print("  Value: ");
            String value = ctx.scanner().nextLine();

            ctx.profileService().setSetting(ctx.userId(), scope, key, value);
            TerminalUtils.printSuccess("Profile setting saved in scope: " + scope);
        } catch (DaoException e) {
            TerminalUtils.printError("Could not save profile setting due to a database issue.");
        } catch (NexusException e) {
            TerminalUtils.printError(e.getMessage());
        }
    }

    private void listProfileSettings() {
        try {
            String scope = askProfileScope();
            var merged = ctx.profileService().listSettings(ctx.userId(), scope, true);
            if (merged.isEmpty()) {
                TerminalUtils.printInfo("No profile settings found for scope: " + scope);
                return;
            }
            TerminalUtils.printSeparator("PROFILE SETTINGS · " + scope);
            String[] headers = {"Section", "Key", "Value"};
            String[][] rows = new String[merged.size()][3];
            int i = 0;
            for (var entry : merged.entrySet()) {
                String key = entry.getKey();
                String section = key.startsWith("policy.") ? "policy"
                    : key.startsWith("context.") ? "context"
                    : key.startsWith("intent.") ? "intent"
                    : key.startsWith("routing.") ? "routing"
                    : key.startsWith("memory.") ? "memory"
                    : "general";
                rows[i++] = new String[] {section, key, entry.getValue()};
            }
            TerminalUtils.printTable(headers, rows);
        } catch (DaoException e) {
            TerminalUtils.printError("Could not load profile settings due to a database issue.");
        }
    }

    private void deleteProfileSetting() {
        try {
            String scope = askProfileScope();
            System.out.print("  Key to delete: ");
            String key = ctx.scanner().nextLine();
            boolean deleted = ctx.profileService().deleteSetting(ctx.userId(), scope, key);
            if (deleted) TerminalUtils.printSuccess("Profile setting deleted.");
            else TerminalUtils.printInfo("No setting found for that key in scope.");
        } catch (DaoException e) {
            TerminalUtils.printError("Could not delete profile setting due to a database issue.");
        }
    }

    private String askProfileScope() {
        String project = ctx.profileService().currentWorkspaceScope();
        System.out.println("  Scope: 1=Project  2=Global  3=Custom");
        System.out.print("  Scope #: ");
        int choice = ctx.safeInt(ctx.scanner().nextLine());
        if (choice == 2) return com.nexus.service.ProfileService.GLOBAL_SCOPE;
        if (choice == 3) {
            System.out.print("  Custom scope label/path: ");
            String custom = ctx.scanner().nextLine().trim();
            return custom.isEmpty() ? project : custom;
        }
        return project;
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
