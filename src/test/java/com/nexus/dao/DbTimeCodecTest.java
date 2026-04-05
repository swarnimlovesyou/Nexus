package com.nexus.dao;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DbTimeCodecTest {

    @Test
    public void fixedClockShouldProduceDeterministicNowEpochSeconds() {
        Instant fixedInstant = Instant.parse("2026-04-05T10:15:30Z");
        DbTimeCodec codec = new DbTimeCodec(Clock.fixed(fixedInstant, ZoneOffset.UTC));

        assertEquals(fixedInstant.getEpochSecond(), codec.nowEpochSeconds());
    }

    @Test
    public void shouldRoundTripLocalDateTimeAsEpochSeconds() {
        DbTimeCodec codec = new DbTimeCodec(Clock.systemUTC());
        LocalDateTime original = LocalDateTime.of(2026, 4, 5, 12, 0, 0);

        long epoch = codec.toEpochSeconds(original);
        LocalDateTime roundTrip = codec.fromEpochSeconds(epoch);

        assertEquals(original, roundTrip);
    }

    @Test
    public void shouldReadLegacyTextTimestampFromResultSet() throws Exception {
        DbTimeCodec codec = new DbTimeCodec(Clock.systemUTC());
        try (Connection c = DriverManager.getConnection("jdbc:sqlite::memory:");
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT '2026-04-05 11:22:33' AS created_at")) {
            rs.next();
            LocalDateTime parsed = codec.readDateTime(rs, "created_at");
            assertEquals(LocalDateTime.of(2026, 4, 5, 11, 22, 33), parsed);
        }
    }

    @Test
    public void shouldReadEpochSecondsFromResultSet() throws Exception {
        DbTimeCodec codec = new DbTimeCodec(Clock.systemUTC());
        long epoch = 1775384130L;
        try (Connection c = DriverManager.getConnection("jdbc:sqlite::memory:");
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT " + epoch + " AS created_at")) {
            rs.next();
            LocalDateTime parsed = codec.readDateTime(rs, "created_at");
            assertEquals(codec.fromEpochSeconds(epoch), parsed);
        }
    }
}
