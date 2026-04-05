package com.nexus.domain;

import java.time.LocalDateTime;

public class ApiKey extends BaseEntity {
    private Integer userId;
    private Provider provider;
    private String alias;
    private String maskedKey;
    private String encodedKey;

    public ApiKey() { super(); }

    public ApiKey(Integer id, Integer userId, Provider provider, String alias,
                  String maskedKey, String encodedKey, LocalDateTime createdAt) {
        super(id, createdAt);
        this.userId = userId;
        this.provider = provider;
        this.alias = alias;
        this.maskedKey = maskedKey;
        this.encodedKey = encodedKey;
    }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Provider getProvider() { return provider; }
    public void setProvider(Provider provider) { this.provider = provider; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public String getMaskedKey() { return maskedKey; }
    public void setMaskedKey(String maskedKey) { this.maskedKey = maskedKey; }
    public String getEncodedKey() { return encodedKey; }
    public void setEncodedKey(String encodedKey) { this.encodedKey = encodedKey; }

    @Override
    public String getEntityDisplayName() {
        return "[" + provider.getDisplayName() + "] " + alias + " → " + maskedKey;
    }

    @Override
    public String toString() {
        return "ApiKey{id=" + getId() + ", provider=" + provider + ", alias='" + alias + "', key=" + maskedKey + "}";
    }
}
