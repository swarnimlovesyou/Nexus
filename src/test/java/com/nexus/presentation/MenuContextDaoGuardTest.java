package com.nexus.presentation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.nexus.exception.DaoException;
import com.nexus.util.TerminalUtils;

public class MenuContextDaoGuardTest {

    @Test
    public void runWithDaoGuardShouldPrintFriendlyMessageWithoutLeakingDaoDetails() {
        MenuContext ctx = new MenuContext();

        String output = captureStdOut(() ->
            ctx.runWithDaoGuard("Database operation failed; no changes were saved.", () -> {
                throw new DaoException("SQLITE_BUSY: low-level details");
            })
        );

        String normalized = TerminalUtils.stripAnsi(output);
        assertTrue(normalized.contains("Database operation failed; no changes were saved."));
        assertFalse(normalized.contains("SQLITE_BUSY"));
    }

    private String captureStdOut(Runnable action) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            action.run();
        } finally {
            System.setOut(originalOut);
        }

        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }
}
