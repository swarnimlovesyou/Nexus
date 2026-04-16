package com.nexus.presentation;

import java.util.List;

import com.nexus.domain.OutcomeMemory;
import com.nexus.domain.TaskType;
import com.nexus.util.TerminalUtils;

/**
 * Execution History menu.
 * Includes Update and Delete capabilities for OutcomeMemory records (Phase 1.5).
 */
public class HistoryMenu {
    private final MenuContext ctx;

    public HistoryMenu(MenuContext ctx) { this.ctx = ctx; }

    public void show() {
        while (true) {
            TerminalUtils.printHeader("Execution History");
            System.out.println("  " + TerminalUtils.AMBER + "1" + TerminalUtils.RESET + "  All executions");
            System.out.println("  " + TerminalUtils.AMBER + "2" + TerminalUtils.RESET + "  Filter by task type");
            System.out.println("  " + TerminalUtils.AMBER + "3" + TerminalUtils.RESET + "  Filter by model ID");
            System.out.println("  " + TerminalUtils.AMBER + "4" + TerminalUtils.RESET + "  Search by date range");
            System.out.println("  " + TerminalUtils.AMBER + "5" + TerminalUtils.RESET + "  Update execution quality score");
            System.out.println("  " + TerminalUtils.AMBER + "6" + TerminalUtils.RESET + "  Delete execution record");
            System.out.println("  " + TerminalUtils.AMBER + "B" + TerminalUtils.RESET + "  Back");
            System.out.println();
            TerminalUtils.printPrompt(ctx.username());

            String choice = ctx.scanner().nextLine().trim().toUpperCase();
            if (choice.equals("B")) return;

            switch (choice) {
                case "1" -> ctx.runWithDaoGuard("Unable to load execution history right now. Please try again.", this::listAllHistory);
                case "2" -> ctx.runWithDaoGuard("Unable to filter history by task right now. Please try again.", this::filterByTask);
                case "3" -> ctx.runWithDaoGuard("Unable to filter history by model right now. Please try again.", this::filterByModel);
                case "4" -> ctx.runWithDaoGuard("Unable to search history by date right now. Please try again.", this::searchByDateRange);
                case "5" -> ctx.runWithDaoGuard("Could not update execution quality. Database operation failed; no changes were saved.", this::updateQuality);
                case "6" -> ctx.runWithDaoGuard("Could not delete execution record. Database operation failed; no changes were saved.", this::deleteRecord);
                default -> TerminalUtils.printError("Unknown option.");
            }
        }
    }

    private void listAllHistory() {
        List<OutcomeMemory> history = ctx.outcomeDao().findByUserId(ctx.userId());
        displayHistory(history);
    }

    private void filterByTask() {
        TaskType task = ctx.pickTask();
        List<OutcomeMemory> history = ctx.outcomeDao().findByUserAndTaskType(ctx.userId(), task);
        displayHistory(history);
    }

    private void filterByModel() {
        List<com.nexus.domain.LlmModel> models = ctx.modelDao().findAll();
        if (models.isEmpty()) { TerminalUtils.printInfo("No models registered."); return; }
        System.out.println();
        for (int i = 0; i < models.size(); i++)
            System.out.printf("  " + TerminalUtils.AMBER + "%d" + TerminalUtils.RESET + "  %s (%s)%n",
                i + 1, models.get(i).getName(), models.get(i).getProvider());
        System.out.print("  Model # (1-" + models.size() + "): ");
        int midx = ctx.safeInt(ctx.scanner().nextLine()) - 1;
        if (midx < 0 || midx >= models.size()) {
            TerminalUtils.printError("Invalid selection.");
            return;
        }
        int mid = models.get(midx).getId();
        List<OutcomeMemory> history = ctx.outcomeDao().findByUserAndModelId(ctx.userId(), mid);
        displayHistory(history);
    }

