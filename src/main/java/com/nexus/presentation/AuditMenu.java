package com.nexus.presentation;

import java.util.List;

import com.nexus.domain.AuditLog;
import com.nexus.util.TerminalUtils;

/**
 * Audit Log Menu.
 * Includes search functionality (Phase 1.4).
 */
public class AuditMenu {
    private final MenuContext ctx;

    public AuditMenu(MenuContext ctx) { this.ctx = ctx; }

    public void show() {
        TerminalUtils.printHeader("Security Audit Log");
        System.out.println("  " + TerminalUtils.AMBER + "1" + TerminalUtils.RESET + "  View all recent logs");
        System.out.println("  " + TerminalUtils.AMBER + "2" + TerminalUtils.RESET + "  Filter by Action Type");
        System.out.println("  " + TerminalUtils.AMBER + "3" + TerminalUtils.RESET + "  Search records");
        System.out.println("  " + TerminalUtils.AMBER + "B" + TerminalUtils.RESET + "  Back");
        System.out.println();
        TerminalUtils.printPrompt(ctx.username());

        String choice = ctx.scanner().nextLine().trim().toUpperCase();
        if (choice.equals("B")) return;

        ctx.runWithDaoGuard("Unable to load audit logs right now. Please try again.", () -> renderAuditSelection(choice));
    }

    private void renderAuditSelection(String choice) {
        List<AuditLog> fullLog = "ADMIN".equals(ctx.loggedInUser().getRole())
            ? ctx.auditLogDao().findAll()
            : ctx.auditLogDao().findByUserId(ctx.userId());
        List<AuditLog> displayed;

        switch (choice) {
            case "2" -> {
                System.out.print("  Action Type (e.g. LOGIN_SUCCESS): ");
                String action = ctx.scanner().nextLine().trim().toUpperCase();
                displayed = fullLog.stream().filter(a -> a.getAction().contains(action)).toList();
            }
            case "3" -> {
                System.out.print("  Search keyword: ");
                String keyword = ctx.scanner().nextLine().trim().toLowerCase();
                displayed = fullLog.stream().filter(a -> 
                    (a.getDetails() != null && a.getDetails().toLowerCase().contains(keyword)) ||
                    a.getAction().toLowerCase().contains(keyword) ||
                    (a.getOutcome() != null && a.getOutcome().toLowerCase().contains(keyword))
                ).toList();
            }
            default -> displayed = fullLog;
        }

        if (displayed.isEmpty()) {
            TerminalUtils.printInfo("No audit logs match criteria.");
            return;
        }

        System.out.println();
        String[] headers = {"ID", "User ID", "Action", "Outcome", "Timestamp", "Details"};
        // Limit to 25 rows for console readability
        int limit = Math.min(displayed.size(), 25);
        String[][] rows = new String[limit][6];
        for (int i = 0; i < limit; i++) {
            AuditLog a = displayed.get(i);
            rows[i] = new String[]{
                String.valueOf(a.getId()),
                a.getUserId() == null ? "SYSTEM" : String.valueOf(a.getUserId()),
                TerminalUtils.AMBER + a.getAction() + TerminalUtils.RESET,
                "SUCCESS".equals(a.getOutcome()) ? TerminalUtils.GREEN + a.getOutcome() + TerminalUtils.RESET : TerminalUtils.RED + a.getOutcome() + TerminalUtils.RESET,
                a.getCreatedAt().toString(),
                a.getDetails() != null ? (a.getDetails().length() > 40 ? a.getDetails().substring(0, 37) + "..." : a.getDetails()) : ""
            };
        }
        TerminalUtils.printTable(headers, rows);
        if (displayed.size() > 25) {
            TerminalUtils.printInfo("Displaying latest 25 of " + displayed.size() + " matched records.");
        }
    }
}
