package com.nexus.presentation;

import com.nexus.domain.Memory;
import com.nexus.domain.MemoryType;
import com.nexus.util.TerminalUtils;

import java.util.List;
import java.util.Map;

/**
 * Memory Vault menu — full CRUD surface (Create/Read/Update/Delete) for Memory entity.
 * Demonstrates: ArrayList iteration, HashMap usage, method delegation to MemoryService.
 */
public class MemoryMenu {
    private final MenuContext ctx;

    public MemoryMenu(MenuContext ctx) { this.ctx = ctx; }

    public void show() {
        while (true) {
            TerminalUtils.printHeader("Memory Vault  ·  Contextd Layer");
            System.out.println("  " + TerminalUtils.AMBER + "1" + TerminalUtils.RESET + "  Store new memory");
            System.out.println("  " + TerminalUtils.AMBER + "2" + TerminalUtils.RESET + "  Recall memories  " + TerminalUtils.GRAY + "(keyword search, ranked by confidence × recency)" + TerminalUtils.RESET);
            System.out.println("  " + TerminalUtils.AMBER + "3" + TerminalUtils.RESET + "  View full vault");
            System.out.println("  " + TerminalUtils.AMBER + "4" + TerminalUtils.RESET + "  Forget memory  " + TerminalUtils.GRAY + "(hard delete by ID)" + TerminalUtils.RESET);
            System.out.println("  " + TerminalUtils.AMBER + "5" + TerminalUtils.RESET + "  Contradiction graph");
            System.out.println("  " + TerminalUtils.AMBER + "6" + TerminalUtils.RESET + "  Run decay pass  " + TerminalUtils.GRAY + "(-5% confidence on stale memories)" + TerminalUtils.RESET);
            System.out.println("  " + TerminalUtils.AMBER + "7" + TerminalUtils.RESET + "  Prune expired / low-confidence");
            System.out.println("  " + TerminalUtils.AMBER + "8" + TerminalUtils.RESET + "  Filter by memory type");
            System.out.println("  " + TerminalUtils.AMBER + "9" + TerminalUtils.RESET + "  Edit memory content  " + TerminalUtils.GRAY + "(update by ID)" + TerminalUtils.RESET);
            System.out.println("  " + TerminalUtils.AMBER + "E" + TerminalUtils.RESET + "  Export vault to CSV");
            System.out.println("  " + TerminalUtils.AMBER + "B" + TerminalUtils.RESET + "  Back");
            System.out.println();
            TerminalUtils.printPrompt(ctx.username());
            switch (ctx.scanner().nextLine().trim().toUpperCase()) {
                case "1" -> ctx.runWithDaoGuard("Could not store memory. Database operation failed; no changes were saved.", this::storeMemory);
                case "2" -> ctx.runWithDaoGuard("Unable to recall memories right now. Please try again.", this::recallMemories);
                case "3" -> ctx.runWithDaoGuard("Unable to load memory vault right now. Please try again.", this::viewVault);
                case "4" -> ctx.runWithDaoGuard("Could not forget memory. Database operation failed; no changes were saved.", this::forgetMemory);
                case "5" -> ctx.runWithDaoGuard("Unable to load contradiction graph right now. Please try again.", this::contradictionGraph);
                case "6" -> ctx.runWithDaoGuard("Could not run decay pass. Database operation failed; no changes were saved.", this::runDecay);
                case "7" -> ctx.runWithDaoGuard("Could not prune memories. Database operation failed; no changes were saved.", this::pruneMemories);
                case "8" -> ctx.runWithDaoGuard("Unable to filter memories right now. Please try again.", this::filterByType);
                case "9" -> ctx.runWithDaoGuard("Could not edit memory. Database operation failed; no changes were saved.", this::editMemory);
                case "E" -> ctx.runWithDaoGuard("Could not export vault right now because memory data could not be loaded.", this::exportVault);
                case "B" -> { return; }
                default  -> TerminalUtils.printError("Unknown option.");
            }
        }
    }