    private void searchByDateRange() {
        System.out.print("  From date (YYYY-MM-DD): ");
        String fromStr = ctx.scanner().nextLine().trim();
        System.out.print("  To date   (YYYY-MM-DD): ");
        String toStr = ctx.scanner().nextLine().trim();

        try {
            java.time.LocalDateTime from = java.time.LocalDate.parse(fromStr).atStartOfDay();
            java.time.LocalDateTime to = java.time.LocalDate.parse(toStr).atTime(23, 59, 59);
            List<OutcomeMemory> history = ctx.outcomeDao().findByUserAndDateRange(ctx.userId(), from, to);
            TerminalUtils.printInfo("Records from " + fromStr + " to " + toStr);
            displayHistory(history);
        } catch (Exception e) {
            TerminalUtils.printError("Invalid date format. Use YYYY-MM-DD (e.g. 2026-04-01).");
        }
    }

    private void displayHistory(List<OutcomeMemory> history) {
        if (history.isEmpty()) { TerminalUtils.printInfo("No execution records found."); return; }
        System.out.println();
        String[] headers = {"ID", "Task", "Model ID", "Cost Usd", "Latency", "Quality", "Date"};
        String[][] rows = new String[Math.min(history.size(), 20)][7];
        for (int i = 0; i < rows.length; i++) {
            OutcomeMemory o = history.get(i);
            String dateStr = o.getCreatedAt() != null ? o.getCreatedAt().toLocalDate().toString() : "—";
            rows[i] = new String[]{
                String.valueOf(o.getId()), o.getTaskType().name(), String.valueOf(o.getModelId()),
                String.format("%.5f", o.getCost()), o.getLatencyMs() + "ms",
                String.format("%.2f", o.getQualityScore()),
                dateStr
            };
        }
        TerminalUtils.printTable(headers, rows);
        if (history.size() > 20) TerminalUtils.printInfo("Showing 20 most recent of " + history.size() + " total records.");
    }

    private void updateQuality() {
        TerminalUtils.printSeparator("UPDATE QUALITY SCORE");
        System.out.print("  Execution Record ID: ");
        int id = ctx.safeInt(ctx.scanner().nextLine());
        if (id <= 0) return;
        
        var opt = ctx.outcomeDao().read(id);
        if (opt.isEmpty()) { TerminalUtils.printError("Record not found."); return; }
        
        OutcomeMemory o = opt.get();
        if (!o.getUserId().equals(ctx.userId())) {
            TerminalUtils.printError("Access denied. You can update only your own records.");
            return;
        }
        System.out.printf("  Current Quality: %.2f%n", o.getQualityScore());
        System.out.print("  New Quality (0.0-1.0): ");
        double q = ctx.safeDouble(ctx.scanner().nextLine());
        if (Double.isNaN(q) || q < 0 || q > 1.0) { TerminalUtils.printError("Invalid quality score."); return; }
        
        o.setQualityScore(q);
        ctx.outcomeDao().update(o);
        TerminalUtils.printSuccess("Quality score updated. Router intelligence recalibrated.");
    }

    private void deleteRecord() {
        TerminalUtils.printSeparator("DELETE EXECUTION RECORD");
        System.out.print("  Execution Record ID: ");
        int id = ctx.safeInt(ctx.scanner().nextLine());
        if (id <= 0) return;

        var opt = ctx.outcomeDao().read(id);
        if (opt.isEmpty()) { TerminalUtils.printError("Record not found."); return; }
        if (!opt.get().getUserId().equals(ctx.userId())) {
            TerminalUtils.printError("Access denied. You can delete only your own records.");
            return;
        }
        
        System.out.print("  Are you sure? (yes/no): ");
        if ("yes".equalsIgnoreCase(ctx.scanner().nextLine().trim())) {
            ctx.outcomeDao().delete(id);
            TerminalUtils.printSuccess("Record #" + id + " deleted.");
        }
    }
}
