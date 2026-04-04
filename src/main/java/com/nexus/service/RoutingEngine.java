package com.nexus.service;

import com.nexus.dao.LlmModelDao;
import com.nexus.dao.OutcomeMemoryDao;
import com.nexus.dao.SuitabilityDao;
import com.nexus.domain.LlmModel;
import com.nexus.domain.ModelSuitability;
import com.nexus.domain.OutcomeMemory;
import com.nexus.domain.TaskType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public LlmModel selectOptimalModel(TaskType taskType) {
        return selectOptimalModel(taskType, Double.MAX_VALUE);
    }

    public LlmModel selectOptimalModel(TaskType taskType, double maxCost) {
        List<ModelSuitability> suitabilities = suitabilityDao.findByTaskType(taskType);
        if (suitabilities.isEmpty()) {
            throw new IllegalStateException("No explicit model suits found for task type: " + taskType);
        }

        // Use HashMap to group history by modelId for O(1) lookup during scoring
        List<OutcomeMemory> history = outcomeMemoryDao.findByTaskType(taskType);
        Map<Integer, List<OutcomeMemory>> historyByModel = new HashMap<>();
        for (OutcomeMemory mem : history) {
            historyByModel.computeIfAbsent(mem.getModelId(), k -> new ArrayList<>()).add(mem);
        }

        LlmModel bestModel = null;
        double highestScore = -1.0;

        for (ModelSuitability ms : suitabilities) {
            Optional<LlmModel> modelOpt = llmModelDao.read(ms.getModelId());
            if (modelOpt.isEmpty()) continue;
            
            LlmModel model = modelOpt.get();
            if (model.getCostPer1kTokens() > maxCost) {
                continue;
            }

            // Pass the pre-grouped history for the specific model
            double compositeScore = calculateCompositeScore(ms, historyByModel.getOrDefault(model.getId(), new ArrayList<>()));

            if (compositeScore > highestScore) {
                highestScore = compositeScore;
                bestModel = model;
            }
        }

        return bestModel;
    }

    private double calculateCompositeScore(ModelSuitability suitability, List<OutcomeMemory> modelHistory) {
        double score = suitability.getBaseScore(); 

        if (!modelHistory.isEmpty()) {
            double totalQuality = 0;
            for (OutcomeMemory memory : modelHistory) {
                totalQuality += memory.getQualityScore();
            }
            double avgQuality = totalQuality / modelHistory.size();
            score = score * 0.5 + avgQuality * 0.5;
        }

        return score;
    }
}
