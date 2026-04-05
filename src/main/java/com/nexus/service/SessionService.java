package com.nexus.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import com.nexus.dao.AuditLogDao;
import com.nexus.dao.LlmModelDao;
import com.nexus.dao.OutcomeMemoryDao;
import com.nexus.dao.SessionDao;
import com.nexus.domain.AgentSession;
import com.nexus.domain.AuditLog;
import com.nexus.domain.LlmModel;
import com.nexus.domain.OutcomeMemory;
import com.nexus.domain.TaskType;
import com.nexus.exception.ResourceNotFoundException;
import com.nexus.exception.ValidationException;

/**
 * Session context service.
 * Sessions make outcome tracking coherent for real developer workflows.
 */
public class SessionService {
    private final SessionDao sessionDao;
    private final LlmModelDao modelDao;
    private final OutcomeMemoryDao outcomeDao;
    private final AuditLogDao auditLogDao;
    private final Clock clock;

    public SessionService() {
        this(Clock.systemUTC());
    }

    public SessionService(Clock clock) {
        this(new SessionDao(), new LlmModelDao(), new OutcomeMemoryDao(), new AuditLogDao(), clock);
    }

    SessionService(SessionDao sessionDao, LlmModelDao modelDao, OutcomeMemoryDao outcomeDao,
                   AuditLogDao auditLogDao, Clock clock) {
        this.sessionDao = sessionDao;
        this.modelDao = modelDao;
        this.outcomeDao = outcomeDao;
        this.auditLogDao = auditLogDao;
        this.clock = clock;
    }

    public AgentSession startSession(int userId, TaskType taskType, int modelId, String notes) {
        List<AgentSession> active = sessionDao.findActiveByUserId(userId);
        if (!active.isEmpty()) {
            throw new ValidationException("You already have an active session (#" + active.get(0).getId() + "). Close it first.");
        }

        if (modelDao.read(modelId).isEmpty()) {
            throw new ValidationException("Selected model does not exist.");
        }

        AgentSession session = new AgentSession();
        session.setUserId(userId);
        session.setTaskType(taskType);
        session.setModelId(modelId);
        session.setStatus("ACTIVE");
        session.setNotes(notes == null ? "" : notes.trim());
        sessionDao.create(session);

        auditLogDao.create(new AuditLog(null, userId, "SESSION_START",
            "sessionId=" + session.getId() + " task=" + taskType + " modelId=" + modelId,
            "SUCCESS", null));

        return session;
    }

    /**
     * Close a session and automatically persist one OutcomeMemory record.
     */
    public AgentSession closeSession(int userId, int sessionId, int inputTokens, int outputTokens,
                                     double qualityScore, String notes) {
        AgentSession session = sessionDao.read(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session not found."));

        if (!session.getUserId().equals(userId)) {
            throw new ValidationException("Cannot close another user's session.");
        }
        if (!session.isActive()) {
            throw new ValidationException("Session is already closed.");
        }
        if (inputTokens < 0 || outputTokens < 0) {
            throw new ValidationException("Token counts must be non-negative.");
        }
        if (qualityScore < 0.0 || qualityScore > 1.0) {
            throw new ValidationException("Quality score must be between 0.0 and 1.0.");
        }

        LlmModel model = modelDao.read(session.getModelId())
            .orElseThrow(() -> new ValidationException("Session model no longer exists."));

        int totalTokens = inputTokens + outputTokens;
        double totalCost = (totalTokens / 1000.0) * model.getCostPer1kTokens();
        LocalDateTime now = LocalDateTime.now(clock);

        session.setInputTokens(inputTokens);
        session.setOutputTokens(outputTokens);
        session.setTotalCost(totalCost);
        session.setQualityScore(qualityScore);
        session.setStatus("CLOSED");
        session.setEndedAt(now);
        if (notes != null && !notes.trim().isEmpty()) {
            session.setNotes(notes.trim());
        }
        sessionDao.update(session);

        long latencyMsLong = Duration.between(session.getCreatedAt(), now).toMillis();
        int latencyMs = latencyMsLong > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) latencyMsLong;

        OutcomeMemory outcome = new OutcomeMemory(
            null,
            userId,
            session.getModelId(),
            session.getTaskType(),
            totalCost,
            latencyMs,
            qualityScore,
            null
        );
        outcomeDao.create(outcome);

        auditLogDao.create(new AuditLog(null, userId, "SESSION_CLOSE",
            "sessionId=" + sessionId + " outcomeId=" + outcome.getId() + " totalCost=" + String.format("%.6f", totalCost),
            "SUCCESS", null));

        return session;
    }

    public List<AgentSession> listUserSessions(int userId) {
        return sessionDao.findByUserId(userId);
    }

    public List<AgentSession> listActiveSessions(int userId) {
        return sessionDao.findActiveByUserId(userId);
    }
}
