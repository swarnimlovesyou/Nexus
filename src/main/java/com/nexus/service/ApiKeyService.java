package com.nexus.service;

import com.nexus.dao.ApiKeyDao;
import com.nexus.dao.AuditLogDao;
import com.nexus.domain.ApiKey;
import com.nexus.domain.AuditLog;
import com.nexus.domain.Provider;
import com.nexus.exception.ValidationException;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

public class ApiKeyService {
    private final ApiKeyDao apiKeyDao;
    private final AuditLogDao auditLogDao;
    // Simple XOR key for local obfuscation (not cryptographic — labeled as such in UI)
    private static final byte XOR_KEY = 0x4E; // 'N' for Nexus

    public ApiKeyService() {
        this.apiKeyDao = new ApiKeyDao();
        this.auditLogDao = new AuditLogDao();
    }

    /**
     * Store a new API key for a user.
     * Stores it XOR-encoded and masked for display.
     */
    public ApiKey storeKey(int userId, Provider provider, String alias, String rawKey) {
        if (rawKey == null || rawKey.trim().isEmpty()) {
            throw new ValidationException("API key cannot be empty.");
        }
        if (alias == null || alias.trim().isEmpty()) {
            throw new ValidationException("Alias cannot be empty.");
        }

        String maskedKey = maskKey(rawKey);
        String encodedKey = xorEncode(rawKey);

        ApiKey key = new ApiKey(null, userId, provider, alias.trim(), maskedKey, encodedKey, null);
        apiKeyDao.create(key);

        auditLogDao.create(new AuditLog(null, userId, "API_KEY_ADD",
            "Provider=" + provider.getDisplayName() + " alias=" + alias, "SUCCESS", null));

        return key;
    }

    /**
     * Retrieve (decode) the raw API key for a provider.
     */
    public Optional<String> retrieveRawKey(int userId, Provider provider) {
        Optional<ApiKey> keyOpt = apiKeyDao.findByUserAndProvider(userId, provider);
        return keyOpt.map(k -> xorDecode(k.getEncodedKey()));
    }

    public List<ApiKey> listKeysForUser(int userId) {
        return apiKeyDao.findByUserId(userId);
    }

    public void deleteKey(int userId, int keyId) {
        ApiKey key = apiKeyDao.read(keyId)
            .orElseThrow(() -> new ValidationException("API key not found."));

        if (key.getUserId() != userId) {
            auditLogDao.create(new AuditLog(null, userId, "API_KEY_DELETE",
                "Attempted delete of keyId=" + keyId + " ownedBy=" + key.getUserId(), "FAILURE", null));
            throw new ValidationException("Access denied: this API key belongs to another user.");
        }

        auditLogDao.create(new AuditLog(null, userId, "API_KEY_DELETE",
            "Provider=" + key.getProvider().getDisplayName() + " alias=" + key.getAlias(), "SUCCESS", null));
        apiKeyDao.delete(keyId);
    }

    public boolean hasKeyForProvider(int userId, Provider provider) {
        return apiKeyDao.findByUserAndProvider(userId, provider).isPresent();
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    private String maskKey(String rawKey) {
        if (rawKey.length() <= 8) return "****";
        String prefix = rawKey.substring(0, Math.min(6, rawKey.length()));
        String suffix = rawKey.substring(rawKey.length() - 4);
        return prefix + "..." + suffix;
    }

    private String xorEncode(String raw) {
        byte[] bytes = raw.getBytes();
        for (int i = 0; i < bytes.length; i++) bytes[i] ^= XOR_KEY;
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String xorDecode(String encoded) {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        for (int i = 0; i < bytes.length; i++) bytes[i] ^= XOR_KEY;
        return new String(bytes);
    }
}
