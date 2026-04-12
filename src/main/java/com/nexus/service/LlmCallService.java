package com.nexus.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.nexus.dao.LlmModelDao;
import com.nexus.dao.UserDao;
import com.nexus.domain.LlmModel;
import com.nexus.domain.Provider;
import com.nexus.domain.User;
import com.nexus.util.TerminalUtils;

/**
 * Service to execute provider calls for routed prompts.
 * Uses real HTTP when possible and falls back to simulation in offline or blocked environments.
 */
public class LlmCallService {

    private final ApiKeyService apiKeyService;
    private final LlmModelDao modelDao;
    private final HttpClient httpClient;

    public LlmCallService(ApiKeyService apiKeyService) {
        this(apiKeyService, null);
    }

    public LlmCallService(ApiKeyService apiKeyService, LlmModelDao modelDao) {
        this.apiKeyService = apiKeyService;
        this.modelDao = modelDao;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    }

    public LlmCallResult executeCall(int userId, LlmModel model, String prompt) throws Exception {
        Provider provider = Provider.fromAny(model.getProvider())
            .orElseThrow(() -> new Exception("Unsupported provider string: " + model.getProvider()));

        Optional<String> keyOpt = apiKeyService.retrieveRawKey(userId, provider);
        if (keyOpt.isEmpty()) {
            throw new Exception("No API key configured for provider: " + provider.getDisplayName() + ". Please configure it in the API Key Vault.");
        }
        String apiKey = keyOpt.get();

        int estimatedInputTokens = estimateTokens(prompt);
        Instant start = Instant.now();

        try {
            ProviderResponse providerResponse = executeRealProviderCall(provider, model, prompt, apiKey);
            long latencyMs = Duration.between(start, Instant.now()).toMillis();

            int inputTokens = providerResponse.inputTokens > 0 ? providerResponse.inputTokens : estimatedInputTokens;
            int outputTokens = providerResponse.outputTokens > 0 ? providerResponse.outputTokens : estimateTokens(providerResponse.content);
            double costUsd = ((inputTokens + outputTokens) / 1000.0) * model.getCostPer1kTokens();

            return new LlmCallResult(
                providerResponse.content,
                latencyMs,
                inputTokens,
                outputTokens,
                costUsd,
                false,
                "Real provider call"
            );
        } catch (Exception realCallError) {
            TerminalUtils.printWarn("Live call unavailable, falling back to simulation: " + realCallError.getMessage());
            return simulateCall(model, provider, estimatedInputTokens);
        }
    }

    private ProviderResponse executeRealProviderCall(Provider provider, LlmModel model, String prompt, String apiKey) throws Exception {
        return switch (provider) {
            case OPENAI, GROQ, OPENROUTER -> executeOpenAiCompatibleCall(provider, model, prompt, apiKey);
            case ANTHROPIC -> executeAnthropicCall(model, prompt, apiKey);
            case GOOGLE_GEMINI -> executeGeminiCall(model, prompt, apiKey);
            case CUSTOM -> throw new Exception("CUSTOM provider calls are not auto-wired. Add endpoint support first.");
        };
    }

    private ProviderResponse executeOpenAiCompatibleCall(Provider provider, LlmModel model, String prompt, String apiKey) throws Exception {
        String endpoint = provider.getBaseUrl() + "/chat/completions";
        String payload = "{" +
            "\"model\":\"" + jsonEscape(model.getName()) + "\"," +
            "\"messages\":[{\"role\":\"user\",\"content\":\"" + jsonEscape(prompt) + "\"}]," +
            "\"temperature\":0.2" +
            "}";

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);
        headers.put("Content-Type", "application/json");

