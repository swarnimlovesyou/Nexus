package com.nexus.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
    private final HttpClient httpClient;

    public LlmCallService(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
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

        String body = sendJson(provider, endpoint, payload, headers);
        String content = extractFirstString(body, "content");
        int promptTokens = extractFirstInt(body, "prompt_tokens", -1);
        int completionTokens = extractFirstInt(body, "completion_tokens", -1);

        if (content == null || content.isEmpty()) {
            throw new Exception("Provider returned no assistant content.");
        }
        return new ProviderResponse(jsonUnescape(content), promptTokens, completionTokens);
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

        String body = sendJson(Provider.GOOGLE_GEMINI, endpoint, payload, headers);
        String text = extractFirstString(body, "text");
        int inputTokens = extractFirstInt(body, "promptTokenCount", -1);
        int outputTokens = extractFirstInt(body, "candidatesTokenCount", -1);

        if (text == null || text.isEmpty()) {
            throw new Exception("Gemini response did not include text content.");
        }
        return new ProviderResponse(jsonUnescape(text), inputTokens, outputTokens);
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
