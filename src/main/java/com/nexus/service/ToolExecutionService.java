package com.nexus.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ToolExecutionService {
    private static final int DEFAULT_MAX_READ_CHARS = 4000;

    public record ToolPolicy(
        boolean allowFsRead,
        boolean allowFsWrite,
        boolean allowShell,
        boolean allowExternalRead,
        boolean allowExternalWrite
    ) {}

    public record ToolResult(boolean success, String output) {}

    public List<String> listTools() {
        return List.of("fs.read", "fs.write", "shell.exec", "git.status");
    }

    public ToolResult execute(String name, Map<String, String> params, ToolPolicy policy) {
        String normalized = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "fs.read" -> fsRead(params, policy);
            case "fs.write" -> fsWrite(params, policy);
            case "shell.exec" -> shellExec(params, policy);
            case "git.status" -> gitStatus(policy);
            default -> new ToolResult(false, "Unsupported tool: " + name);
        };
    }

    private ToolResult fsRead(Map<String, String> params, ToolPolicy policy) {
        if (!policy.allowFsRead()) {
            return new ToolResult(false, "fs.read blocked by policy.");
        }

        String pathRaw = params.get("path");
        if (pathRaw == null || pathRaw.isBlank()) {
            return new ToolResult(false, "Missing parameter: path");
        }

        Path target = resolvePath(pathRaw);
        Path workspace = workspaceRoot();
        if (!target.startsWith(workspace) && !policy.allowExternalRead()) {
            return new ToolResult(false, "External file read blocked by policy.");
        }

        if (!Files.exists(target)) {
            return new ToolResult(false, "File not found: " + target);
        }

        try {
            String content = Files.readString(target, StandardCharsets.UTF_8);
            int maxChars = parseInt(params.get("maxchars"), DEFAULT_MAX_READ_CHARS);
            if (content.length() > maxChars) {
                content = content.substring(0, Math.max(0, maxChars)) + "\n...<truncated>";
            }
            return new ToolResult(true, content);
        } catch (IOException e) {
            return new ToolResult(false, "Failed to read file: " + e.getMessage());
        }
    }

    private ToolResult fsWrite(Map<String, String> params, ToolPolicy policy) {
        if (!policy.allowFsWrite()) {
            return new ToolResult(false, "fs.write blocked by policy.");
        }

        String pathRaw = params.get("path");
        String content = params.getOrDefault("content", "");
        boolean overwrite = parseBoolean(params.get("overwrite"));

        if (pathRaw == null || pathRaw.isBlank()) {
            return new ToolResult(false, "Missing parameter: path");
        }

        Path target = resolvePath(pathRaw);
        Path workspace = workspaceRoot();
        if (!target.startsWith(workspace) && !policy.allowExternalWrite()) {
            return new ToolResult(false, "External file write blocked by policy.");
        }

        try {
            if (target.getParent() != null && !Files.exists(target.getParent())) {
                Files.createDirectories(target.getParent());
            }
            if (Files.exists(target) && !overwrite) {
                return new ToolResult(false, "Target exists. Set overwrite=true to replace.");
            }
            Files.writeString(target, content, StandardCharsets.UTF_8);
            return new ToolResult(true, "Wrote file: " + target);
        } catch (IOException e) {
            return new ToolResult(false, "Failed to write file: " + e.getMessage());
        }
    }

    private ToolResult shellExec(Map<String, String> params, ToolPolicy policy) {
        if (!policy.allowShell()) {
            return new ToolResult(false, "shell.exec blocked by policy.");
        }

        String command = params.get("command");
        if (command == null || command.isBlank()) {
            return new ToolResult(false, "Missing parameter: command");
        }

        int timeout = parseInt(params.get("timeoutseconds"), 20);
        ProcessBuilder pb = buildShellCommand(command);
        pb.directory(workspaceRoot().toFile());
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            boolean completed = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                return new ToolResult(false, "shell.exec timed out after " + timeout + "s");
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (output.isBlank()) {
                output = "Command completed with exit code " + process.exitValue();
            }
            return new ToolResult(process.exitValue() == 0, output.trim());
        } catch (Exception e) {
            return new ToolResult(false, "shell.exec failed: " + e.getMessage());
        }
    }

    private ToolResult gitStatus(ToolPolicy policy) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("command", "git status --short");
        params.put("timeoutseconds", "15");
        return shellExec(params, policy);
    }

    private ProcessBuilder buildShellCommand(String command) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return new ProcessBuilder("cmd", "/c", command);
        }
        return new ProcessBuilder("bash", "-lc", command);
    }

    private Path resolvePath(String raw) {
        Path p = Paths.get(raw);
        if (!p.isAbsolute()) {
            p = workspaceRoot().resolve(p);
        }
        return p.normalize().toAbsolutePath();
    }

    private Path workspaceRoot() {
        return Paths.get(System.getProperty("user.dir")).normalize().toAbsolutePath();
    }

    private int parseInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private boolean parseBoolean(String raw) {
        if (raw == null || raw.isBlank()) return false;
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("true")
            || normalized.equals("yes")
            || normalized.equals("1")
            || normalized.equals("on")
            || normalized.equals("allow");
    }
}
