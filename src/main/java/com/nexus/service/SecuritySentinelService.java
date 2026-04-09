package com.nexus.service;

import com.nexus.dao.AuditLogDao;
import com.nexus.domain.AuditLog;
import com.nexus.util.TerminalUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Proactive Security Sentinel.
 * Scans the local filesystem for leaked credentials, secrets, and high-risk patterns.
 */
public class SecuritySentinelService {

    private final AuditLogDao auditLogDao;
    
    // Patterns for common secrets and high-risk calls
    private static final Map<String, Pattern> RULES = new HashMap<>() {{
        put("AWS_KEY", Pattern.compile("AKIA[0-9A-Z]{16}"));
        put("GENERIC_ID", Pattern.compile("(?i)(api[_-]?key|secret|password|passwd|auth[_-]?token)[\"']?\\s*[:=]\\s*[\"']([a-zA-Z0-9$_.+!*'(),-]{8,})[\"']"));
        put("SQL_INJECTION", Pattern.compile("(?i)Statement\\.execute\\(\\s*\".*\\+\\s*[a-zA-Z0-9_]+\\s*.*\"\\)"));
        put("HARDCODED_IP", Pattern.compile("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"));
    }};

    public SecuritySentinelService() {
        this.auditLogDao = new AuditLogDao();
    }

    /**
     * Executes a full security audit of the local codebase.
     */
    public List<SecurityFinding> performFullAudit(int userId, String rootPath) {
        TerminalUtils.printInfo("Nexus Security Sentinel is scanning for risks...");
        List<SecurityFinding> findings = new ArrayList<>();
        Path root = Paths.get(rootPath);

        try (var stream = Files.walk(root)) {
            List<Path> filesToScan = stream
                .filter(p -> !p.toString().contains(".git") && !p.toString().contains("target"))
                .filter(p -> Files.isRegularFile(p))
                .collect(Collectors.toList());

            for (Path path : filesToScan) {
                try {
                    String content = Files.readString(path);
                    String fileName = path.getFileName().toString();

                    RULES.forEach((ruleName, pattern) -> {
                        Matcher m = pattern.matcher(content);
                        while (m.find()) {
                            findings.add(new SecurityFinding(ruleName, fileName, m.start(), "High"));
                        }
                    });
                } catch (Exception ignored) {} // Skip binary/unreadable files
            }

            if (!findings.isEmpty()) {
                String logDetails = findings.stream()
                    .map(f -> f.type() + " in " + f.file())
                    .collect(Collectors.joining(", "));
                auditLogDao.create(new AuditLog(null, userId, "SECURITY_AUDIT", "Found " + findings.size() + " risks: " + logDetails, "WARNING", null));
            }

        } catch (IOException e) {
            TerminalUtils.printError("Security audit failed: " + e.getMessage());
        }
        return findings;
    }

    public record SecurityFinding(String type, String file, int position, String severity) {}
}
