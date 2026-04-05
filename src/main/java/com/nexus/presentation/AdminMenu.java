package com.nexus.presentation;

import java.util.List;

import com.nexus.domain.LlmModel;
import com.nexus.domain.ModelSuitability;
import com.nexus.domain.Provider;
import com.nexus.domain.TaskType;
import com.nexus.domain.User;
import com.nexus.util.SecurityUtils;
import com.nexus.util.TerminalUtils;

/**
 * System Administration menu.
 * Restricted to users with the ADMIN role.
 * Includes User Update (Phase 1.1) and Suitability CRUD (Phase 1.3).
 */
public class AdminMenu {
    private final MenuContext ctx;

    public AdminMenu(MenuContext ctx) { this.ctx = ctx; }

    public void show() {
        if (!"ADMIN".equals(ctx.loggedInUser().getRole())) {
            TerminalUtils.printError("Access Denied: Administrator privileges required.");
            // Phase 2.3: Use the dead exception class here if you wanted, but printError returns gracefully.
            return;
        }

        while(true) {
            TerminalUtils.printHeader("System Administration");
            System.out.println("  " + TerminalUtils.GOLD + "--- USER MANAGEMENT ---" + TerminalUtils.RESET);
            System.out.println("  " + TerminalUtils.AMBER + "1" + TerminalUtils.RESET + "  List all users");
            System.out.println("  " + TerminalUtils.AMBER + "2" + TerminalUtils.RESET + "  Delete user");
            System.out.println("  " + TerminalUtils.AMBER + "3" + TerminalUtils.RESET + "  Update user (username/password)");
            
            System.out.println("\n  " + TerminalUtils.GOLD + "--- MODEL CONFIGURATION ---" + TerminalUtils.RESET);
            System.out.println("  " + TerminalUtils.AMBER + "4" + TerminalUtils.RESET + "  Register new LlmModel");
            System.out.println("  " + TerminalUtils.AMBER + "5" + TerminalUtils.RESET + "  Decommission LlmModel");
            
            System.out.println("\n  " + TerminalUtils.GOLD + "--- SUITABILITY MATRIX ---" + TerminalUtils.RESET);
            System.out.println("  " + TerminalUtils.AMBER + "6" + TerminalUtils.RESET + "  Add suitability score");
            System.out.println("  " + TerminalUtils.AMBER + "7" + TerminalUtils.RESET + "  Edit suitability score");
            
            System.out.println("\n  " + TerminalUtils.AMBER + "B" + TerminalUtils.RESET + "  Back to Main Dashboard");
            System.out.println();
            TerminalUtils.printPrompt(ctx.username());
            
            String choice = ctx.scanner().nextLine().trim().toUpperCase();
            if ("B".equals(choice)) break;
            
            switch (choice) {
                case "1" -> listUsers();
                case "2" -> deleteUser();
                case "3" -> updateUser(); // Phase 1.1
                case "4" -> registerModel();
                case "5" -> decommissionModel();
                case "6" -> mapSuitability(true); // Phase 1.3
                case "7" -> mapSuitability(false); // Phase 1.3
            }
        }
    }