    private void storeMemory() {
        TerminalUtils.printSeparator("STORE MEMORY");
        String scope = askScope();
        System.out.print("  Content: "); String content = ctx.scanner().nextLine();
        System.out.print("  Tags (comma-separated): "); String tags = ctx.scanner().nextLine();
        System.out.println("  Type: 1=FACT  2=PREFERENCE  3=EPISODE  4=SKILL");
        System.out.print("  Type #: ");
        MemoryType[] types = {MemoryType.FACT, MemoryType.PREFERENCE, MemoryType.EPISODE, MemoryType.SKILL};
        int tidx = ctx.safeInt(ctx.scanner().nextLine()) - 1;
        if (tidx < 0 || tidx >= types.length) { TerminalUtils.printError("Invalid type."); return; }
        MemoryType type = types[tidx];

        System.out.print("  Pin for long-term retention? (yes/no): ");
        boolean pinned = "yes".equalsIgnoreCase(ctx.scanner().nextLine().trim());

        int ttlDays = type.getDefaultTtlDays();

        TerminalUtils.spinner("Encoding and persisting memory...", 500);
        Memory mem = com.nexus.service.MemoryService.GLOBAL_SCOPE.equals(scope)
            ? ctx.memoryService().storeGlobal(ctx.userId(), content, tags, type, ttlDays, pinned)
            : ctx.memoryService().storeScoped(ctx.userId(), scope, content, tags, type, ttlDays, pinned);

        if (mem.getType() == MemoryType.CONTRADICTION) {
            TerminalUtils.printWarn("Contradiction detected with existing FACT. Stored as CONTRADICTION type.");
        } else {
            TerminalUtils.printSuccess("Memory stored. ID: " + mem.getId() + "  TTL: " + mem.getType().getDefaultTtlDays() + " days");
            TerminalUtils.printInfo("Scope: " + scope + " | Pinned: " + (pinned ? "yes" : "no"));
        }
    }

    private void recallMemories() {
        TerminalUtils.printSeparator("RECALL MEMORIES");
        String scope = askScope();
        System.out.print("  Search query: "); String query = ctx.scanner().nextLine();
        TerminalUtils.spinner("Executing hybrid retrieval...", 600);

        List<Memory> results = ctx.memoryService().recallForScope(ctx.userId(), scope, query);
        if (results.isEmpty()) { TerminalUtils.printInfo("No memories matched."); return; }

        System.out.println();
        String[] headers = {"ID", "Type", "Confidence", "Content", "Tags", "TTL"};
        String[][] rows  = new String[Math.min(results.size(), 10)][6];
        for (int i = 0; i < rows.length; i++) {
            Memory m = results.get(i);
            rows[i] = new String[]{
                String.valueOf(m.getId()),
                TerminalUtils.AMBER + m.getType().name() + TerminalUtils.RESET,
                TerminalUtils.confidenceBar(m.getConfidence()),
                m.getContent().length() > 45 ? m.getContent().substring(0, 42) + "..." : m.getContent(),
                m.getTags() != null ? m.getTags() : "",
                m.daysUntilExpiry() == Long.MAX_VALUE ? "∞" : m.daysUntilExpiry() + "d"
            };
        }
        TerminalUtils.printTable(headers, rows);
        if (results.size() > 10) TerminalUtils.printInfo("Showing top 10 of " + results.size() + " results.");
        TerminalUtils.printInfo("Recall scope: " + scope);
    }

