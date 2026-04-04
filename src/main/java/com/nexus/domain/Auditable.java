package com.nexus.domain;

import java.time.LocalDateTime;

public interface Auditable {
    String getAuditSummary();
    LocalDateTime getTimestamp();
}
