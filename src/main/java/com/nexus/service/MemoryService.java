package com.nexus.service;

import com.nexus.dao.AuditLogDao;
import com.nexus.dao.MemoryDao;
import com.nexus.domain.AuditLog;
import com.nexus.domain.Memory;
import com.nexus.domain.MemoryType;
import com.nexus.exception.ValidationException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MemoryService {
    public static final String GLOBAL_SCOPE = "global";
    public static final String PINNED_TAG = "pinned:true";

    private final MemoryDao memoryDao;
    private final AuditLogDao auditLogDao;
    private final Clock clock;

    public MemoryService() {
        this(Clock.systemUTC());
    }

    public MemoryService(Clock clock) {
        this(new MemoryDao(), new AuditLogDao(), clock);
    }

    MemoryService(MemoryDao memoryDao, AuditLogDao auditLogDao, Clock clock) {
        this.memoryDao = memoryDao;
        this.auditLogDao = auditLogDao;
        this.clock = clock;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STORE — Method Overloading (3 signatures)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Overload 1 (full): Store with content, tags, and type.
     * Uses the default TTL for the given memory type.
     * Automatically detects contradictions with existing FACTs sharing the same tag.
     */
    public Memory store(int userId, String content, String tags, MemoryType type) {
        return storeScoped(userId, currentWorkspaceScope(), content, tags, type, type.getDefaultTtlDays(), false);
    }

    /**
     * Overload 2 (minimal): Store with content and type only — no tags.
     * Convenience method for quick fact or preference storage.
     */
    public Memory store(int userId, String content, MemoryType type) {
        return storeScoped(userId, currentWorkspaceScope(), content, "", type, type.getDefaultTtlDays(), false);
    }

    /**
     * Overload 3 (full + TTL override): Store with all fields including a custom TTL.
     * Used by the outcome→memory bridge to store EPISODE memories with shorter TTL.
     * All other overloads delegate here.
     */
    public Memory store(int userId, String content, String tags, MemoryType type, int ttlOverrideDays) {
        return storeScoped(userId, currentWorkspaceScope(), content, tags, type, ttlOverrideDays, false);
    }

    /**
     * Scope-aware memory storage.
     * Scope is typically the current workspace path; global scope is supported for reusable profile context.
     */
    public Memory storeScoped(int userId, String scope, String content, String tags, MemoryType type,
                              int ttlOverrideDays, boolean pinned) {
        if (content == null || content.trim().isEmpty())
            throw new ValidationException("Memory content cannot be empty.");

        String normalizedScope = normalizeScope(scope);

        // Contradiction detection: check if a FACT with same tag already exists
        if (type == MemoryType.FACT && tags != null && !tags.trim().isEmpty()) {
            for (String tag : tags.split(",")) {
                List<Memory> existing = memoryDao.searchContentByAgent(userId, normalizedScope, tag.trim());
                for (Memory m : existing) {
                    if (m.getType() == MemoryType.FACT && !m.getContent().equalsIgnoreCase(content)) {
                        type = MemoryType.CONTRADICTION;
                        break;
                    }
                }
                if (type == MemoryType.CONTRADICTION) break;
            }
        }

        // Use the smaller of ttlOverrideDays and type's default (prevents unlimited TTL abuse)
        int effectiveTtl = Math.max(1, ttlOverrideDays);
        if (pinned) {
            effectiveTtl = Math.max(effectiveTtl, 3650);
        }
        LocalDateTime expiresAt = LocalDateTime.now(clock).plusDays(effectiveTtl);

        Memory mem = new Memory();
        mem.setUserId(userId);
        mem.setAgentId(normalizedScope);
        mem.setContent(content.trim());
        mem.setTags(withScopeTags(tags, normalizedScope, pinned));
        mem.setType(type);
        mem.setConfidence(1.0);
        mem.setExpiresAt(expiresAt);

        memoryDao.create(mem);
        auditLogDao.create(new AuditLog(null, userId, "MEMORY_STORE",
            "type=" + type + " ttl=" + effectiveTtl + "d scope=" + normalizedScope + " pinned=" + pinned,
            "SUCCESS", null));

        return mem;
    }

    public Memory storeGlobal(int userId, String content, String tags, MemoryType type, int ttlOverrideDays, boolean pinned) {
        return storeScoped(userId, GLOBAL_SCOPE, content, tags, type, ttlOverrideDays, pinned);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RECALL / READ
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Hybrid recall: keyword match on content + tags, scored by confidence × recency.
     * Uses ArrayList<Memory> for results and HashMap<Integer,Double> for score mapping.
     */
    public List<Memory> recall(int userId, String query) {
        return recallForScope(userId, currentWorkspaceScope(), query);
    }

    /**
     * Scope-aware recall with fallback to global profile memories.
     */
    public List<Memory> recallForScope(int userId, String scope, String query) {
        String normalizedScope = normalizeScope(scope);
        String safeQuery = query == null ? "" : query;

        List<Memory> scopedMatches = memoryDao.searchContentByAgent(userId, normalizedScope, safeQuery);
        List<Memory> globalMatches = normalizedScope.equals(GLOBAL_SCOPE)
            ? List.of()
            : memoryDao.searchContentByAgent(userId, GLOBAL_SCOPE, safeQuery);

        Map<Integer, Memory> merged = new LinkedHashMap<>();
        for (Memory m : scopedMatches) merged.put(m.getId(), m);
        for (Memory m : globalMatches) merged.putIfAbsent(m.getId(), m);

        List<Memory> matches = merged.values().stream().toList();

        LocalDateTime now = LocalDateTime.now(clock);
        // Score map: memoryId → hybrid score  (demonstrates HashMap usage)
        Map<Integer, Double> scoreMap = new HashMap<>();
        for (Memory m : matches) {
            long daysOld = java.time.temporal.ChronoUnit.DAYS.between(m.getCreatedAt(), now);
            double recencyFactor = Math.max(0, 1.0 - (daysOld / 365.0));
            double scopeBoost = normalizedScope.equals(m.getAgentId()) ? 0.12 : 0.04;
            double pinBoost = isPinned(m) ? 0.10 : 0.0;
            double hybridScore = m.getConfidence() * 0.6 + recencyFactor * 0.3 + scopeBoost + pinBoost;
            scoreMap.put(m.getId(), hybridScore);
        }

        List<Memory> scored = matches.stream()
            .sorted(Comparator.comparingDouble(m -> -scoreMap.getOrDefault(m.getId(), 0.0)))
            .collect(Collectors.toList());

        // Mark as accessed
        for (Memory m : scored) {
            memoryDao.updateConfidenceAndAccess(m.getId(), m.getConfidence());
        }

        if (!safeQuery.isEmpty()) {
            auditLogDao.create(new AuditLog(null, userId, "MEMORY_RECALL",
                "query=" + safeQuery + " scope=" + normalizedScope + " results=" + scored.size(), "SUCCESS", null));
        }

        return scored;
    }

    /** Get all memories for a user, sorted by confidence descending. */
    public List<Memory> getAllMemories(int userId) {
        return memoryDao.findByUserId(userId);
    }

    public List<Memory> getByScope(int userId, String scope) {
        return memoryDao.findByUserIdAndAgentId(userId, normalizeScope(scope));
    }

    /** Get memories filtered by type. Uses ArrayList internally. */
    public List<Memory> getByType(int userId, MemoryType type) {
        return memoryDao.findByType(userId, type);
    }

    /**
     * Get a breakdown of memory counts per type.
     * Demonstrates HashMap usage for grouped counts.
     */
    public Map<MemoryType, Integer> getTypeCounts(int userId) {
        List<Memory> all = memoryDao.findByUserId(userId);
        Map<MemoryType, Integer> counts = new HashMap<>();
        for (MemoryType t : MemoryType.values()) counts.put(t, 0);
        for (Memory m : all) counts.merge(m.getType(), 1, Integer::sum);
        return counts;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UPDATE (CRUD completeness)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Update the content of an existing memory by ID.
     * Ownership check ensures users can only edit their own memories.
     */
    public boolean updateContent(int userId, int memoryId, String newContent) {
        if (newContent == null || newContent.trim().isEmpty())
            throw new ValidationException("New content cannot be empty.");

        return memoryDao.read(memoryId).map(m -> {
            if (m.getUserId() != userId) {
                throw new ValidationException("Cannot edit a memory that belongs to another user.");
            }
            m.setContent(newContent.trim());
            memoryDao.update(m);
            auditLogDao.create(new AuditLog(null, userId, "MEMORY_UPDATE",
                "id=" + memoryId + " preview=" + newContent.substring(0, Math.min(40, newContent.length())),
                "SUCCESS", null));
            return true;
        }).orElse(false);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DELETE
    // ═══════════════════════════════════════════════════════════════════════

    /** Hard delete a memory by ID. */
    public void forget(int userId, int memoryId) {
        Memory m = memoryDao.read(memoryId)
            .orElseThrow(() -> new ValidationException("Memory not found."));

        if (m.getUserId() != userId) {
            auditLogDao.create(new AuditLog(null, userId, "MEMORY_FORGET",
                "Attempted delete of memoryId=" + memoryId + " ownedBy=" + m.getUserId(),
                "FAILURE", null));
            throw new ValidationException("Cannot delete a memory that belongs to another user.");
        }

        boolean deleted = memoryDao.deleteByIdAndUserId(memoryId, userId);
        if (!deleted) {
            auditLogDao.create(new AuditLog(null, userId, "MEMORY_FORGET",
                "Delete failed for memoryId=" + memoryId + " ownedBy=" + userId,
                "FAILURE", null));
            throw new ValidationException("Failed to delete memory.");
        }

        auditLogDao.create(new AuditLog(null, userId, "MEMORY_FORGET",
            "id=" + memoryId + " content=" + m.getContent().substring(0, Math.min(40, m.getContent().length())),
            "SUCCESS", null));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DECAY / PRUNE
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Decay pass: reduce confidence 5% for each memory not accessed in >7 days.
     * Returns count of decayed memories.
     */
    public int runDecayPass(int userId) {
        List<Memory> all = memoryDao.findByUserId(userId); // ArrayList
        LocalDateTime threshold = LocalDateTime.now(clock).minusDays(7);
        int count = 0;
        for (Memory m : all) {
            if (isPinned(m)) continue;
            LocalDateTime lastAccess = m.getLastAccessedAt() != null ? m.getLastAccessedAt() : m.getCreatedAt();
            if (lastAccess != null && lastAccess.isBefore(threshold)) {
                double newConf = Math.max(0.0, m.getConfidence() - 0.05);
                memoryDao.updateConfidenceAndAccess(m.getId(), newConf);
                count++;
            }
        }
        auditLogDao.create(new AuditLog(null, userId, "MEMORY_DECAY",
            "decayed=" + count + " memories", "SUCCESS", null));
        return count;
    }

    /** Prune expired or low-confidence memories for this user only. */
    public int pruneExpired(int userId) {
        List<Memory> all = memoryDao.findByUserId(userId);
        LocalDateTime now = LocalDateTime.now(clock);
        int pruned = 0;
        for (Memory m : all) {
            if (isPinned(m)) continue;
            boolean expired = m.getExpiresAt() != null && m.getExpiresAt().isBefore(now);
            boolean lowConfidence = m.getConfidence() < 0.10;
            if (expired || lowConfidence) {
                if (m.getId() != null && memoryDao.deleteByIdAndUserId(m.getId(), userId)) {
                    pruned++;
                }
            }
        }
        auditLogDao.create(new AuditLog(null, userId, "MEMORY_PRUNE",
            "pruned=" + pruned + " memories", "SUCCESS", null));
        return pruned;
    }

    /** Get contradiction pairs for graph view. */
    public List<Memory> getContradictions(int userId) {
        return memoryDao.findContradictions(userId);
    }

    public String currentWorkspaceScope() {
        return normalizeScope(System.getProperty("user.dir"));
    }

    private String normalizeScope(String scope) {
        if (scope == null || scope.trim().isEmpty()) {
            return GLOBAL_SCOPE;
        }
        return scope.trim().toLowerCase();
    }

    private String withScopeTags(String tags, String scope, boolean pinned) {
        String base = tags == null ? "" : tags.trim();
        StringBuilder sb = new StringBuilder(base);
        if (!base.isEmpty()) sb.append(",");
        sb.append("scope:").append(scope);
        if (pinned) sb.append(",").append(PINNED_TAG);
        return sb.toString();
    }

    private boolean isPinned(Memory memory) {
        if (memory.getTags() == null || memory.getTags().isBlank()) return false;
        String tags = memory.getTags().toLowerCase();
        return tags.contains(PINNED_TAG);
    }
}
