package com.nexus.presentation;

import com.nexus.domain.LlmModel;
import com.nexus.domain.ModelSuitability;
import com.nexus.domain.TaskType;
import com.nexus.util.TerminalUtils;

import java.util.List;

/**
 * Model Discovery menu — list, filter, and view suitability matrix.
 */
public class ModelMenu {
    private final MenuContext ctx;

    public ModelMenu(MenuContext ctx) { this.ctx = ctx; }

    public void show() {
        TerminalUtils.printHeader("Model Discovery");
        System.out.println("  " + TerminalUtils.AMBER + "1" + TerminalUtils.RESET + "  List all models");
        System.out.println("  " + TerminalUtils.AMBER + "2" + TerminalUtils.RESET + "  Filter by provider");
        System.out.println("  " + TerminalUtils.AMBER + "3" + TerminalUtils.RESET + "  View suitability matrix");
        System.out.println("  " + TerminalUtils.AMBER + "B" + TerminalUtils.RESET + "  Back");
        System.out.println();
        TerminalUtils.printPrompt(ctx.username());
        switch (ctx.scanner().nextLine().trim().toUpperCase()) {
            case "1" -> listAllModels();
            case "2" -> filterByProvider();
            case "3" -> suitabilityMatrix();
        }
    }

    public void listAllModels() {
        List<LlmModel> models = ctx.modelDao().findAll();
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
        System.out.print("  Provider name: "); String q = ctx.scanner().nextLine().trim();
        List<LlmModel> filtered = ctx.modelDao().findByProvider(q);
        if (filtered.isEmpty()) { TerminalUtils.printInfo("No models found for provider: " + q); return; }
        System.out.println();
        for (LlmModel m : filtered)
            System.out.printf("  [%d] %s — $%.5f/1k%n", m.getId(), m.getName(), m.getCostPer1kTokens());
    }

    private void suitabilityMatrix() {
        TerminalUtils.printSeparator("SUITABILITY MATRIX");
        List<LlmModel>       models = ctx.modelDao().findAll();
        TaskType[]           tasks  = TaskType.values();
        String[] headers = new String[tasks.length + 1];
        headers[0] = "Model";
        for (int i = 0; i < tasks.length; i++) headers[i + 1] = tasks[i].name().substring(0, Math.min(5, tasks[i].name().length()));
        String[][] rows = new String[models.size()][headers.length];
        for (int r = 0; r < models.size(); r++) {
            LlmModel m = models.get(r);
            rows[r][0] = m.getName();
            for (int c = 0; c < tasks.length; c++) {
                List<ModelSuitability> suits = ctx.suitabilityDao().findByTaskType(tasks[c]);
                double score = suits.stream().filter(s -> s.getModelId().equals(m.getId()))
                    .mapToDouble(ModelSuitability::getBaseScore).max().orElse(0.0);
                rows[r][c + 1] = score == 0 ? TerminalUtils.GRAY + "  ─  " + TerminalUtils.RESET :
                    TerminalUtils.AMBER + String.format("%.2f", score) + TerminalUtils.RESET;
            }
        }
        System.out.println();
        TerminalUtils.printTable(headers, rows);
    }
}
