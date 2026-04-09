package com.nexus.presentation;

import com.nexus.service.ArchitectureService;
import com.nexus.service.MarketIntelligenceService;
import com.nexus.service.SecuritySentinelService;
import com.nexus.util.TerminalUtils;

import java.util.List;

/**
 * The Advanced Intelligence Hub.
 * Exposes the 3 'God-Tier' features: Architectural Memory, Security Sentinel, and Web Grounding.
 */
public class IntelligenceMenu {
    private final MenuContext ctx;
    private final ArchitectureService archService;
    private final SecuritySentinelService securityService;
    private final MarketIntelligenceService marketService;

    public IntelligenceMenu(MenuContext ctx) {
        this.ctx = ctx;
        this.archService = new ArchitectureService();
        this.securityService = new SecuritySentinelService();
        this.marketService = new MarketIntelligenceService();
    }

    public void show() {
        while (true) {
            TerminalUtils.printHeader("NEXUS INTELLIGENCE HUB");
            System.out.println("  " + TerminalUtils.GOLD + "1" + TerminalUtils.RESET + "  Build Architectural Context Graph  " + TerminalUtils.GRAY + "(Project DNA)" + TerminalUtils.RESET);
            System.out.println("  " + TerminalUtils.GOLD + "2" + TerminalUtils.RESET + "  Run Proactive Security Audit       " + TerminalUtils.GRAY + "(Sentinel Scan)" + TerminalUtils.RESET);
            System.out.println("  " + TerminalUtils.GOLD + "3" + TerminalUtils.RESET + "  Sync Web-Market Intelligence       " + TerminalUtils.GRAY + "(Reality Check)" + TerminalUtils.RESET);
            System.out.println("  " + TerminalUtils.GOLD + "B" + TerminalUtils.RESET + "  Back to Dashboard");
            System.out.println();
            TerminalUtils.printPrompt(ctx.username());

            switch (ctx.scanner().nextLine().trim().toUpperCase()) {
                case "1" -> buildGraph();
                case "2" -> runSecurityAudit();
                case "3" -> syncMarket();
                case "B" -> { return; }
                default  -> TerminalUtils.printError("Unknown system command.");
            }
        }
    }

    private void buildGraph() {
        TerminalUtils.printSeparator("CONTEXTURE INDEXING");
        int count = archService.buildContextGraph(ctx.userId(), ".");
        TerminalUtils.printSuccess("Mapped DNA for " + count + " classes. Project architecture is now persisted in Memory Vault.");
    }

    private void runSecurityAudit() {
        TerminalUtils.printSeparator("SECURITY SENTINEL SCAN");
        List<SecuritySentinelService.SecurityFinding> findings = securityService.performFullAudit(ctx.userId(), ".");
        
        if (findings.isEmpty()) {
            TerminalUtils.printSuccess("No security risks detected. Environment is secure.");
        } else {
            String[] headers = {"Type", "File", "Severity"};
            String[][] rows = new String[findings.size()][3];
            for (int i = 0; i < findings.size(); i++) {
                var f = findings.get(i);
                rows[i] = new String[]{f.type(), f.file(), TerminalUtils.RED + f.severity() + TerminalUtils.RESET};
            }
            TerminalUtils.printTable(headers, rows);
            TerminalUtils.printWarn(findings.size() + " risks exposed. Nexus has logged these to the Audit Vault.");
        }
    }

    private void syncMarket() {
        TerminalUtils.printSeparator("WEB-MARKET GROUNDING");
        int updated = marketService.syncMarketRates();
        if (updated > 0) {
            TerminalUtils.printSuccess("Reality Checked! Updated " + updated + " models with live market pricing.");
        } else {
            TerminalUtils.printInfo("Local registry is already in sync with global market rates.");
        }
    }
}
