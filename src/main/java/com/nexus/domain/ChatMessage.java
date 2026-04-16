package com.nexus.domain;

/**
 * A single message in a multi-turn LLM conversation.
 * Role is one of: "system", "user", "assistant" (OpenAI/Groq convention).
 */
public record ChatMessage(String role, String content) {

    public static ChatMessage system(String content)    { return new ChatMessage("system",    content); }
    public static ChatMessage user(String content)      { return new ChatMessage("user",      content); }
    public static ChatMessage assistant(String content)  { return new ChatMessage("assistant", content); }

    public boolean isSystem()    { return "system".equals(role); }
    public boolean isUser()      { return "user".equals(role); }
    public boolean isAssistant() { return "assistant".equals(role); }
}
