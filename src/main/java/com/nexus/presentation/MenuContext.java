package com.nexus.presentation;

import java.util.Scanner;

import com.nexus.dao.AuditLogDao;
import com.nexus.dao.LlmModelDao;
import com.nexus.dao.OutcomeMemoryDao;
import com.nexus.dao.SuitabilityDao;
import com.nexus.domain.TaskType;
import com.nexus.domain.User;
import com.nexus.exception.DaoException;
import com.nexus.exception.NexusException;
import com.nexus.service.ApiKeyService;
import com.nexus.service.LlmCallService;
import com.nexus.service.MemoryService;
import com.nexus.service.ProfileService;
import com.nexus.service.RoutingEngine;
import com.nexus.service.SessionService;
import com.nexus.service.UserService;
import com.nexus.util.TerminalUtils;

/**
 * Shared context object passed to all menu classes.
 * Avoids duplicating service/DAO references across every menu.
 * Demonstrates encapsulation — all fields are private with accessors.
 */
public class MenuContext {
    private final Scanner scanner;
    private final UserService userService;
    private final RoutingEngine routingEngine;
    private final MemoryService memoryService;
    private final ApiKeyService apiKeyService;
    private final LlmModelDao modelDao;
    private final SuitabilityDao suitabilityDao;
    private final OutcomeMemoryDao outcomeDao;
    private final AuditLogDao auditLogDao;
    private final LlmCallService llmCallService;
    private final SessionService sessionService;
    private final ProfileService profileService;
    private User loggedInUser;

    public MenuContext() {
        this.scanner        = new Scanner(System.in);
        this.userService    = new UserService();
        this.routingEngine  = new RoutingEngine();
        this.memoryService  = new MemoryService();
        this.apiKeyService  = new ApiKeyService();
        this.modelDao       = new LlmModelDao();
        this.suitabilityDao = new SuitabilityDao();
        this.outcomeDao     = new OutcomeMemoryDao();
        this.auditLogDao    = new AuditLogDao();
        this.llmCallService = new LlmCallService(apiKeyService);
        this.sessionService = new SessionService();
        this.profileService = new ProfileService();
    }

    // ── Accessors ────────────────────────────────────────────────────
    public Scanner scanner()            { return scanner; }
    public UserService userService()    { return userService; }
    public RoutingEngine routingEngine(){ return routingEngine; }
    public MemoryService memoryService(){ return memoryService; }
    public ApiKeyService apiKeyService(){ return apiKeyService; }
    public LlmModelDao modelDao()      { return modelDao; }
    public SuitabilityDao suitabilityDao() { return suitabilityDao; }
    public OutcomeMemoryDao outcomeDao(){ return outcomeDao; }
    public AuditLogDao auditLogDao()   { return auditLogDao; }
    public LlmCallService llmCallService() { return llmCallService; }
    public SessionService sessionService() { return sessionService; }
    public ProfileService profileService() { return profileService; }

    public User loggedInUser()         { return loggedInUser; }
    public void setLoggedInUser(User u){ this.loggedInUser = u; }
    public int userId()                { return loggedInUser.getId(); }
    public String username()           { return loggedInUser.getUsername(); }

    // ── Shared helpers ───────────────────────────────────────────────

    /** Safe integer parse — returns -1 on failure */
    public int safeInt(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return -1; }
    }

    /** Safe double parse — returns NaN on failure */
    public double safeDouble(String s) {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception e) { return Double.NaN; }
    }

    /** Reusable task-type picker */
    public TaskType pickTask() {
        TaskType[] tasks = TaskType.values();
        System.out.println();
        for (int i = 0; i < tasks.length; i++) {
            System.out.printf("  " + TerminalUtils.AMBER + "%-2d" + TerminalUtils.RESET + " %s%n", i + 1, tasks[i].name());
        }
        System.out.print("  Task (1-" + tasks.length + "): ");
        int idx = safeInt(scanner.nextLine()) - 1;
        if (idx < 0 || idx >= tasks.length) throw new NexusException("Invalid task selection (1-" + tasks.length + ").");
        return tasks[idx];
    }

    /**
     * Wraps menu actions to convert low-level DAO failures into clear user-facing guidance.
     */
    public void runWithDaoGuard(String failureMessage, Runnable action) {
        try {
            action.run();
        } catch (DaoException e) {
            TerminalUtils.printError(failureMessage);
        }
    }
}
