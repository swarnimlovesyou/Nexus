package com.nexus.service;

import com.nexus.dao.LlmModelDao;
import com.nexus.dao.OutcomeMemoryDao;
import com.nexus.dao.SuitabilityDao;
import com.nexus.domain.LlmModel;
import com.nexus.domain.ModelSuitability;
import com.nexus.domain.OutcomeMemory;
import com.nexus.domain.TaskType;

import java.util.List;
import java.util.Optional;

public class RoutingEngine {
    private final SuitabilityDao suitabilityDao;
    private final OutcomeMemoryDao outcomeMemoryDao;
    private final LlmModelDao llmModelDao;

    public RoutingEngine() {
        this.suitabilityDao = new SuitabilityDao();
        this.outcomeMemoryDao = new OutcomeMemoryDao();
        this.llmModelDao = new LlmModelDao();
    }

    /**
     * Demonstrates Method Overloading.
     * Selects best model based purely on composite score with no cost constraints.
     */
    public LlmModel selectOptimalModel(TaskType taskType) {
        return selectOptimalModel(taskType, Double.MAX_VALUE);
    }

    /**
     * Demonstrates Method Overloading.
     * Selects best model for a task, keeping cost per 1k tokens below maxCost.
     */
    public LlmModel selectOptimalModel(TaskType taskType, double maxCost) {
        List<ModelSuitability> suitabilities = suitabilityDao.findByTaskType(taskType);
        if (suitabilities.isEmpty()) {
            throw new IllegalStateException("No explicit model suits found for task type: " + taskType);
        }

        List<OutcomeMemory> history = outcomeMemoryDao.findByTaskType(taskType);

        LlmModel bestModel = null;
        double highestScore = -1.0;

        for (ModelSuitability ms : suitabilities) {
            Optional<LlmModel> modelOpt = llmModelDao.read(ms.getModelId());
            if (modelOpt.isEmpty()) continue;
            
            LlmModel model = modelOpt.get();
            if (model.getCostPer1kTokens() > maxCost) {
                continue; // Skip models exceeding budget limit
            }

            double compositeScore = calculateCompositeScore(ms, model.getId(), history);

            if (compositeScore > highestScore) {
                highestScore = compositeScore;
                bestModel = model;
            }
        }

        return bestModel;
    }

    private double calculateCompositeScore(ModelSuitability suitability, Integer modelId, List<OutcomeMemory> history) {
        // Explicit Upfront Model (The Solution to Discrepancy #1)
        double score = suitability.getBaseScore(); 

        // Apply learned intelligence from outcomes
        double totalQuality = 0;
        int count = 0;

        for (OutcomeMemory memory : history) {
            if (memory.getModelId().equals(modelId)) {
                totalQuality += memory.getQualityScore();
                count++;
            }
        }

        if (count > 0) {
            double avgQuality = totalQuality / count;
            // Historical outcomes sway the base score up or down
            // For instance, if base score is 0.8, and avg quality is 0.9, 
            // the composite score climbs towards 0.85
            score = score * 0.5 + avgQuality * 0.5;
        }

        return score;
    }
}
