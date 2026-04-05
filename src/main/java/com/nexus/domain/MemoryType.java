package com.nexus.domain;

public enum MemoryType {
    FACT("Persistent fact, long-lived knowledge", 365),
    PREFERENCE("User preference or style guide", 90),
    EPISODE("Historical event or execution record", 30),
    SKILL("Learned task pattern or prompt template", 180),
    CONTRADICTION("Detected conflict with an existing memory", 7);

    private final String description;
    private final int defaultTtlDays;

    MemoryType(String description, int defaultTtlDays) {
        this.description = description;
        this.defaultTtlDays = defaultTtlDays;
    }

    public String getDescription() { return description; }
    public int getDefaultTtlDays() { return defaultTtlDays; }
}