    private void listUsers() {
        TerminalUtils.printSeparator("REGISTERED USERS");
        List<User> users = ctx.userService().getAllUsers();
        String[] headers = {"ID", "Username", "Role", "Registered"};
        String[][] rows = new String[users.size()][4];
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            rows[i] = new String[]{
                String.valueOf(u.getId()), u.getUsername(),
                "ADMIN".equals(u.getRole()) ? TerminalUtils.GOLD + u.getRole() + TerminalUtils.RESET : u.getRole(),
                u.getCreatedAt().toLocalDate().toString()
            };
        }
        System.out.println();
        TerminalUtils.printTable(headers, rows);
    }

    private void deleteUser() {
        System.out.print("  User ID to delete: ");
        int id = ctx.safeInt(ctx.scanner().nextLine());
        if (id == ctx.userId()) { TerminalUtils.printError("Cannot delete yourself."); return; }
        if (id > 0) {
            ctx.userService().deleteUser(id);
            TerminalUtils.printSuccess("User deleted.");
        }
    }

    private void updateUser() {
        TerminalUtils.printSeparator("UPDATE USER");
        System.out.print("  User ID to update: ");
        int id = ctx.safeInt(ctx.scanner().nextLine());
        if (id <= 0) return;
        
        var userOpt = ctx.userService().getAllUsers().stream().filter(u -> u.getId().equals(id)).findFirst();
        if (userOpt.isEmpty()) { TerminalUtils.printError("User not found."); return; }
        
        User user = userOpt.get();
        System.out.printf("  Current username: %s%n", user.getUsername());
        System.out.print("  New username (leave blank to keep): ");
        String nu = ctx.scanner().nextLine().trim();
        if (!nu.isEmpty()) user.setUsername(nu);
        
        System.out.print("  New password (leave blank to keep): ");
        String np = ctx.scanner().nextLine().trim();
        if (!np.isEmpty()) user.setPasswordHash(SecurityUtils.hashPassword(np));
        
        if (!nu.isEmpty() || !np.isEmpty()) {
            try {
                ctx.userService().updateUser(id, nu, np);
                TerminalUtils.printSuccess("User updated successfully.");
            } catch (Exception e) {
                TerminalUtils.printError("Failed to update user: " + e.getMessage());
            }
        } else {
            TerminalUtils.printInfo("No changes made.");
        }
    }

    private void registerModel() {
        TerminalUtils.printSeparator("REGISTER NEW MODEL");
        System.out.print("  Model name (e.g. gpt-4): "); String name = ctx.scanner().nextLine().trim();
        System.out.print("  Provider: "); String provInput = ctx.scanner().nextLine().trim();
        System.out.print("  Cost per 1k tokens (USD): "); double cost = ctx.safeDouble(ctx.scanner().nextLine());
        if (name.isEmpty() || provInput.isEmpty() || Double.isNaN(cost)) {
            TerminalUtils.printError("Invalid input."); return;
        }

        String prov = Provider.fromAny(provInput)
            .map(Provider::getDisplayName)
            .orElse(provInput);
        
        LlmModel m = new LlmModel(null, name, prov, cost, null);
        ctx.modelDao().create(m);
        TerminalUtils.printSuccess("Model registered successfully. Use 'Add suitability score' next.");
    }

    private void decommissionModel() {
        System.out.print("  Model ID to decommission: ");
        int id = ctx.safeInt(ctx.scanner().nextLine());
        if (id > 0) {
            ctx.modelDao().delete(id);
            TerminalUtils.printSuccess("Model decommissioned.");
        }
    }

    private void mapSuitability(boolean isNew) {
        TerminalUtils.printSeparator(isNew ? "ADD SUITABILITY" : "EDIT SUITABILITY");
        List<LlmModel> models = ctx.modelDao().findAll();
        for (LlmModel m : models) System.out.printf("  [%d] %s (%s)%n", m.getId(), m.getName(), m.getProvider());
        System.out.print("  Model ID: ");
        int mid = ctx.safeInt(ctx.scanner().nextLine());
        if (models.stream().noneMatch(m -> m.getId().equals(mid))) { TerminalUtils.printError("Invalid Model ID."); return; }
        
        TaskType t = ctx.pickTask();
        System.out.print("  Score (0.0 to 1.0): ");
        double score = ctx.safeDouble(ctx.scanner().nextLine());
        if (Double.isNaN(score) || score < 0 || score > 1) { TerminalUtils.printError("Invalid score."); return; }
        
        if (isNew) {
            ctx.suitabilityDao().create(new ModelSuitability(null, mid, t, score, null));
            TerminalUtils.printSuccess("Suitability mapping created.");
        } else {
            // Find existing to update
            var suits = ctx.suitabilityDao().findByTaskType(t);
            var existing = suits.stream().filter(s -> s.getModelId().equals(mid)).findFirst();
            if (existing.isPresent()) {
                ModelSuitability s = existing.get();
                s.setBaseScore((float)score);
                ctx.suitabilityDao().update(s);
                TerminalUtils.printSuccess("Suitability mapping updated.");
            } else {
                TerminalUtils.printError("No existing mapping to edit. Use 'Add suitability' instead.");
            }
        }
    }
}
