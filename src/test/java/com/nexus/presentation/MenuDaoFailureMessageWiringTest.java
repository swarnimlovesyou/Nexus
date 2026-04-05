package com.nexus.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import com.nexus.domain.AdminUser;
import com.nexus.domain.RegularUser;
import com.nexus.domain.User;

public class MenuDaoFailureMessageWiringTest {

    @Test
    public void memoryForgetShouldUseClearDaoFailureMessage() {
        MessageCaptureMenuContext ctx = new MessageCaptureMenuContext("4\nB\n", regularUser());

        new MemoryMenu(ctx).show();

        assertEquals(
            "Could not forget memory. Database operation failed; no changes were saved.",
            ctx.lastFailureMessage()
        );
    }

    @Test
    public void historyQualityUpdateShouldUseClearDaoFailureMessage() {
        MessageCaptureMenuContext ctx = new MessageCaptureMenuContext("5\nB\n", regularUser());

        new HistoryMenu(ctx).show();

        assertEquals(
            "Could not update execution quality. Database operation failed; no changes were saved.",
            ctx.lastFailureMessage()
        );
    }

    @Test
    public void routingStartSessionShouldUseClearDaoFailureMessage() {
        MessageCaptureMenuContext ctx = new MessageCaptureMenuContext("6\nB\n", regularUser());

        new RoutingMenu(ctx).show();

        assertEquals(
            "Could not start session. Database operation failed; no changes were saved.",
            ctx.lastFailureMessage()
        );
    }

    @Test
    public void adminRegisterModelShouldUseClearDaoFailureMessage() {
        MessageCaptureMenuContext ctx = new MessageCaptureMenuContext("4\nB\n", adminUser());

        new AdminMenu(ctx).show();

        assertEquals(
            "Could not register model. Database operation failed; no changes were saved.",
            ctx.lastFailureMessage()
        );
    }

    private static User regularUser() {
        return new RegularUser(101, "tester", "hash", LocalDateTime.now());
    }

    private static User adminUser() {
        return new AdminUser(1, "admin", "hash", LocalDateTime.now());
    }

    private static class MessageCaptureMenuContext extends MenuContext {
        private final Scanner scriptedScanner;
        private String lastFailureMessage;

        MessageCaptureMenuContext(String scriptedInput, User user) {
            this.scriptedScanner = new Scanner(new ByteArrayInputStream(scriptedInput.getBytes(StandardCharsets.UTF_8)));
            setLoggedInUser(user);
        }

        @Override
        public Scanner scanner() {
            return scriptedScanner;
        }

        @Override
        public void runWithDaoGuard(String failureMessage, Runnable action) {
            this.lastFailureMessage = failureMessage;
            // We only verify message wiring here; action execution is tested separately.
        }

        String lastFailureMessage() {
            return lastFailureMessage;
        }
    }
}
