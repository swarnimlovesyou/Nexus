package com.nexus.service;

import com.nexus.dao.AuditLogDao;
import com.nexus.dao.LlmModelDao;
import com.nexus.dao.OutcomeMemoryDao;
import com.nexus.dao.SuitabilityDao;
import com.nexus.domain.AuditLog;
import com.nexus.domain.LlmModel;
import com.nexus.domain.ModelSuitability;
import com.nexus.domain.OutcomeMemory;
import com.nexus.domain.Provider;
import com.nexus.domain.TaskType;

import java.util.*;

public class RoutingEngine {
    private final SuitabilityDao suitabilityDao;
    private final OutcomeMemoryDao outcomeMemoryDao;
    private final LlmModelDao llmModelDao;
    private final AuditLogDao auditLogDao;
    private final ApiKeyService apiKeyService;

    // Weights for composite scoring
    private static final double W_SUITABILITY = 0.35;
    private static final double W_QUALITY     = 0.30;
    private static final double W_LATENCY     = 0.15;
    private static final double W_COST        = 0.20;

    public RoutingEngine() {
        this.suitabilityDao   = new SuitabilityDao();
        this.outcomeMemoryDao = new OutcomeMemoryDao();
        this.llmModelDao      = new LlmModelDao();
        this.auditLogDao      = new AuditLogDao();
        this.apiKeyService    = new ApiKeyService();
    }

    /** Method Overload 1: No budget constraint */
    public LlmModel selectOptimalModel(TaskType taskType) {
        return selectOptimalModel(taskType, Double.MAX_VALUE);
    }

    /** Method Overload 2: With budget cap (cost per 1k tokens) */
    public LlmModel selectOptimalModel(TaskType taskType, double maxCostPer1k) {
        return selectOptimalModelForUser(taskType, maxCostPer1k, null);
    }

    /**
     * Method Overload 3: User-aware routing.
     * Filters models to only those the user has an API key for.
     * Falls back to all models if the user has no keys configured.
     */
    public LlmModel selectOptimalModelForUser(TaskType taskType, double maxCostPer1k, Integer userId) {
        List<ModelSuitability> suitabilities = suitabilityDao.findByTaskType(taskType);
        if (suitabilities.isEmpty()) {
            throw new IllegalStateException("No suitability profiles found for: " + taskType);
        }

        // Build set of providers user has keys for (null userId = no filter)
        Set<String> accessibleProviders = getAccessibleProviders(userId);

        List<OutcomeMemory> history = outcomeMemoryDao.findByTaskType(taskType);
        Map<Integer, List<OutcomeMemory>> histByModel = new HashMap<>();
        for (OutcomeMemory m : history) {
            histByModel.computeIfAbsent(m.getModelId(), k -> new ArrayList<>()).add(m);
        }

        // Compute max latency and cost for normalisation
        double maxLatency = history.stream().mapToDouble(OutcomeMemory::getLatencyMs).max().orElse(3000);
        double maxCost    = suitabilities.stream()
            .map(s -> llmModelDao.read(s.getModelId()))
            .filter(Optional::isPresent).map(Optional::get)
            .mapToDouble(LlmModel::getCostPer1kTokens).max().orElse(0.10);

        LlmModel bestModel    = null;
        double   highestScore = -1.0;

        for (ModelSuitability ms : suitabilities) {
            Optional<LlmModel> modelOpt = llmModelDao.read(ms.getModelId());
            if (modelOpt.isEmpty()) continue;
            LlmModel model = modelOpt.get();

            if (model.getCostPer1kTokens() > maxCostPer1k) continue;

            // Skip if user has keys configured but not for this provider
            if (!accessibleProviders.isEmpty() && !accessibleProviders.contains(model.getProvider().toLowerCase())) {
                continue;
            }

            List<OutcomeMemory> modelHist = histByModel.getOrDefault(model.getId(), new ArrayList<>());
            double composite = computeComposite(ms, model, modelHist, maxLatency, maxCost);

            if (composite > highestScore) {
                highestScore = composite;
                bestModel    = model;
            }
        }

        // If filtering by provider left no results, fall back to all models
        if (bestModel == null && !accessibleProviders.isEmpty()) {
            return selectOptimalModelForUser(taskType, maxCostPer1k, null);
        }

        if (bestModel != null) {
            auditLogDao.create(new AuditLog(null, userId, "ROUTING_DECISION",
                "task=" + taskType + " selected=" + bestModel.getName() + " score=" + String.format("%.3f", highestScore),
                "SUCCESS", null));
        }
        return bestModel;
    }

    /**
     * Explain routing — return a ranked breakdown of every candidate model with individual signal scores.
     */
    public List<ModelScoreBreakdown> explainRouting(TaskType taskType) {
        return explainRoutingForUser(taskType, null);
    }

