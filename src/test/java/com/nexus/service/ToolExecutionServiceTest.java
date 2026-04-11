package com.nexus.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class ToolExecutionServiceTest {

    @Test
    public void fsReadShouldRespectPolicyBlock() {
        ToolExecutionService service = new ToolExecutionService();
        ToolExecutionService.ToolPolicy policy = new ToolExecutionService.ToolPolicy(false, true, false, false, false);

        ToolExecutionService.ToolResult result = service.execute(
            "fs.read",
            Map.of("path", "README.md"),
            policy
        );

        assertFalse(result.success());
        assertTrue(result.output().contains("blocked by policy"));
    }

    @Test
    public void fsReadShouldBlockExternalPathWhenPolicyDisallowsIt() throws Exception {
        ToolExecutionService service = new ToolExecutionService();
        ToolExecutionService.ToolPolicy policy = new ToolExecutionService.ToolPolicy(true, true, false, false, false);

        Path external = Files.createTempFile("nexus-ext-read", ".txt");
        Files.writeString(external, "outside", StandardCharsets.UTF_8);

        try {
            ToolExecutionService.ToolResult result = service.execute(
                "fs.read",
                Map.of("path", external.toString()),
                policy
            );

            assertFalse(result.success());
            assertTrue(result.output().contains("External file read blocked"));
        } finally {
            Files.deleteIfExists(external);
        }
    }

    @Test
    public void fsWriteShouldCreateWorkspaceFileWhenAllowed() throws Exception {
        ToolExecutionService service = new ToolExecutionService();
        ToolExecutionService.ToolPolicy policy = new ToolExecutionService.ToolPolicy(true, true, false, false, false);

        Path workspace = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path target = workspace.resolve("target").resolve("tool-write-test.txt");
        Files.deleteIfExists(target);

        try {
            ToolExecutionService.ToolResult result = service.execute(
                "fs.write",
                Map.of(
                    "path", "target/tool-write-test.txt",
                    "content", "hello tool",
                    "overwrite", "true"
                ),
                policy
            );

            assertTrue(result.success());
            assertTrue(Files.exists(target));
            assertTrue(Files.readString(target, StandardCharsets.UTF_8).contains("hello tool"));
        } finally {
            Files.deleteIfExists(target);
        }
    }

    @Test
    public void shellExecShouldBeBlockedWhenPolicyDisallows() {
        ToolExecutionService service = new ToolExecutionService();
        ToolExecutionService.ToolPolicy policy = new ToolExecutionService.ToolPolicy(true, true, false, false, false);

        ToolExecutionService.ToolResult result = service.execute(
            "shell.exec",
            Map.of("command", "echo hi"),
            policy
        );

        assertFalse(result.success());
        assertTrue(result.output().contains("blocked by policy"));
    }

    @Test
    public void shellExecShouldRunWhenPolicyAllows() {
        ToolExecutionService service = new ToolExecutionService();
        ToolExecutionService.ToolPolicy policy = new ToolExecutionService.ToolPolicy(true, true, true, false, false);

        ToolExecutionService.ToolResult result = service.execute(
            "shell.exec",
            Map.of("command", "echo nexus-shell-test", "timeoutseconds", "5"),
            policy
        );

        assertTrue(result.success());
        assertTrue(result.output().toLowerCase().contains("nexus-shell-test"));
    }
}