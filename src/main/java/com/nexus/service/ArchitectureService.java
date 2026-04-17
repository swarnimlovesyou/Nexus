package com.nexus.service;

import com.nexus.domain.MemoryType;
import com.nexus.util.TerminalUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The Architectural Memory Engine.
 * Scans the local codebase to build a semantic graph of class relationships
 * and stores them in the Memory Vault.
 */
public class ArchitectureService {

    private final MemoryService memoryService;
    private static final Pattern CLASS_PATTERN = Pattern.compile("class\\s+([A-Z][a-zA-Z0-9_]*)");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("import\\s+([a-zA-Z0-9_\\.]+)");

    public ArchitectureService() {
        this.memoryService = new MemoryService();
    }

    /**
     * Deep scan of the current workspace to map the DNA of the project.
     */
    public int buildContextGraph(int userId, String rootPath) {
        TerminalUtils.printInfo("Nexus is indexing your project architecture...");
        Path root = Paths.get(rootPath);
        Map<String, List<String>> relations = new HashMap<>();
        int count = 0;

        try (var stream = Files.walk(root)) {
            List<Path> javaFiles = stream
                .filter(p -> p.toString().endsWith(".java"))
                .collect(Collectors.toList());

            for (Path path : javaFiles) {
                String content = Files.readString(path);
                String className = extractClassName(content);
                if (className == null) continue;

                List<String> imports = extractImports(content);
                relations.put(className, imports);
                
                // Store individual file context
                String summary = String.format("Class: %s | Location: %s | Dependencies: %s", 
                    className, path.getFileName(), String.join(", ", imports));
                
                memoryService.store(userId, summary, 
                    "ARCH_MAP," + className, MemoryType.FACT);
                count++;
            }

            // Store the global relationship graph as a single intelligence artifact
            storeGlobalGraph(userId, relations);

        } catch (IOException e) {
            TerminalUtils.printError("Architectural scan failed: " + e.getMessage());
        }
        return count;
    }

    private String extractClassName(String content) {
        Matcher matcher = CLASS_PATTERN.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }

    private List<String> extractImports(String content) {
        List<String> imports = new ArrayList<>();
        Matcher matcher = IMPORT_PATTERN.matcher(content);
        while (matcher.find()) {
            String imp = matcher.group(1);
            if (imp.contains("com.nexus")) { // Only track internal dependencies for the graph
                String[] parts = imp.split("\\.");
                imports.add(parts[parts.length - 1]);
            }
        }
        return imports;
    }

    private void storeGlobalGraph(int userId, Map<String, List<String>> relations) {
        StringBuilder sb = new StringBuilder("PROJECT_DNA_GRAPH:\n");
        relations.forEach((clazz, deps) -> {
            sb.append(clazz).append(" -> ").append(String.join(", ", deps)).append("\n");
        });
        memoryService.store(userId, sb.toString(), 
            "GLOBAL_DNA,GRAPH", MemoryType.FACT);
    }
}
