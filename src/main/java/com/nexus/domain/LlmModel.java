package com.nexus.domain;

import java.time.LocalDateTime;

public class LlmModel extends BaseEntity {
    private String name;
    private String provider;
    private double costPer1kTokens;

    public LlmModel() {
        super();
    }

    public LlmModel(Integer id, String name, String provider, double costPer1kTokens, LocalDateTime createdAt) {
        super(id, createdAt);
        this.name = name;
        this.provider = provider;
        this.costPer1kTokens = costPer1kTokens;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public double getCostPer1kTokens() { return costPer1kTokens; }
    public void setCostPer1kTokens(double costPer1kTokens) { this.costPer1kTokens = costPer1kTokens; }

    @Override
    public String getEntityDisplayName() {
        return "Model: " + provider + "/" + name;
    }

    @Override
    public String toString() {
        return "LlmModel{" +
               "id=" + getId() +
               ", name='" + name + '\'' +
               ", provider='" + provider + '\'' +
               ", costPer1kTokens=$" + costPer1kTokens +
               '}';
    }
}
