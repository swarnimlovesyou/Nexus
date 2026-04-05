package com.nexus.presentation;

import com.nexus.domain.*;
import com.nexus.util.TerminalUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Financial Intelligence Dashboard — actual vs optimal spend analysis.
 * Includes bug fix: correctly calculates optimal spend by multiplying the optimal model's token rate.
 */
public class FinanceMenu {
    private final MenuContext ctx;
    private final Map<TaskType, Map<Integer, Double>> suitabilityCache;

    public FinanceMenu(MenuContext ctx) {
        this.ctx = ctx;
        this.suitabilityCache = new LinkedHashMap<>();
    }

    public void show() {
        while (true) {
            TerminalUtils.printSeparator("FINANCIAL DASHBOARD");
            System.out.println("  1. Lifetime Report");
            System.out.println("  2. Last 7 Days");
            System.out.println("  3. Last 30 Days");
            System.out.println("  B. Back");
            System.out.println();
            TerminalUtils.printPrompt(ctx.username());

            String input = ctx.scanner().nextLine().trim().toLowerCase();
            if ("b".equals(input)) return;

            List<OutcomeMemory> history = ctx.outcomeDao().findByUserId(ctx.userId());
            if (history.isEmpty()) {
                TerminalUtils.printInfo("No execution history available for cost analysis.");
                return;
            }

            String label;
            List<OutcomeMemory> scoped;
            switch (input) {
                case "1" -> {
                    label = "LIFETIME";
                    scoped = history;
                }
                case "2" -> {
                    label = "LAST 7 DAYS";
                    scoped = filterByRange(history, LocalDateTime.now().minusDays(7));
                }
                case "3" -> {
                    label = "LAST 30 DAYS";
                    scoped = filterByRange(history, LocalDateTime.now().minusDays(30));
                }
                default -> {
                    TerminalUtils.printError("Unknown option.");
                    continue;
                }
            }

            if (scoped.isEmpty()) {
                TerminalUtils.printInfo("No executions recorded for " + label + ".");
                continue;
            }

            TerminalUtils.spinner("Compiling cross-model cost analytics...", 700);
            renderReport(scoped, label, 0.70);
        }
    }

    private List<OutcomeMemory> filterByRange(List<OutcomeMemory> history, LocalDateTime cutoff) {
        List<OutcomeMemory> filtered = new ArrayList<>();
        for (OutcomeMemory o : history) {
            if (o.getCreatedAt() != null && (o.getCreatedAt().isEqual(cutoff) || o.getCreatedAt().isAfter(cutoff))) {
                filtered.add(o);
            }
        }
        return filtered;
    }

    private void renderReport(List<OutcomeMemory> history, String label, double qualityThreshold) {
        double actualSpend = 0.0;
        double optimalSpend = 0.0;
        double avgQuality = 0.0;
        Map<String, Double> spendByModel = new LinkedHashMap<>();
        Map<TaskType, Double> spendByTask = new LinkedHashMap<>();
        Map<TaskType, Integer> callsByTask = new LinkedHashMap<>();
        Map<Integer, LlmModel> modelById = new LinkedHashMap<>();

        List<LlmModel> models = ctx.modelDao().findAll();
        for (LlmModel model : models) {
            modelById.put(model.getId(), model);
        }

        for (OutcomeMemory o : history) {
            actualSpend += o.getCost();
            avgQuality += o.getQualityScore();

            spendByTask.merge(o.getTaskType(), o.getCost(), Double::sum);
            callsByTask.merge(o.getTaskType(), 1, Integer::sum);

            LlmModel usedModel = modelById.get(o.getModelId());
            if (usedModel != null) {
                spendByModel.merge(usedModel.getName(), o.getCost(), Double::sum);
            }

            if (usedModel == null || usedModel.getCostPer1kTokens() <= 0) {
                optimalSpend += o.getCost();
                continue;
            }

            double minCost = findCheapestSuitableRate(o.getTaskType(), models, qualityThreshold);
            if (minCost < Double.MAX_VALUE) {
                // Outcome records store realized cost, so we estimate token volume from used rate.
                double estimatedTokens = (o.getCost() / usedModel.getCostPer1kTokens()) * 1000.0;
                optimalSpend += minCost * (estimatedTokens / 1000.0);
            } else {
                optimalSpend += o.getCost();
            }
        }

        avgQuality = avgQuality / history.size();
        double savings = Math.max(0.0, actualSpend - optimalSpend);

        System.out.println("\n  " + TerminalUtils.GOLD + "=== TOTAL SPEND (" + label + ") ===" + TerminalUtils.RESET);
        System.out.printf("  Executions             : %d%n", history.size());
        System.out.printf("  Actual recorded spend : $%.6f%n", actualSpend);
        System.out.printf("  Optimal routed spend  : $%.6f%n", optimalSpend);
        System.out.printf("  Avg quality score     : %.2f%n", avgQuality);

        if (savings > 0.00001) {
            System.out.printf("  Avoidable spend       : " + TerminalUtils.RED + "$%.6f" + TerminalUtils.RESET + " (Left on the table)%n", savings);
        } else {
            System.out.println("  Avoidable spend       : " + TerminalUtils.GREEN + "$0.00000" + TerminalUtils.RESET + " (Perfect routing)");
        }

        System.out.println("\n  " + TerminalUtils.GOLD + "=== SPEND BY MODEL ===" + TerminalUtils.RESET);
        final double finalActualSpend = actualSpend;
        spendByModel.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .forEach(e -> {
                double pct = finalActualSpend <= 0 ? 0.0 : (e.getValue() / finalActualSpend) * 100;
                System.out.printf("  %-20s : $%.6f  (%5.1f%%)%n", e.getKey(), e.getValue(), pct);
            });

        System.out.println("\n  " + TerminalUtils.GOLD + "=== SPEND BY TASK ===" + TerminalUtils.RESET);
        spendByTask.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .forEach(e -> {
                int calls = callsByTask.getOrDefault(e.getKey(), 0);
                double avg = calls == 0 ? 0.0 : e.getValue() / calls;
                System.out.printf("  %-20s : total=%-12s calls=%-4d avg=%s%n",
                    e.getKey().name(),
                    String.format("$%.6f", e.getValue()),
                    calls,
                    String.format("$%.6f", avg));
            });
    }

    private double findCheapestSuitableRate(TaskType taskType, List<LlmModel> models, double threshold) {
        Map<Integer, Double> suitabilityByModel = suitabilityCache.computeIfAbsent(taskType, t -> {
            Map<Integer, Double> map = new LinkedHashMap<>();
            for (ModelSuitability suitability : ctx.suitabilityDao().findByTaskType(t)) {
                map.merge(suitability.getModelId(), suitability.getBaseScore(), Math::max);
            }
            return map;
        });

        double minCost = Double.MAX_VALUE;
        for (LlmModel model : models) {
            double suitability = suitabilityByModel.getOrDefault(model.getId(), 0.0);
            if (suitability >= threshold && model.getCostPer1kTokens() < minCost) {
                minCost = model.getCostPer1kTokens();
            }
        }
        return minCost;
    }
}