    private void viewVault() {
        TerminalUtils.printSeparator("FULL MEMORY VAULT");
        System.out.println("  1. Current project scope");
        System.out.println("  2. Global scope");
        System.out.println("  3. All scopes");
        System.out.print("  View #: ");
        int mode = ctx.safeInt(ctx.scanner().nextLine());

        List<Memory> all;
        String scopeLabel;
        if (mode == 1) {
            scopeLabel = ctx.memoryService().currentWorkspaceScope();
            all = ctx.memoryService().getByScope(ctx.userId(), scopeLabel);
        } else if (mode == 2) {
            scopeLabel = com.nexus.service.MemoryService.GLOBAL_SCOPE;
            all = ctx.memoryService().getByScope(ctx.userId(), scopeLabel);
        } else {
            scopeLabel = "all";
            all = ctx.memoryService().getAllMemories(ctx.userId());
        }
        if (all.isEmpty()) { TerminalUtils.printInfo("Vault is empty. Use 'Store memory' to add entries."); return; }

        System.out.println();
        for (MemoryType t : MemoryType.values()) {
            List<Memory> typed = all.stream().filter(m -> m.getType() == t).toList();
            if (typed.isEmpty()) continue;
            TerminalUtils.printSeparator(t.name() + " · " + t.getDescription());
            for (Memory m : typed) {
                String bar = TerminalUtils.confidenceBar(m.getConfidence());
                System.out.printf("  " + TerminalUtils.GRAY + "#%-4d" + TerminalUtils.RESET + " %s  %s  | %s%n",
                    m.getId(), bar, m.getContent().length() > 50 ? m.getContent().substring(0, 47) + "..." : m.getContent(),
                    m.getTags() != null ? m.getTags() : "");
            }
        }
        System.out.println();
        TerminalUtils.printInfo("Total memories: " + all.size());
        TerminalUtils.printInfo("Scope: " + scopeLabel);
    }

    private void forgetMemory() {
        TerminalUtils.printSeparator("FORGET MEMORY");
        System.out.print("  Memory ID to forget: ");
        int id = ctx.safeInt(ctx.scanner().nextLine());
        if (id <= 0) { TerminalUtils.printError("Invalid ID."); return; }
        System.out.print("  Confirm delete? (yes/no): ");
        if ("yes".equalsIgnoreCase(ctx.scanner().nextLine().trim())) {
            ctx.memoryService().forget(ctx.userId(), id);
            TerminalUtils.printSuccess("Memory #" + id + " erased from vault.");
        } else {
            TerminalUtils.printInfo("Cancelled.");
        }
    }

    private void contradictionGraph() {
        TerminalUtils.printSeparator("CONTRADICTION GRAPH");
        List<Memory> contradictions = ctx.memoryService().getContradictions(ctx.userId());
        if (contradictions.isEmpty()) {
            TerminalUtils.printSuccess("No contradictions found. Memory graph is clean.");
            return;
        }
        System.out.println();
        for (Memory c : contradictions) {
            System.out.println("  " + TerminalUtils.RED + "⚡ CONTRADICTION #" + c.getId() + TerminalUtils.RESET);
            System.out.println("  " + TerminalUtils.GRAY + "Content: " + TerminalUtils.RESET + c.getContent());
            System.out.println("  " + TerminalUtils.GRAY + "Tags: " + TerminalUtils.RESET + c.getTags());
            System.out.println("  " + TerminalUtils.GRAY + "Confidence: " + TerminalUtils.RESET + TerminalUtils.confidenceBar(c.getConfidence()));
            System.out.println();
        }
    }

    private void runDecay() {
        TerminalUtils.spinner("Running confidence decay pass...", 800);
        int decayed = ctx.memoryService().runDecayPass(ctx.userId());
        TerminalUtils.printSuccess(decayed + " memories decayed by 5% confidence.");
    }

    private void pruneMemories() {
        System.out.print("  Prune all expired / low-confidence memories? (yes/no): ");
        if ("yes".equalsIgnoreCase(ctx.scanner().nextLine().trim())) {
            TerminalUtils.spinner("Pruning stale memories...", 600);
            int pruned = ctx.memoryService().pruneExpired(ctx.userId());
            TerminalUtils.printSuccess(pruned + " memories pruned.");
        } else {
            TerminalUtils.printInfo("Cancelled.");
        }
    }

