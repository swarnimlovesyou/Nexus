package com.nexus.presentation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import com.nexus.dao.UserDao;
import com.nexus.domain.RegularUser;
import com.nexus.domain.TaskType;
import com.nexus.domain.User;
import com.nexus.service.ProfileService;

public class NexusCommandRunnerParsingTest {

    @SuppressWarnings("unchecked")
    @Test
    public void parseKeyValueParamsShouldHandleValidAndInvalidPairs() throws Exception {
        NexusCommandRunner runner = newRunner();
        Method method = NexusCommandRunner.class.getDeclaredMethod("parseKeyValueParams", String.class);
        method.setAccessible(true);

        Map<String, String> parsed = (Map<String, String>) method.invoke(
            runner,
            "path=README.md;maxchars=1200;badpair; =nope;timeoutseconds=7"
        );

        assertEquals("README.md", parsed.get("path"));
        assertEquals("1200", parsed.get("maxchars"));
        assertEquals("7", parsed.get("timeoutseconds"));
        assertFalse(parsed.containsKey("badpair"));
    }

    @Test
    public void joinPartsShouldPreservePipeSeparatedPrompt() throws Exception {
        NexusCommandRunner runner = newRunner();
        Method method = NexusCommandRunner.class.getDeclaredMethod("joinParts", String[].class, int.class);
        method.setAccessible(true);

        String[] parts = {"CALL", "GENERAL_CHAT", "create", "a", "plan|with pipes"};
        String joined = (String) method.invoke(runner, (Object) parts, 2);

        assertEquals("create|a|plan|with pipes", joined);
    }

    @Test
    public void parseBooleanTokenShouldAcceptCommonTruthyTokens() throws Exception {
        NexusCommandRunner runner = newRunner();
        Method method = NexusCommandRunner.class.getDeclaredMethod("parseBooleanToken", String.class);
        method.setAccessible(true);

        assertTrue((boolean) method.invoke(runner, "true"));
        assertTrue((boolean) method.invoke(runner, "yes"));
        assertTrue((boolean) method.invoke(runner, "allow"));
        assertFalse((boolean) method.invoke(runner, "false"));
        assertFalse((boolean) method.invoke(runner, "0"));
    }

    @Test
    public void executeRecipeToolShouldThrowForUnsupportedTool() {
        NexusCommandRunner runner = newRunner();
        User user = new RegularUser(4242, "recipe-test", "hash", LocalDateTime.now());

        RuntimeException ex = assertThrows(
            RuntimeException.class,
            () -> invokeRecipeLine(runner, user, "TOOL|unsupported.tool|k=v")
        );

        assertTrue(ex.getMessage().contains("TOOL step failed"));
    }

    @Test
    public void executeRecipeToolShouldSucceedForFsRead() {
        NexusCommandRunner runner = newRunner();
        User user = new RegularUser(4242, "recipe-test", "hash", LocalDateTime.now());

        assertDoesNotThrow(() -> invokeRecipeLine(runner, user, "TOOL|fs.read|path=README.md;maxchars=80"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void collectToolParamsShouldMergeParamsAndExplicitFlags() throws Exception {
        NexusCommandRunner runner = newRunner();
        Method method = NexusCommandRunner.class.getDeclaredMethod("collectToolParams", Map.class);
        method.setAccessible(true);

        Map<String, String> flags = Map.of(
            "--user", "admin",
            "--name", "fs.read",
            "--params", "path=README.md;maxchars=80",
            "--maxchars", "120",
            "--timeoutseconds", "7"
        );

        Map<String, String> params = (Map<String, String>) method.invoke(runner, flags);

        assertEquals("README.md", params.get("path"));
        assertEquals("120", params.get("maxchars"));
        assertEquals("7", params.get("timeoutseconds"));
        assertFalse(params.containsKey("user"));
        assertFalse(params.containsKey("name"));
    }

    @Test
    public void executeWithFallbackShouldFailFastWhenDisabledAndNoPreferredModel() throws Exception {
        NexusCommandRunner runner = newRunner();
        int userId = ensureTestUserId();
        User user = new RegularUser(userId, "fallback-test", "hash", LocalDateTime.now());

        ProfileService profileService = new ProfileService();
        String scope = profileService.currentWorkspaceScope();
        profileService.setSetting(userId, scope, "policy.enable_provider_fallback", "false");

        Method method = NexusCommandRunner.class.getDeclaredMethod(
            "executeWithFallback",
            User.class,
            TaskType.class,
            String.class,
            com.nexus.domain.LlmModel.class
        );
        method.setAccessible(true);

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> invokeExecuteWithFallback(method, runner, user, TaskType.GENERAL_CHAT, "hello", null)
        );

        assertTrue(ex.getMessage().contains("No candidate models available"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void validateRecipeFileShouldDetectInvalidLine() throws Exception {
        NexusCommandRunner runner = newRunner();
        Method method = NexusCommandRunner.class.getDeclaredMethod("validateRecipeFile", Path.class);
        method.setAccessible(true);

        Path recipe = Files.createTempFile("nexus-invalid", ".recipe");
        Files.writeString(recipe, "CALL|NOT_A_TASK|hello\n", StandardCharsets.UTF_8);

        try {
            List<String> issues = (List<String>) method.invoke(runner, recipe);
            assertFalse(issues.isEmpty());
            assertTrue(issues.get(0).toLowerCase().contains("line"));
        } finally {
            Files.deleteIfExists(recipe);
        }
    }

    @Test
    public void validateGeneratedContentStrictShouldRejectProsePrefix() throws Exception {
        NexusCommandRunner runner = newRunner();
        Method method = NexusCommandRunner.class.getDeclaredMethod(
            "validateGeneratedContent", String.class, String.class, String.class, boolean.class);
        method.setAccessible(true);

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> invokeValidateGeneratedContent(method, runner,
                "Here's your Java code: public class A {}", "A.java", "java", true)
        );

        assertTrue(ex.getMessage().toLowerCase().contains("strict codegen"));
    }

    private NexusCommandRunner newRunner() {
        Scanner scanner = new Scanner(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
        return new NexusCommandRunner(scanner);
    }

    private int ensureTestUserId() {
        UserDao userDao = new UserDao();
        Optional<User> existing = userDao.findByUsername("fallback_test_user");
        if (existing.isPresent()) {
            return existing.get().getId();
        }

        RegularUser user = new RegularUser(null, "fallback_test_user", "test-hash", LocalDateTime.now());
        userDao.create(user);
        return user.getId();
    }

    private void invokeRecipeLine(NexusCommandRunner runner, User user, String line) throws Exception {
        Method method = NexusCommandRunner.class.getDeclaredMethod("executeRecipeLine", User.class, String.class);
        method.setAccessible(true);
        try {
            method.invoke(runner, user, line);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(cause);
        }
    }

    private void invokeExecuteWithFallback(
        Method method,
        NexusCommandRunner runner,
        User user,
        TaskType task,
        String prompt,
        com.nexus.domain.LlmModel preferredModel
    ) throws Exception {
        try {
            method.invoke(runner, user, task, prompt, preferredModel);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof IllegalArgumentException illegal) {
                throw illegal;
            }
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(cause);
        }
    }

    private void invokeValidateGeneratedContent(
        Method method,
        NexusCommandRunner runner,
        String content,
        String outputPath,
        String format,
        boolean strict
    ) throws Exception {
        try {
            method.invoke(runner, content, outputPath, format, strict);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof IllegalArgumentException illegal) {
                throw illegal;
            }
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(cause);
        }
    }
}