    public List<ModelScoreBreakdown> explainRoutingForUser(TaskType taskType, Integer userId) {
        List<ModelSuitability> suitabilities = suitabilityDao.findByTaskType(taskType);
        List<OutcomeMemory>    history       = outcomeMemoryDao.findByTaskType(taskType);
        Map<Integer, List<OutcomeMemory>> histByModel = new HashMap<>();
        for (OutcomeMemory m : history) histByModel.computeIfAbsent(m.getModelId(), k -> new ArrayList<>()).add(m);

        Set<String> accessibleProviders = getAccessibleProviders(userId);

        double maxLatency = history.stream().mapToDouble(OutcomeMemory::getLatencyMs).max().orElse(3000);
        double maxCost    = suitabilities.stream()
            .map(s -> llmModelDao.read(s.getModelId()))
            .filter(Optional::isPresent).map(Optional::get)
            .mapToDouble(LlmModel::getCostPer1kTokens).max().orElse(0.10);

        List<ModelScoreBreakdown> breakdown = new ArrayList<>();
        for (ModelSuitability ms : suitabilities) {
            Optional<LlmModel> modelOpt = llmModelDao.read(ms.getModelId());
            if (modelOpt.isEmpty()) continue;
            LlmModel model = modelOpt.get();
            List<OutcomeMemory> modelHist = histByModel.getOrDefault(model.getId(), new ArrayList<>());

            double suitScore  = ms.getBaseScore();
            double qualScore  = modelHist.isEmpty() ? 0.5 : modelHist.stream().mapToDouble(OutcomeMemory::getQualityScore).average().orElse(0.5);
            double latScore   = modelHist.isEmpty() ? 0.5 : 1.0 - (modelHist.stream().mapToDouble(OutcomeMemory::getLatencyMs).average().orElse(1500) / maxLatency);
            double costScore  = maxCost == 0 ? 1.0 : 1.0 - (model.getCostPer1kTokens() / maxCost);
            double composite  = computeComposite(ms, model, modelHist, maxLatency, maxCost);

            // Mark whether user has a key for this provider
            boolean hasKey = accessibleProviders.isEmpty() || accessibleProviders.contains(model.getProvider().toLowerCase());
            breakdown.add(new ModelScoreBreakdown(model, suitScore, qualScore, latScore, costScore, composite, modelHist.size(), hasKey));
        }

        breakdown.sort(Comparator.comparingDouble(ModelScoreBreakdown::composite).reversed());
        return breakdown;
    }

    /**
     * What-if analysis: show best model at different price caps.
     */
    public List<String> whatIfBudget(TaskType taskType, double[] budgetTiers) {
        List<String> results = new ArrayList<>();
        for (double cap : budgetTiers) {
            try {
                LlmModel m = selectOptimalModel(taskType, cap);
                results.add(String.format("Budget $%.4f/1k → %s (%s)", cap, m != null ? m.getName() : "None", m != null ? m.getProvider() : ""));
            } catch (Exception e) {
                results.add(String.format("Budget $%.4f/1k → No model available", cap));
            }
        }
        return results;
    }

    private double computeComposite(ModelSuitability ms, LlmModel model,
                                     List<OutcomeMemory> hist, double maxLatency, double maxCost) {
        double suitScore = ms.getBaseScore();
        double qualScore = hist.isEmpty() ? 0.5 :
            hist.stream().mapToDouble(OutcomeMemory::getQualityScore).average().orElse(0.5);
        double latScore = hist.isEmpty() ? 0.5 :
            1.0 - (hist.stream().mapToDouble(OutcomeMemory::getLatencyMs).average().orElse(1500) / Math.max(1, maxLatency));
        double costScore = maxCost == 0 ? 1.0 :
            1.0 - (model.getCostPer1kTokens() / maxCost);

        return (suitScore * W_SUITABILITY)
             + (qualScore * W_QUALITY)
             + (latScore  * W_LATENCY)
             + (costScore * W_COST);
    }

    /**
     * Returns lowercase provider names for which the user has stored API keys.
     * Returns empty set if userId is null (no filtering applied).
     */
    private Set<String> getAccessibleProviders(Integer userId) {
        if (userId == null) return Collections.emptySet();
        Set<String> providers = new HashSet<>();
        for (Provider p : Provider.values()) {
            if (apiKeyService.hasKeyForProvider(userId, p)) {
                providers.add(p.getDisplayName().toLowerCase());
            }
        }
        return providers;
    }

    /** Data carrier for routing explanation */
    public record ModelScoreBreakdown(
        LlmModel model,
        double suitabilityScore,
        double qualityScore,
        double latencyScore,
        double costScore,
        double composite,
        int sampleSize,
        boolean hasApiKey
    ) {}
}