        try {
            String body = sendJson(provider, endpoint, payload, headers);
            return parseOpenAiCompatibleResponse(body);
        } catch (Exception e) {
            // Self-diagnose Groq model mismatch: fetch /models, auto-switch to a working id, and retry once.
            if (provider == Provider.GROQ && isModelNotFound(e)) {
                List<String> available = listOpenAiCompatibleModelIds(provider, apiKey);
                if (!available.isEmpty()) {
                    String chosen = choosePreferredGroqModelId(available);
                    String old = model.getName();
                    if (chosen != null && !chosen.equalsIgnoreCase(old)) {
                        TerminalUtils.printWarn("Groq model not available: " + old);
                        TerminalUtils.printInfo("Available Groq models (first 10): " + String.join(", ", available.subList(0, Math.min(10, available.size()))));
                        TerminalUtils.printInfo("Auto-switching model to: " + chosen);

                        model.setName(chosen);
                        // Persist the fix when we're running inside the app/command runner with DB access.
                        if (modelDao != null && model.getId() != null) {
                            try {
                                modelDao.update(model);
                            } catch (Exception ignored) {
                                // If we can't persist, still retry with the chosen model for this run.
                            }
                        }

                        String retryPayload = "{" +
                            "\"model\":\"" + jsonEscape(model.getName()) + "\"," +
                            "\"messages\":[{\"role\":\"user\",\"content\":\"" + jsonEscape(prompt) + "\"}]," +
                            "\"temperature\":0.2" +
                            "}";
                        String retryBody = sendJson(provider, endpoint, retryPayload, headers);
                        return parseOpenAiCompatibleResponse(retryBody);
                    }
                }

                String hint = "GROQ model_not_found. Try updating your model id. ";
                if (!available.isEmpty()) {
                    hint += "Available models include: " + String.join(", ", available.subList(0, Math.min(10, available.size())));
                }
                throw new Exception(hint + " | root=" + e.getMessage());
            }
            throw e;
        }
    }

    private ProviderResponse parseOpenAiCompatibleResponse(String body) throws Exception {
        String content = extractFirstString(body, "content");
        int promptTokens = extractFirstInt(body, "prompt_tokens", -1);
        int completionTokens = extractFirstInt(body, "completion_tokens", -1);
        if (content == null || content.isEmpty()) {
            throw new Exception("Provider returned no assistant content.");
        }
        return new ProviderResponse(jsonUnescape(content), promptTokens, completionTokens);
    }

    private boolean isModelNotFound(Exception e) {
        if (e == null || e.getMessage() == null) return false;
        String msg = e.getMessage().toLowerCase(Locale.ROOT);
        return msg.contains("model_not_found") || msg.contains("does not exist") || msg.contains("invalid_request_error");
    }

    private List<String> listOpenAiCompatibleModelIds(Provider provider, String apiKey) {
        try {
            String endpoint = provider.getBaseUrl() + "/models";
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + apiKey);
            headers.put("Content-Type", "application/json");
            String body = sendGetJson(endpoint, headers);
            return parseModelIdsFromListResponse(body);
        } catch (Exception e) {
            return List.of();
        }
    }

    private String choosePreferredGroqModelId(List<String> ids) {
        if (ids == null || ids.isEmpty()) return null;
        // Prefer the commonly-available fast model first.
        for (String preferred : List.of("llama-3.1-8b-instant", "llama-3.3-70b-versatile")) {
            for (String id : ids) {
                if (id != null && id.equalsIgnoreCase(preferred)) return id;
            }
        }
        // Otherwise pick the first llama model.
        for (String id : ids) {
            if (id != null && id.toLowerCase(Locale.ROOT).contains("llama")) return id;
        }
        return ids.get(0);
    }

    private List<String> parseModelIdsFromListResponse(String json) {
        if (json == null || json.isBlank()) return List.of();
        // Very small JSON parser: collect unique values of "id":"...".
        LinkedHashSet<String> out = new LinkedHashSet<>();
        String token = "\"id\"";
        int idx = 0;
        while (idx < json.length()) {
            int p = json.indexOf(token, idx);
            if (p < 0) break;
            int colon = json.indexOf(':', p + token.length());
            if (colon < 0) break;
            int q1 = json.indexOf('"', colon + 1);
            if (q1 < 0) break;
            int q2 = json.indexOf('"', q1 + 1);
            if (q2 < 0) break;
            String id = json.substring(q1 + 1, q2);
            if (!id.isBlank()) out.add(id);
            idx = q2 + 1;
        }
        return new ArrayList<>(out);
    }

    private String sendGetJson(String endpoint, Map<String, String> headers) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .timeout(Duration.ofSeconds(30))
            .GET();

        for (Map.Entry<String, String> header : headers.entrySet()) {
            requestBuilder.header(header.getKey(), header.getValue());
        }

        HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        int code = response.statusCode();
        if (code < 200 || code >= 300) {
            throw new Exception("HTTP " + code + ": " + trimBody(response.body()));
        }
        return response.body();
    }

    private ProviderResponse executeAnthropicCall(LlmModel model, String prompt, String apiKey) throws Exception {
        String endpoint = Provider.ANTHROPIC.getBaseUrl() + "/v1/messages";
        String payload = "{" +
            "\"model\":\"" + jsonEscape(model.getName()) + "\"," +
            "\"max_tokens\":512," +
            "\"messages\":[{\"role\":\"user\",\"content\":\"" + jsonEscape(prompt) + "\"}]" +
            "}";

        Map<String, String> headers = new HashMap<>();
        headers.put("x-api-key", apiKey);
        headers.put("anthropic-version", "2023-06-01");
        headers.put("Content-Type", "application/json");

        String body = sendJson(Provider.ANTHROPIC, endpoint, payload, headers);
        String text = extractFirstString(body, "text");
        int inputTokens = extractFirstInt(body, "input_tokens", -1);
        int outputTokens = extractFirstInt(body, "output_tokens", -1);

        if (text == null || text.isEmpty()) {
            throw new Exception("Anthropic response did not include text content.");
        }
        return new ProviderResponse(jsonUnescape(text), inputTokens, outputTokens);
    }

    private ProviderResponse executeGeminiCall(LlmModel model, String prompt, String apiKey) throws Exception {
        String endpoint = Provider.GOOGLE_GEMINI.getBaseUrl()
            + "/v1beta/models/"
            + URLEncoder.encode(model.getName(), StandardCharsets.UTF_8)
            + ":generateContent?key="
            + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

        String payload = "{" +
            "\"contents\":[{\"parts\":[{\"text\":\"" + jsonEscape(prompt) + "\"}]}]" +
            "}";

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        try {
            String body = sendJson(Provider.GOOGLE_GEMINI, endpoint, payload, headers);
            return parseGeminiGenerateContentResponse(body);
        } catch (Exception e) {
            if (isGeminiModelNotFound(e)) {
                List<String> available = listGeminiModelIds(apiKey);
                String replacement = choosePreferredGeminiModelId(available);
                if (replacement != null && !replacement.equalsIgnoreCase(model.getName())) {
                    TerminalUtils.printInfo("Gemini model not available: " + model.getName());
                    if (!available.isEmpty()) {
                        TerminalUtils.printInfo("Available Gemini models (first 10): " + String.join(", ", available.subList(0, Math.min(10, available.size()))));
                    }
                    TerminalUtils.printInfo("Auto-switching model to: " + replacement);

                    model.setName(replacement);
                    if (modelDao != null && model.getId() != null) {
                        try {
                            modelDao.update(model);
                        } catch (Exception ignored) {
                            // Best-effort only; the retry can still succeed even if persistence fails.
                        }
                    }

                    String retryEndpoint = Provider.GOOGLE_GEMINI.getBaseUrl()
                        + "/v1beta/models/"
                        + URLEncoder.encode(model.getName(), StandardCharsets.UTF_8)
                        + ":generateContent?key="
                        + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
                    String retryBody = sendJson(Provider.GOOGLE_GEMINI, retryEndpoint, payload, headers);
                    return parseGeminiGenerateContentResponse(retryBody);
                }

                String hint = "Gemini model not available or not supported for generateContent. Try updating your model id.";
                if (!available.isEmpty()) {
                    hint += " Available models include: " + String.join(", ", available.subList(0, Math.min(10, available.size())));
                }
                throw new Exception(hint + " | root=" + e.getMessage());
            }
            throw e;
        }
    }

    private ProviderResponse parseGeminiGenerateContentResponse(String body) throws Exception {
        String text = extractFirstString(body, "text");
        int inputTokens = extractFirstInt(body, "promptTokenCount", -1);
        int outputTokens = extractFirstInt(body, "candidatesTokenCount", -1);
        if (text == null || text.isEmpty()) {
            throw new Exception("Gemini response did not include text content.");
        }
        return new ProviderResponse(jsonUnescape(text), inputTokens, outputTokens);
    }

    private boolean isGeminiModelNotFound(Exception e) {
        if (e == null || e.getMessage() == null) return false;
        String msg = e.getMessage().toLowerCase(Locale.ROOT);
        // Common Gemini errors:
        // - "models/<id> is not found for API version v1beta"
        // - "... is not supported for generateContent"
        return msg.contains("http 404")
            || (msg.contains("models/") && msg.contains("not found"))
            || msg.contains("not supported for generatecontent")
            || (msg.contains("api version") && msg.contains("is not found"));
    }

    private List<String> listGeminiModelIds(String apiKey) {
        try {
            String endpoint = Provider.GOOGLE_GEMINI.getBaseUrl()
                + "/v1beta/models?key="
                + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            String body = sendGetJson(endpoint, headers);
            return parseGeminiModelIdsFromListResponse(body);
        } catch (Exception e) {
            return List.of();
        }
    }

    private String choosePreferredGeminiModelId(List<String> ids) {
        if (ids == null || ids.isEmpty()) return null;
        for (String preferred : List.of(
            "gemini-1.5-pro-latest",
            "gemini-1.5-pro",
            "gemini-2.0-flash",
            "gemini-1.5-flash-latest",
            "gemini-1.5-flash"
        )) {
            for (String id : ids) {
                if (id != null && id.equalsIgnoreCase(preferred)) return id;
            }
        }
        for (String id : ids) {
            if (id != null && id.toLowerCase(Locale.ROOT).contains("gemini")) return id;
        }
        return ids.get(0);
    }

    private List<String> parseGeminiModelIdsFromListResponse(String json) {
        if (json == null || json.isBlank()) return List.of();
        // Gemini ListModels returns entries like: {"name":"models/gemini-1.5-pro-latest", ...}
        LinkedHashSet<String> out = new LinkedHashSet<>();
        String token = "\"name\"";
        int idx = 0;
        while (idx < json.length()) {
            int p = json.indexOf(token, idx);
            if (p < 0) break;
            int colon = json.indexOf(':', p + token.length());
            if (colon < 0) break;
            int q1 = json.indexOf('"', colon + 1);
            if (q1 < 0) break;
            int q2 = json.indexOf('"', q1 + 1);
            if (q2 < 0) break;
            String name = json.substring(q1 + 1, q2);
            if (name.startsWith("models/")) {
                out.add(name.substring("models/".length()));
            }
            idx = q2 + 1;
        }
        return new ArrayList<>(out);
    }

    private String sendJson(Provider provider, String endpoint, String payload, Map<String, String> headers) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .timeout(Duration.ofSeconds(45))
            .POST(HttpRequest.BodyPublishers.ofString(payload));

        for (Map.Entry<String, String> header : headers.entrySet()) {
            requestBuilder.header(header.getKey(), header.getValue());
        }

        HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        int code = response.statusCode();
        if (code < 200 || code >= 300) {
            String errorMsg = trimBody(response.body());
            if (code == 429) {
                throw new Exception("RATE_LIMIT: " + provider.getDisplayName() + " quota exceeded. Switching to fallback...");
            }
            if (code == 400 && errorMsg.contains("context_length")) {
                throw new Exception("CONTEXT_LIMIT: Prompt too large for " + endpoint);
            }
            throw new Exception("HTTP " + code + ": " + errorMsg);
        }
        return response.body();
    }

    private LlmCallResult simulateCall(LlmModel model, Provider provider, int estimatedInputTokens) throws InterruptedException {
        Instant start = Instant.now();
        Thread.sleep(500 + estimatedInputTokens);

        String responseText = "Simulated response using " + model.getName() + ".\n"
            + "To force a real call, ensure outbound network access and a valid API key for " + provider.getDisplayName() + ".";

        int estimatedOutputTokens = estimateTokens(responseText);
        double costUsd = ((estimatedInputTokens + estimatedOutputTokens) / 1000.0) * model.getCostPer1kTokens();
        long latencyMs = Duration.between(start, Instant.now()).toMillis();

        return new LlmCallResult(responseText, latencyMs, estimatedInputTokens, estimatedOutputTokens, costUsd, true, "Simulation fallback");
    }

    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 1;
        return Math.max(1, text.length() / 4);
    }

    private String jsonEscape(String raw) {
        return raw
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }

    private String jsonUnescape(String raw) {
        return raw
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\");
    }

    private String extractFirstString(String json, String key) {
        if (json == null || json.isEmpty() || key == null || key.isEmpty()) {
            return null;
        }

        String token = "\"" + key + "\"";
        int start = 0;
        while (true) {
            int keyPos = json.indexOf(token, start);
            if (keyPos < 0) return null;

            int colonPos = json.indexOf(':', keyPos + token.length());
            if (colonPos < 0) return null;

            int valueStart = colonPos + 1;
            while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
                valueStart++;
            }

            if (valueStart >= json.length() || json.charAt(valueStart) != '"') {
                start = keyPos + token.length();
                continue;
            }

            StringBuilder raw = new StringBuilder();
            boolean escaped = false;
            for (int i = valueStart + 1; i < json.length(); i++) {
                char c = json.charAt(i);
                if (escaped) {
                    raw.append('\\').append(c);
                    escaped = false;
                    continue;
                }
                if (c == '\\') {
                    escaped = true;
                    continue;
                }
                if (c == '"') {
                    return raw.toString();
                }
                raw.append(c);
            }
            return raw.toString();
        }
    }

    private int extractFirstInt(String json, String key, int fallback) {
        if (json == null || json.isEmpty() || key == null || key.isEmpty()) {
            return fallback;
        }

        String token = "\"" + key + "\"";
        int start = 0;
        while (true) {
            int keyPos = json.indexOf(token, start);
            if (keyPos < 0) return fallback;

            int colonPos = json.indexOf(':', keyPos + token.length());
            if (colonPos < 0) return fallback;

            int valueStart = colonPos + 1;
            while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
                valueStart++;
            }

            int valueEnd = valueStart;
            if (valueEnd < json.length() && (json.charAt(valueEnd) == '-' || Character.isDigit(json.charAt(valueEnd)))) {
                valueEnd++;
                while (valueEnd < json.length() && Character.isDigit(json.charAt(valueEnd))) {
                    valueEnd++;
                }
                try {
                    return Integer.parseInt(json.substring(valueStart, valueEnd));
                } catch (NumberFormatException ignored) {
                    return fallback;
                }
            }

            start = keyPos + token.length();
        }
    }

    private String trimBody(String body) {
        if (body == null) return "";
        return body.length() > 220 ? body.substring(0, 220) + "..." : body;
    }

    private record ProviderResponse(String content, int inputTokens, int outputTokens) {}

    public record LlmCallResult(String content, long latencyMs, int inputTokens, int outputTokens,
                                double costUsd, boolean simulated, String mode) {}

    public record HealthReport(boolean reachable, long latencyMs, String status, String provider) {}

    public HealthReport checkHealth(int userId, LlmModel model) {
        Instant start = Instant.now();
        int effectiveUserId = userId;
        
        // For admin health checks (-1), look up the admin user ID
        if (userId == -1) {
            UserDao userDao = new UserDao();
            Optional<User> adminOpt = userDao.findByUsername("admin");
            if (adminOpt.isPresent()) {
                effectiveUserId = adminOpt.get().getId();
            } else {
                return new HealthReport(false, 0, "No admin user found", model.getProvider());
            }
        }
        
        try {
            Provider provider = Provider.fromAny(model.getProvider()).orElse(Provider.CUSTOM);
            Optional<String> keyOpt = apiKeyService.retrieveRawKey(effectiveUserId, provider);
            
            if (keyOpt.isEmpty()) {
                return new HealthReport(false, 0, "No API key configured", model.getProvider());
            }
            
            executeCall(effectiveUserId, model, "Ping");
            long latency = Duration.between(start, Instant.now()).toMillis();
            return new HealthReport(true, latency, "Healthy", model.getProvider());
        } catch (Exception e) {
            long latency = Duration.between(start, Instant.now()).toMillis();
            return new HealthReport(false, latency, e.getMessage(), model.getProvider());
        }
    }
}
