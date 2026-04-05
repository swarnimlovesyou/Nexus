package com.nexus.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Canonical timestamp codec for DB persistence.
 *
 * Storage format: UNIX epoch seconds (UTC) as INTEGER.
 * Read path is backwards-compatible with legacy TEXT timestamp rows.
 */
public class DbTimeCodec {
    private final Clock clock;

    public DbTimeCodec() {
        this(Clock.systemUTC());
    }

    public DbTimeCodec(Clock clock) {
        this.clock = clock;
    }

    public long nowEpochSeconds() {
        return clock.instant().getEpochSecond();
    }

    public long toEpochSeconds(LocalDateTime value) {
        return value.toEpochSecond(ZoneOffset.UTC);
    }

    public LocalDateTime fromEpochSeconds(long value) {
        return LocalDateTime.ofEpochSecond(value, 0, ZoneOffset.UTC);
    }

    public void setDateTime(PreparedStatement pstmt, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            pstmt.setNull(index, Types.BIGINT);
            return;
        }
        pstmt.setLong(index, toEpochSeconds(value));
    }

    public LocalDateTime readDateTime(ResultSet rs, String columnLabel) throws SQLException {
        Object raw = rs.getObject(columnLabel);
        if (raw == null) return null;

        if (raw instanceof Number n) {
            return fromEpochAuto(n.longValue());
        }
        if (raw instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (raw instanceof String s) {
            return parseString(s);
        }

        return parseString(rs.getString(columnLabel));
    }

    private LocalDateTime fromEpochAuto(long value) {
        long epochSeconds = Math.abs(value) > 9_999_999_999L ? (value / 1000L) : value;
        return fromEpochSeconds(epochSeconds);
    }

    private LocalDateTime parseString(String raw) {
        if (raw == null || raw.isBlank()) return null;

        String s = raw.trim();

        try {
            return fromEpochAuto(Long.parseLong(s));
        } catch (NumberFormatException ignored) {}

        try {
            return OffsetDateTime.parse(s).toLocalDateTime();
        } catch (Exception ignored) {}

        try {
            return LocalDateTime.ofInstant(Instant.parse(s), ZoneOffset.UTC);
        } catch (Exception ignored) {}

        String normalized = s.replace(" ", "T");
        try {
            return LocalDateTime.parse(normalized.length() >= 19 ? normalized.substring(0, 19) : normalized);
        } catch (Exception ignored) {}

        return null;
    }
}
