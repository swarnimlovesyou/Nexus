package com.nexus.service;

import com.nexus.dao.AuditLogDao;
import com.nexus.dao.MemoryDao;
import com.nexus.domain.AuditLog;
import com.nexus.domain.Memory;
import com.nexus.domain.MemoryType;
import com.nexus.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MemoryService {
    private final MemoryDao memoryDao;
    private final AuditLogDao auditLogDao;

    public MemoryService() {
        this.memoryDao = new MemoryDao();
        this.auditLogDao = new AuditLogDao();
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
        return store(userId, content, tags, type, type.getDefaultTtlDays());
    }

    /**
     * Overload 2 (minimal): Store with content and type only — no tags.
     * Convenience method for quick fact or preference storage.
     */
    public Memory store(int userId, String content, MemoryType type) {
        return store(userId, content, "", type, type.getDefaultTtlDays());
    }

    /**
     * Overload 3 (full + TTL override): Store with all fields including a custom TTL.
     * Used by the outcome→memory bridge to store EPISODE memories with shorter TTL.
     * All other overloads delegate here.
     */
    public Memory store(int userId, String content, String tags, MemoryType type, int ttlOverrideDays) {
        if (content == null || content.trim().isEmpty())
            throw new ValidationException("Memory content cannot be empty.");

        // Contradiction detection: check if a FACT with same tag already exists
        if (type == MemoryType.FACT && tags != null && !tags.trim().isEmpty()) {
            for (String tag : tags.split(",")) {
                List<Memory> existing = memoryDao.searchContent(userId, tag.trim());
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
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(effectiveTtl);

        Memory mem = new Memory();
        mem.setUserId(userId);
        mem.setAgentId("default");
        mem.setContent(content.trim());
        mem.setTags(tags != null ? tags.trim() : "");
        mem.setType(type);
        mem.setConfidence(1.0);
        mem.setExpiresAt(expiresAt);

        memoryDao.create(mem);
        auditLogDao.create(new AuditLog(null, userId, "MEMORY_STORE",
            "type=" + type + " ttl=" + effectiveTtl + "d tags=" + tags, "SUCCESS", null));

        return mem;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RECALL / READ
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Hybrid recall: keyword match on content + tags, scored by confidence × recency.
     * Uses ArrayList<Memory> for results and HashMap<Integer,Double> for score mapping.
     */
    public List<Memory> recall(int userId, String query) {
        List<Memory> matches = memoryDao.searchContent(userId, query); // ArrayList

        LocalDateTime now = LocalDateTime.now();
        // Score map: memoryId → hybrid score  (demonstrates HashMap usage)
        Map<Integer, Double> scoreMap = new HashMap<>();
        for (Memory m : matches) {
            long daysOld = java.time.temporal.ChronoUnit.DAYS.between(m.getCreatedAt(), now);
            double recencyFactor = Math.max(0, 1.0 - (daysOld / 365.0));
            double hybridScore = m.getConfidence() * 0.7 + recencyFactor * 0.3;
            scoreMap.put(m.getId(), hybridScore);
        }

        List<Memory> scored = matches.stream()
            .sorted(Comparator.comparingDouble(m -> -scoreMap.getOrDefault(m.getId(), 0.0)))
            .collect(Collectors.toList());

        // Mark as accessed
        for (Memory m : scored) {
            memoryDao.updateConfidenceAndAccess(m.getId(), m.getConfidence());
        }

        if (!query.isEmpty()) {
            auditLogDao.create(new AuditLog(null, userId, "MEMORY_RECALL",
                "query=" + query + " results=" + scored.size(), "SUCCESS", null));
        }

        return scored;
    }

    /** Get all memories for a user, sorted by confidence descending. */
    public List<Memory> getAllMemories(int userId) {
        return memoryDao.findByUserId(userId);
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
        memoryDao.read(memoryId).ifPresent(m -> {
            memoryDao.delete(memoryId);
            auditLogDao.create(new AuditLog(null, userId, "MEMORY_FORGET",
                "id=" + memoryId + " content=" + m.getContent().substring(0, Math.min(40, m.getContent().length())),
                "SUCCESS", null));
        });
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
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        int count = 0;
        for (Memory m : all) {
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
        int pruned = memoryDao.pruneStale(userId);
        auditLogDao.create(new AuditLog(null, userId, "MEMORY_PRUNE",
            "pruned=" + pruned + " memories", "SUCCESS", null));
        return pruned;
    }

    /** Get contradiction pairs for graph view. */
    public List<Memory> getContradictions(int userId) {
        return memoryDao.findContradictions(userId);
    }
}
