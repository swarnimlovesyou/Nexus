package com.nexus.presentation;

import com.nexus.domain.ApiKey;
import com.nexus.domain.Provider;
import com.nexus.util.TerminalUtils;

import java.util.List;

/**
 * API Key Vault menu — CRUD for API keys.
 * XOR encoding is obfuscation, not encryption — documented honestly.
 */
public class ApiKeyMenu {
    private final MenuContext ctx;

    public ApiKeyMenu(MenuContext ctx) { this.ctx = ctx; }

    public void show() {
        TerminalUtils.printHeader("API Key Vault");
        TerminalUtils.printInfo("Keys are XOR-encoded and stored locally. Never transmitted. Known limitation: not AES-encrypted.");
        System.out.println();
        System.out.println("  " + TerminalUtils.AMBER + "1" + TerminalUtils.RESET + "  Add new API key");
        System.out.println("  " + TerminalUtils.AMBER + "2" + TerminalUtils.RESET + "  View my keys");
        System.out.println("  " + TerminalUtils.AMBER + "3" + TerminalUtils.RESET + "  Delete a key");
        System.out.println("  " + TerminalUtils.AMBER + "B" + TerminalUtils.RESET + "  Back");
        System.out.println();
        TerminalUtils.printPrompt(ctx.username());
        switch (ctx.scanner().nextLine().trim().toUpperCase()) {
            case "1" -> addKey();
            case "2" -> viewKeys();
            case "3" -> deleteKey();
        }
    }

    private void addKey() {
        TerminalUtils.printSeparator("ADD API KEY");
        Provider[] providers = Provider.values();
        for (int i = 0; i < providers.length; i++)
            System.out.printf("  " + TerminalUtils.AMBER + "%d" + TerminalUtils.RESET + "  %-20s %s%n",
                i + 1, providers[i].getDisplayName(), TerminalUtils.GRAY + providers[i].getBaseUrl() + TerminalUtils.RESET);
        System.out.print("  Provider (1-" + providers.length + "): ");
        int pidx = ctx.safeInt(ctx.scanner().nextLine()) - 1;
        if (pidx < 0 || pidx >= providers.length) { TerminalUtils.printError("Invalid provider selection."); return; }
        Provider p = providers[pidx];
        System.out.print("  Alias (e.g. my-work-key): ");
        String alias = ctx.scanner().nextLine().trim();
        System.out.print("  API Key: ");
        String rawKey = ctx.scanner().nextLine().trim();
        if (rawKey.isEmpty()) { TerminalUtils.printError("API key cannot be empty."); return; }

        TerminalUtils.spinner("Encoding and storing key...", 500);
        ApiKey stored = ctx.apiKeyService().storeKey(ctx.userId(), p, alias, rawKey);
        TerminalUtils.printSuccess("Key stored: " + stored.getMaskedKey() + "  provider=" + p.getDisplayName());
    }

    public void viewKeys() {
        TerminalUtils.printSeparator("STORED API KEYS");
        List<ApiKey> keys = ctx.apiKeyService().listKeysForUser(ctx.userId());
        if (keys.isEmpty()) { TerminalUtils.printInfo("No keys stored yet."); return; }

        String[] headers = {"ID", "Provider", "Alias", "Masked Key", "Added"};
        String[][] rows  = new String[keys.size()][5];
        for (int i = 0; i < keys.size(); i++) {
            ApiKey k = keys.get(i);
            rows[i] = new String[]{
                String.valueOf(k.getId()),
                TerminalUtils.AMBER + k.getProvider().getDisplayName() + TerminalUtils.RESET,
                k.getAlias(),
                TerminalUtils.GOLD + k.getMaskedKey() + TerminalUtils.RESET,
                k.getCreatedAt() != null ? k.getCreatedAt().toLocalDate().toString() : "—"
            };
        }
        System.out.println();
        TerminalUtils.printTable(headers, rows);
    }

    private void deleteKey() {
        viewKeys();
        System.out.print("  Key ID to delete: ");
        int id = ctx.safeInt(ctx.scanner().nextLine());
        if (id <= 0) { TerminalUtils.printError("Invalid key ID."); return; }
        System.out.print("  Confirm? (yes/no): ");
        if ("yes".equalsIgnoreCase(ctx.scanner().nextLine().trim())) {
            ctx.apiKeyService().deleteKey(ctx.userId(), id);
            TerminalUtils.printSuccess("Key deleted.");
        }
    }
}
