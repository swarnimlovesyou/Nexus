package com.nexus.service;

import java.util.ArrayList;
import java.util.List;

import com.nexus.domain.TaskType;

/**
 * Task Decomposition Service.
 * Analyzes complex prompts and splits them into distinct sub-tasks for optimal routing.
 */
public class TaskPlannerService {

    public record PlannedTask(TaskType type, String prompt) {}

    public List<PlannedTask> plan(String prompt) {
        List<PlannedTask> plan = new ArrayList<>();
        String p = prompt.toLowerCase();

        // 1. Heuristic-based decomposition
        boolean wantsCode = p.contains("code") || p.contains("function") || p.contains("implement") || p.contains("script");
        boolean wantsTests = p.contains("test") || p.contains("unit test") || p.contains("verify");
        boolean wantsExplain = p.contains("explain") || p.contains("how it works") || p.contains("what is");
        boolean wantsSummarize = p.contains("summarize") || p.contains("tl;dr") || p.contains("digest");

        if (wantsCode && wantsTests) {
            plan.add(new PlannedTask(TaskType.CODE_GENERATION, "Implement the following: " + prompt));
            plan.add(new PlannedTask(TaskType.UNIT_TESTING, "Write unit tests for the code generated in the previous step, based on: " + prompt));
        } else if (wantsCode) {
            plan.add(new PlannedTask(TaskType.CODE_GENERATION, prompt));
        } else if (wantsExplain) {
            plan.add(new PlannedTask(TaskType.GENERAL_KNOWLEDGE, prompt));
        } else if (wantsSummarize) {
            plan.add(new PlannedTask(TaskType.SUMMARIZATION, prompt));
        } else {
            // Default to reasoning or general knowledge
            plan.add(new PlannedTask(TaskType.REASONING, prompt));
        }

        return plan;
    }
}
