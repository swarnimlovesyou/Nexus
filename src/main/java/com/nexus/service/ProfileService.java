package com.nexus.service;

import com.nexus.dao.AuditLogDao;
import com.nexus.dao.UserProfileDao;
import com.nexus.domain.AuditLog;
import com.nexus.exception.ValidationException;

import java.util.Map;
import java.util.regex.Pattern;

public class ProfileService {
    public static final String GLOBAL_SCOPE = UserProfileDao.GLOBAL_SCOPE;

    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]{2,64}$");
    private static final int MAX_VALUE_LENGTH = 600;

    private final UserProfileDao userProfileDao;
    private final AuditLogDao auditLogDao;

    public ProfileService() {
        this.userProfileDao = new UserProfileDao();
        this.auditLogDao = new AuditLogDao();
    }

    public void setSetting(int userId, String scope, String key, String value) {
        String normalizedScope = normalizeScope(scope);
        String normalizedKey = normalizeKey(key);
        String normalizedValue = normalizeValue(value);

        userProfileDao.upsert(userId, normalizedScope, normalizedKey, normalizedValue);
        auditLogDao.create(new AuditLog(null, userId, "PROFILE_SET",
            "scope=" + normalizedScope + " key=" + normalizedKey, "SUCCESS", null));
    }

    public Map<String, String> listSettings(int userId, String scope, boolean mergedWithGlobal) {
        String normalizedScope = normalizeScope(scope);
        return mergedWithGlobal
            ? userProfileDao.findMergedByUserAndScope(userId, normalizedScope)
            : userProfileDao.findByUserAndScope(userId, normalizedScope);
    }

    public boolean deleteSetting(int userId, String scope, String key) {
        String normalizedScope = normalizeScope(scope);
        String normalizedKey = normalizeKey(key);
        boolean deleted = userProfileDao.deleteByUserScopeAndKey(userId, normalizedScope, normalizedKey);
        if (deleted) {
            auditLogDao.create(new AuditLog(null, userId, "PROFILE_DELETE",
                "scope=" + normalizedScope + " key=" + normalizedKey, "SUCCESS", null));
        }
        return deleted;
    }

    public String buildContextBlock(int userId, String scope) {
        Map<String, String> settings = listSettings(userId, scope, true);
        if (settings.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("Persistent profile settings (always follow unless unsafe):\n");
        settings.forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
        return sb.toString();
    }

    public boolean isActionAllowed(int userId, String scope, String policyKey) {
        Map<String, String> settings = listSettings(userId, scope, true);
        String value = settings.get(policyKey);
        if (value == null || value.isBlank()) {
            // Default allow to preserve backward compatibility for existing CLI users.
            return true;
        }
        return parseBoolean(value);
    }

    public boolean getBooleanSetting(int userId, String scope, String key, boolean fallback) {
        Map<String, String> settings = listSettings(userId, scope, true);
        String value = settings.get(key);
        if (value == null || value.isBlank()) return fallback;
        return parseBoolean(value);
    }

    public int getIntSetting(int userId, String scope, String key, int fallback) {
        Map<String, String> settings = listSettings(userId, scope, true);
        String value = settings.get(key);
        if (value == null || value.isBlank()) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public String currentWorkspaceScope() {
        return normalizeScope(System.getProperty("user.dir"));
    }

    private String normalizeScope(String scope) {
        if (scope == null || scope.trim().isEmpty()) return GLOBAL_SCOPE;
        return scope.trim().toLowerCase();
    }

    private String normalizeKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new ValidationException("Profile key cannot be empty.");
        }
        String normalized = key.trim().toLowerCase();
        if (!KEY_PATTERN.matcher(normalized).matches()) {
            throw new ValidationException("Profile key must match [a-zA-Z0-9_.-] and be 2-64 chars.");
        }
        return normalized;
    }

    private String normalizeValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("Profile value cannot be empty.");
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_VALUE_LENGTH) {
            throw new ValidationException("Profile value is too long (max " + MAX_VALUE_LENGTH + " chars).");
        }
        return normalized;
    }

    private boolean parseBoolean(String value) {
        String normalized = value.trim().toLowerCase();
        return normalized.equals("true")
            || normalized.equals("yes")
            || normalized.equals("1")
            || normalized.equals("on")
            || normalized.equals("allow");
    }
}
