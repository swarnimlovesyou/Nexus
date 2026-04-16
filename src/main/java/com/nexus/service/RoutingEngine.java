package com.nexus.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
     * Method Overload 3: User-aware routing — backward-compatible convenience wrapper.
     * Returns the best key-accessible model, or the unconstrained best if no keys are stored.
     * For richer Key-missing information, use selectWithResult() instead.
     */
    public LlmModel selectOptimalModelForUser(TaskType taskType, double maxCostPer1k, Integer userId) {
        return selectWithResult(taskType, maxCostPer1k, userId).recommended();
    }

    /**
     * Full routing decision, returns a RoutingResult that tells the caller:
     *  - recommended()        : best model the user CAN use (has a key, or no keys configured)
     *  - optimalWithoutKey()  : the globally top-scoring model, even if no key is stored
     *  - keyMissing()         : true when the unconstrained optimal differs from recommended
     *
     * This allows callers (e.g. RoutingMenu) to say:
     *   "The optimal model is X but you don't have the key — using Y instead."
     */
    public RoutingResult selectWithResult(TaskType taskType, double maxCostPer1k, Integer userId) {
        List<ModelSuitability> suitabilities = suitabilityDao.findByTaskType(taskType);
        if (suitabilities.isEmpty()) {
            throw new IllegalStateException("No suitability profiles found for: " + taskType);
        }

        Set<Provider> accessibleProviders = getAccessibleProviders(userId);

        List<OutcomeMemory> history = outcomeMemoryDao.findByTaskType(taskType);
        Map<Integer, List<OutcomeMemory>> histByModel = new HashMap<>();
        for (OutcomeMemory m : history) {
            histByModel.computeIfAbsent(m.getModelId(), k -> new ArrayList<>()).add(m);
        }

        double maxLatency = history.stream().mapToDouble(OutcomeMemory::getLatencyMs).max().orElse(3000);
        double maxCost    = suitabilities.stream()
            .map(s -> llmModelDao.read(s.getModelId()))
            .filter(Optional::isPresent).map(Optional::get)
            .mapToDouble(LlmModel::getCostPer1kTokens).max().orElse(0.10);

        // --- Pass 1: Score ALL eligible models regardless of key --------------------------------
        LlmModel globalBest      = null;
        double   globalBestScore = -1.0;

        // --- Pass 2: Score only models the user has a key for ----------------------------------
        LlmModel keyedBest      = null;
        double   keyedBestScore = -1.0;

        for (ModelSuitability ms : suitabilities) {
            Optional<LlmModel> modelOpt = llmModelDao.read(ms.getModelId());
            if (modelOpt.isEmpty()) continue;
            LlmModel model = modelOpt.get();
            if (model.getCostPer1kTokens() > maxCostPer1k) continue;

            List<OutcomeMemory> modelHist = histByModel.getOrDefault(model.getId(), new ArrayList<>());
            double composite = computeComposite(ms, model, modelHist, maxLatency, maxCost);

            // Track unconstrained global best
            if (composite > globalBestScore) {
                globalBestScore = composite;
                globalBest = model;
            }

            // Track best among key-accessible providers only
            if (!accessibleProviders.isEmpty()) {
                Optional<Provider> modelProvider = Provider.fromAny(model.getProvider());
                boolean hasKey = modelProvider.isPresent() && accessibleProviders.contains(modelProvider.get());
                if (hasKey && composite > keyedBestScore) {
                    keyedBestScore = composite;
                    keyedBest = model;
                }
            }
        }

        // If user has no keys configured at all, treat everything as accessible
        boolean noKeysConfigured = accessibleProviders.isEmpty();
        LlmModel recommended = noKeysConfigured ? globalBest : keyedBest;

        // Key is "missing" when the global champion is a different model than the keyed champion
        boolean keyMissing = !noKeysConfigured
            && globalBest != null
            && (recommended == null || !globalBest.getId().equals(recommended.getId()));

        // Persist audit record
        final LlmModel auditModel = recommended != null ? recommended : globalBest;
        if (auditModel != null) {
            List<ModelSuitability> suits = suitabilityDao.findByTaskType(taskType);
            double suitScore = suits.stream()
                .filter(s -> s.getModelId().equals(auditModel.getId()))
                .mapToDouble(ModelSuitability::getBaseScore).findFirst().orElse(0.0);
            List<OutcomeMemory> bestHist = histByModel.getOrDefault(auditModel.getId(), new ArrayList<>());
            double qualScore = bestHist.isEmpty() ? 0.5 : bestHist.stream().mapToDouble(OutcomeMemory::getQualityScore).average().orElse(0.5);
            double latScore  = bestHist.isEmpty() ? 0.5 : 1.0 - (bestHist.stream().mapToDouble(OutcomeMemory::getLatencyMs).average().orElse(1500) / Math.max(1, maxLatency));
            double costScore = maxCost == 0 ? 1.0 : 1.0 - (auditModel.getCostPer1kTokens() / maxCost);
            double score     = recommended == auditModel ? (keyedBestScore > 0 ? keyedBestScore : globalBestScore) : globalBestScore;

            String richDetails = String.format(
                "task=%s | model=%s | composite=%.3f | suit=%.2f | qual=%.2f | lat=%.2f | cost=%.2f | samples=%d | keyPresent=%b | keyMissing=%b",
                taskType, auditModel.getName(), score,
                suitScore, qualScore, latScore, costScore,
                bestHist.size(), !keyMissing, keyMissing
            );
            auditLogDao.create(new AuditLog(null, userId, "ROUTING_DECISION", richDetails, "SUCCESS", null));
        }

        return new RoutingResult(recommended, globalBest, keyMissing);
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

        Set<Provider> accessibleProviders = getAccessibleProviders(userId);

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

            Optional<Provider> modelProvider = Provider.fromAny(model.getProvider());
            // Mark whether user has a key for this provider
            boolean hasKey = accessibleProviders.isEmpty() || modelProvider.map(accessibleProviders::contains).orElse(false);
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
     * Autonomous Calibration: Recalibrates base suitability scores in the DB 
     * based on the actual quality and latency captured in OutcomeHistory.
     * This makes the routing engine self-learning.
     */
    public int recalibrateScores(Integer userId) {
        List<ModelSuitability> allSuitabilities = suitabilityDao.findAll();
        int updateCount = 0;

        for (ModelSuitability ms : allSuitabilities) {
            List<OutcomeMemory> history = outcomeMemoryDao.findByUserAndTaskType(userId, ms.getTaskType());
            List<OutcomeMemory> modelHistory = history.stream()
                .filter(o -> o.getModelId().equals(ms.getModelId()))
                .toList();

            if (modelHistory.size() < 3) continue; // Need at least 3 samples to adjust

            double avgQuality = modelHistory.stream().mapToDouble(OutcomeMemory::getQualityScore).average().orElse(0.5);
            
            // Adjust base score by moving it 10% towards the actual experienced quality.
            double oldScore = ms.getBaseScore();
            double newScore = oldScore + (avgQuality - oldScore) * 0.10;
            
            // Clamp score between 0.1 and 1.0
            newScore = Math.max(0.1, Math.min(1.0, newScore));
            
            if (Math.abs(newScore - oldScore) > 0.005) {
                ms.setBaseScore(newScore);
                suitabilityDao.update(ms);
                updateCount++;
                
                String log = String.format("Auto-calibrated model %d for %s: %.2f -> %.2f (samples=%d)", 
                    ms.getModelId(), ms.getTaskType(), oldScore, newScore, modelHistory.size());
                auditLogDao.create(new AuditLog(null, userId, "AUTO_CALIBRATION", log, "SUCCESS", null));
            }
        }
        return updateCount;
    }

    /**
     * Returns lowercase provider names for which the user has stored API keys.
     * Returns empty set if userId is null (no filtering applied).
     */
    private Set<Provider> getAccessibleProviders(Integer userId) {
        if (userId == null) return Collections.emptySet();
        Set<Provider> providers = new HashSet<>();
        for (Provider p : Provider.values()) {
            if (apiKeyService.hasKeyForProvider(userId, p)) {
                providers.add(p);
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

    /**
     * Full routing decision result.
     *
     * recommended()       — model to actually use (has API key, or no keys configured at all)
     * optimalWithoutKey() — globally highest-scoring model regardless of whether a key exists
     * keyMissing()        — true when the unconstrained optimal model has no key stored for it,
     *                       so recommended() is a fallback, not the true best
     */
    public record RoutingResult(
        LlmModel recommended,
        LlmModel optimalWithoutKey,
        boolean keyMissing
    ) {
        /** Convenience: true when recommended == null (no usable model at all) */
        public boolean noModelAvailable() { return recommended == null && optimalWithoutKey == null; }
    }
}
