package com.nexus.domain;

import java.util.Locale;
import java.util.Optional;

public enum Provider {
    OPENAI("OpenAI", "https://api.openai.com/v1"),
    ANTHROPIC("Anthropic", "https://api.anthropic.com"),
    GOOGLE_GEMINI("Google Gemini", "https://generativelanguage.googleapis.com"),
    GROQ("Groq", "https://api.groq.com/openai/v1"),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1"),
    CUSTOM("Custom", "");

    private final String displayName;
    private final String baseUrl;

    Provider(String displayName, String baseUrl) {
        this.displayName = displayName;
        this.baseUrl = baseUrl;
    }

    public String getDisplayName() { return displayName; }
    public String getBaseUrl() { return baseUrl; }

    /**
     * Resolve provider from free-form input (seeded model rows, admin input, enum names).
     * This keeps routing and live calls resilient to alias spelling differences.
     */
    public static Optional<Provider> fromAny(String raw) {
        if (raw == null || raw.trim().isEmpty()) return Optional.empty();

        String n = raw.trim().toLowerCase(Locale.ROOT)
            .replace("_", "")
            .replace("-", "")
            .replace(" ", "");

        if (n.equals("openai")) return Optional.of(OPENAI);
        if (n.equals("anthropic") || n.equals("claude")) return Optional.of(ANTHROPIC);
        if (n.equals("google") || n.equals("googlegemini") || n.equals("gemini")) return Optional.of(GOOGLE_GEMINI);
        if (n.equals("groq")) return Optional.of(GROQ);
        if (n.equals("openrouter")) return Optional.of(OPENROUTER);
        if (n.equals("custom")) return Optional.of(CUSTOM);

        // Fallback: enum name / displayName exact match (case-insensitive)
        for (Provider p : values()) {
            if (p.name().equalsIgnoreCase(raw) || p.displayName.equalsIgnoreCase(raw)) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() { return displayName; }
}
