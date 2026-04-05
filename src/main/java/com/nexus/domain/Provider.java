package com.nexus.domain;

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

    @Override
    public String toString() { return displayName; }
}
