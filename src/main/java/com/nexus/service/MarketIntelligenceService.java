package com.nexus.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.nexus.dao.LlmModelDao;
import com.nexus.domain.LlmModel;
import com.nexus.util.TerminalUtils;

/**
 * Reality Grounding Service.
 * Bridges the local registry with the live global market to sync pricing and availability.
 */
public class MarketIntelligenceService {

    private final LlmModelDao modelDao;
    private final HttpClient httpClient;

    public MarketIntelligenceService() {
        this.modelDao = new LlmModelDao();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    /**
     * Pings the OpenRouter model index to sync latest pricing into the local DB.
     */
    public int syncMarketRates() {
        TerminalUtils.printInfo("Syncing with Global Market Intelligence (OpenRouter)...");
        int updated = 0;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://openrouter.ai/api/v1/models"))
                .GET()
                .build();

            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return 0;

            String body = resp.body();
            List<LlmModel> localModels = modelDao.findAll();

            for (LlmModel local : localModels) {
                // Simplified extraction logic for demo
                double price = extractPriceForModel(body, local.getName());
                if (price > 0 && Math.abs(price - local.getCostPer1kTokens()) > 0.000001) {
                    local.setCostPer1kTokens(price);
                    modelDao.update(local);
                    updated++;
                }
            }
        } catch (IOException | InterruptedException | RuntimeException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            TerminalUtils.printWarn("Market sync partially failed: " + e.getMessage());
        }
        return updated;
    }

    private double extractPriceForModel(String json, String modelName) {
        // Regex to find "id":"modelName" ... "prompt":X
        String pattern = "\"" + Pattern.quote(modelName) + "\".*?\"prompt\"\\s*:\\s*\"([0-9.]+)\"";
        Matcher m = Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            return Double.parseDouble(m.group(1)) * 1000; // Convert to per 1k
        }
        return -1;
    }
}
