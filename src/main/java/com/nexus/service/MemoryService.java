package com.nexus.service;

import com.nexus.dao.AuditLogDao;
import com.nexus.dao.MemoryDao;
import com.nexus.domain.AuditLog;
import com.nexus.domain.Memory;
import com.nexus.domain.MemoryType;
import com.nexus.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MemoryService {
    private final MemoryDao memoryDao;
    private final AuditLogDao auditLogDao;

    public MemoryService() {
        this.memoryDao = new MemoryDao();
        this.auditLogDao = new AuditLogDao();
    }

    /**
     * Store a new memory. Automatically detects contradictions with existing FACTs sharing the same tag.
     */
    public Memory store(int userId, String content, String tags, MemoryType type) {
        if (content == null || content.trim().isEmpty())
            throw new ValidationException("Memory content cannot be empty.");

        // Contradiction detection: check if a FACT with same tag already exists
        if (type == MemoryType.FACT && tags != null) {
            for (String tag : tags.split(",")) {
                List<Memory> existing = memoryDao.searchContent(userId, tag.trim());
                for (Memory m : existing) {
                    if (m.getType() == MemoryType.FACT && !m.getContent().equalsIgnoreCase(content)) {
                        // Flag as contradiction automatically
                        type = MemoryType.CONTRADICTION;
                        break;
                    }
                }
                if (type == MemoryType.CONTRADICTION) break;
            }
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(type.getDefaultTtlDays());

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
            "type=" + type + " tags=" + tags, "SUCCESS", null));

        return mem;
    }

    /**
     * Hybrid recall: keyword match on content + tags, scored by confidence × recency.
     */
    public List<Memory> recall(int userId, String query) {
        List<Memory> matches = memoryDao.searchContent(userId, query);

        // Score by confidence * recency (more recent = higher weight)
        LocalDateTime now = LocalDateTime.now();
        List<Memory> scored = matches.stream()
            .sorted(Comparator.comparingDouble((Memory m) -> {
                long daysOld = java.time.temporal.ChronoUnit.DAYS.between(m.getCreatedAt(), now);
                double recencyFactor = Math.max(0, 1.0 - (daysOld / 365.0));
                return -(m.getConfidence() * 0.7 + recencyFactor * 0.3); // negative for descending
            }))
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

    /**
     * Get all memories for a user, sorted by confidence descending.
     */
    public List<Memory> getAllMemories(int userId) {
        return memoryDao.findByUserId(userId);
    }

    public List<Memory> getByType(int userId, MemoryType type) {
        return memoryDao.findByType(userId, type);
    }

    /**
     * Hard delete a memory by ID.
     */
    public void forget(int userId, int memoryId) {
        memoryDao.read(memoryId).ifPresent(m -> {
            memoryDao.delete(memoryId);
            auditLogDao.create(new AuditLog(null, userId, "MEMORY_FORGET",
                "id=" + memoryId + " content=" + m.getContent().substring(0, Math.min(40, m.getContent().length())),
                "SUCCESS", null));
        });
    }

    /**
     * Decay pass: reduce confidence 5% for each memory not accessed in >7 days.
     * Returns count of decayed memories.
     */
    public int runDecayPass(int userId) {
        List<Memory> all = memoryDao.findByUserId(userId);
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

    /**
     * Prune expired or low-confidence memories.
     */
    public int pruneExpired(int userId) {
        int pruned = memoryDao.pruneStale(userId);
        auditLogDao.create(new AuditLog(null, userId, "MEMORY_PRUNE",
            "pruned=" + pruned + " memories", "SUCCESS", null));
        return pruned;
    }

    /**
     * Get contradiction pairs for graph view.
     */
    public List<Memory> getContradictions(int userId) {
        return memoryDao.findContradictions(userId);
    }
}