    private void filterByType() {
        TerminalUtils.printSeparator("FILTER BY MEMORY TYPE");
        Map<MemoryType, Integer> counts = ctx.memoryService().getTypeCounts(ctx.userId());
        for (MemoryType t : MemoryType.values()) {
            System.out.printf("  " + TerminalUtils.AMBER + "%-2s" + TerminalUtils.RESET + " %-20s  %d memories%n",
                String.valueOf(t.ordinal() + 1), t.name() + " (" + t.getDescription() + ")", counts.getOrDefault(t, 0));
        }
        System.out.print("  Type # (1-" + MemoryType.values().length + "): ");
        int tidx = ctx.safeInt(ctx.scanner().nextLine()) - 1;
        if (tidx < 0 || tidx >= MemoryType.values().length) { TerminalUtils.printError("Invalid selection."); return; }
        MemoryType selected = MemoryType.values()[tidx];

        List<Memory> typed = ctx.memoryService().getByType(ctx.userId(), selected);
        if (typed.isEmpty()) { TerminalUtils.printInfo("No " + selected.name() + " memories found."); return; }

        System.out.println();
        String[] headers = {"ID", "Confidence", "Content", "Tags", "Expires"};
        String[][] rows  = new String[typed.size()][5];
        for (int i = 0; i < typed.size(); i++) {
            Memory m = typed.get(i);
            rows[i] = new String[]{
                String.valueOf(m.getId()),
                TerminalUtils.confidenceBar(m.getConfidence()),
                m.getContent().length() > 45 ? m.getContent().substring(0, 42) + "..." : m.getContent(),
                m.getTags() != null ? m.getTags() : "",
                m.daysUntilExpiry() == Long.MAX_VALUE ? "\u221e" : m.daysUntilExpiry() + "d"
            };
        }
        TerminalUtils.printTable(headers, rows);
        TerminalUtils.printInfo(typed.size() + " " + selected.name() + " memories.");
    }

    private void editMemory() {
        TerminalUtils.printSeparator("EDIT MEMORY CONTENT");
        System.out.print("  Memory ID to edit: ");
        int id = ctx.safeInt(ctx.scanner().nextLine());
        if (id <= 0) { TerminalUtils.printError("Invalid ID."); return; }

        ctx.memoryService().getAllMemories(ctx.userId()).stream()
            .filter(m -> m.getId() == id).findFirst()
            .ifPresentOrElse(
                m -> System.out.println("  Current: " + TerminalUtils.GRAY + m.getContent() + TerminalUtils.RESET),
                () -> TerminalUtils.printError("Memory #" + id + " not found.")
            );

        System.out.print("  New content: ");
        String newContent = ctx.scanner().nextLine().trim();
        if (newContent.isEmpty()) { TerminalUtils.printInfo("Cancelled — content unchanged."); return; }

        boolean updated = ctx.memoryService().updateContent(ctx.userId(), id, newContent);
        if (updated) TerminalUtils.printSuccess("Memory #" + id + " updated.");
        else         TerminalUtils.printError("Could not update memory #" + id + ".");
    }

    private void exportVault() {
        TerminalUtils.printSeparator("EXPORT VAULT");
        List<Memory> all = ctx.memoryService().getAllMemories(ctx.userId());
        if (all.isEmpty()) { TerminalUtils.printInfo("Vault is empty. Nothing to export."); return; }
        
        System.out.print("  Enter filename (e.g. export.csv): ");
        String filename = ctx.scanner().nextLine().trim();
        if (filename.isEmpty()) filename = "nexus_memories.csv";
        if (!filename.endsWith(".csv")) filename += ".csv";
        
        TerminalUtils.spinner("Exporting " + all.size() + " memories...", 500);
        try {
            com.nexus.service.ExportService svc = new com.nexus.service.ExportService();
            String path = svc.exportToCsv(all, filename);
            TerminalUtils.printSuccess("Export complete! File saved to: " + path);
        } catch (Exception e) {
            TerminalUtils.printError("Export failed: " + e.getMessage());
        }
    }

    private String askScope() {
        String current = ctx.memoryService().currentWorkspaceScope();
        System.out.println("  Scope: 1=Current Project  2=Global  3=Custom");
        System.out.print("  Scope #: ");
        int choice = ctx.safeInt(ctx.scanner().nextLine());
        if (choice == 2) return com.nexus.service.MemoryService.GLOBAL_SCOPE;
        if (choice == 3) {
            System.out.print("  Custom scope label/path: ");
            String custom = ctx.scanner().nextLine().trim();
            return custom.isEmpty() ? current : custom.toLowerCase();
        }
        return current;
    }
}
