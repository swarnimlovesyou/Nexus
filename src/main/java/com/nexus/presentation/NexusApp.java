package com.nexus.presentation;

import com.nexus.dao.LlmModelDao;
import com.nexus.dao.OutcomeMemoryDao;
import com.nexus.dao.SuitabilityDao;
import com.nexus.domain.LlmModel;
import com.nexus.domain.ModelSuitability;
import com.nexus.domain.OutcomeMemory;
import com.nexus.domain.TaskType;
import com.nexus.domain.User;
import com.nexus.exception.NexusException;
import com.nexus.service.RoutingEngine;
import com.nexus.service.UserService;
import com.nexus.util.TerminalUtils;

import java.util.List;
import java.util.Scanner;

public class NexusApp {
    private final Scanner scanner;
    private final UserService userService;
    private final RoutingEngine routingEngine;
    private final LlmModelDao modelDao;
    private final SuitabilityDao suitabilityDao;
    private final OutcomeMemoryDao outcomeDao;

    private User loggedInUser = null;

    public NexusApp() {
        this.scanner = new Scanner(System.in);
        this.userService = new UserService();
        this.routingEngine = new RoutingEngine();
        this.modelDao = new LlmModelDao();
        this.suitabilityDao = new SuitabilityDao();
        this.outcomeDao = new OutcomeMemoryDao();
    }

    public static void main(String[] args) {
        NexusApp app = new NexusApp();
        app.seedInitialData();
        app.run();
    }

    private void seedInitialData() {
        if (userService.getAllUsers().isEmpty()) {
            userService.registerUser("admin", "admin123", "ADMIN");
        }
        
        if (modelDao.findAll().isEmpty()) {
            LlmModel gpt4 = new LlmModel(null, "GPT-4o", "OpenAI", 0.05, null);
            LlmModel claude = new LlmModel(null, "Claude-3.5-Sonnet", "Anthropic", 0.03, null);
            modelDao.create(gpt4);
            modelDao.create(claude);
            
            suitabilityDao.create(new ModelSuitability(null, gpt4.getId(), TaskType.CODE_GENERATION, 0.9, null));
            suitabilityDao.create(new ModelSuitability(null, claude.getId(), TaskType.CODE_GENERATION, 0.85, null));
            suitabilityDao.create(new ModelSuitability(null, claude.getId(), TaskType.CREATIVE_WRITING, 0.95, null));
        }
    }

    public void run() {
        TerminalUtils.clearScreen();
        TerminalUtils.printBanner();
        
        while (true) {
            try {
                if (!scanner.hasNextLine()) {
                    return; // Prevent crash when no input is available (e.g. non-interactive shells)
                }
                if (loggedInUser == null) {
                    showAuthMenu();
                } else {
                    showMainMenu();
                }
            } catch (NexusException e) {
                TerminalUtils.printError(e.getMessage());
            } catch (Exception e) {
                TerminalUtils.printError("Unexpected error: " + e.getMessage());
            }
        }
    }

    private void showAuthMenu() {
        String content = "1. Login to your account\n" +
                         "2. Register new developer\n" +
                         "3. Shutdown Nexus Autopilot";
        TerminalUtils.printClaudeStyleBox("AUTHENTICATION MODE", content);
        System.out.print(TerminalUtils.ORANGE + "nexus@auth ~ % " + TerminalUtils.RESET);
        String opt = scanner.nextLine();
        
        switch (opt) {
            case "1": login(); break;
            case "2": register(); break;
            case "3": 
                TerminalUtils.printInfo("Shutting down...");
                System.exit(0);
                break;
            default: TerminalUtils.printError("Invalid command");
        }
    }

    private void login() {
        TerminalUtils.printStep("Entering login sequence...");
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        
        loggedInUser = userService.authenticate(username, password);
        TerminalUtils.printSuccess("Access granted. Welcome back, " + loggedInUser.getUsername());
    }

    private void register() {
        TerminalUtils.printStep("Registering new developer...");
        System.out.print("Desired Username: ");
        String username = scanner.nextLine();
        System.out.print("Security Password: ");
        String password = scanner.nextLine();
        userService.registerUser(username, password, "USER");
        TerminalUtils.printSuccess("Registration complete. Please login.");
    }

    private void showMainMenu() {
        StringBuilder sb = new StringBuilder();
        sb.append("1. Simulate LLM Routing (Auto-optimize)\n");
        sb.append("2. Model Discovery (View & Search Models)\n");
        sb.append("3. Observability Dashboard (History & Filters)\n");
        sb.append("4. Financial Report (Cost Analysis)\n");
        if ("ADMIN".equals(loggedInUser.getRole())) {
            sb.append("5. System Administration (User Management)\n");
            sb.append("6. Model Registry Management (Add/Remove Models)\n");
        }
        sb.append("0. Logoff nexus session");

        TerminalUtils.printClaudeStyleBox("DASHBOARD: " + loggedInUser.getEntityDisplayName(), sb.toString());
        System.out.print(TerminalUtils.ORANGE + "nexus@" + loggedInUser.getUsername().toLowerCase() + " ~ % " + TerminalUtils.RESET);
        String opt = scanner.nextLine();

        switch (opt) {
            case "1": simulateRouting(); break;
            case "2": viewModels(); break;
            case "3": viewHistory(); break;
            case "4": financialReport(); break;
            case "5": manageUsers(); break;
            case "6": manageModels(); break;
            case "0": loggedInUser = null; break;
            default: TerminalUtils.printError("Unknown operation");
        }
    }

    private void simulateRouting() {
        TerminalUtils.printHeader("INTELLIGENT ROUTING ENGINE");
        TaskType[] tasks = TaskType.values();
        for (int i = 0; i < tasks.length; i++) {
            System.out.println((i + 1) + ". " + tasks[i].name());
        }
        System.out.print("Target Task Profile (1-" + tasks.length + "): ");
        int taskIdx = Integer.parseInt(scanner.nextLine()) - 1;
        TaskType selectedTask = tasks[taskIdx];

        TerminalUtils.printStep("Analyzing performance metrics for " + selectedTask + "...");
        LlmModel model = routingEngine.selectOptimalModel(selectedTask);
        
        TerminalUtils.printSuccess("Recommendation: " + model.getName() + " (" + model.getProvider() + ")");
        System.out.print("Accept and evaluate response (0.0 - 1.0 quality score): ");
        double quality = Double.parseDouble(scanner.nextLine());

        OutcomeMemory record = new OutcomeMemory(null, loggedInUser.getId(), model.getId(), 
                                                selectedTask, model.getCostPer1kTokens() * 0.15, 
                                                (int)(Math.random() * 500 + 300), quality, null);
        outcomeDao.create(record);
        TerminalUtils.printSuccess("Outcome committed to memory. System optimized.");
    }

    private void viewModels() {
        TerminalUtils.printHeader("MODEL DISCOVERY");
        System.out.println("1. List All Models");
        System.out.println("2. Filter by Provider");
        System.out.print("Option: ");
        String opt = scanner.nextLine();
        
        List<LlmModel> models;
        if ("2".equals(opt)) {
            System.out.print("Provider search query: ");
            models = modelDao.findByProvider(scanner.nextLine());
        } else {
            models = modelDao.findAll();
        }

        System.out.println("\nRESULTS:");
        for (LlmModel m : models) {
            System.out.printf("[%d] %-20s | Provider: %-15s | Cost/1k: $%.4f\n", m.getId(), m.getName(), m.getProvider(), m.getCostPer1kTokens());
        }
    }

    private void viewHistory() {
        TerminalUtils.printHeader("OBSERVABILITY SYSTEM");
        System.out.println("Filter Options: (1) All, (2) By Task, (3) By Model");
        System.out.print("Choice: ");
        String choice = scanner.nextLine();

        List<OutcomeMemory> history;
        if ("2".equals(choice)) {
            System.out.print("Task type name: ");
            history = outcomeDao.findByTaskType(TaskType.valueOf(scanner.nextLine().toUpperCase()));
        } else if ("3".equals(choice)) {
            System.out.print("Model ID: ");
            history = outcomeDao.findByModelId(Integer.parseInt(scanner.nextLine()));
        } else {
            history = outcomeDao.findAll();
        }

        System.out.println("\nEXECUTION TRACE HISTORY:");
        for (OutcomeMemory om : history) {
            System.out.println(om.getCreatedAt() + " | Task: " + om.getTaskType() + " | Score: " + om.getQualityScore());
        }
    }

    private void financialReport() {
        TerminalUtils.printHeader("FINANCIAL PERFORMANCE");
        List<OutcomeMemory> history = outcomeDao.findAll();
        double totalCost = 0;
        int totalRequests = history.size();
        for (OutcomeMemory om : history) totalCost += om.getCost();

        String report = String.format("Total Token Spend: $%.4f\nTotal Executions: %d\nAvg Cost / Call: $%.4f", 
                                     totalCost, totalRequests, (totalRequests == 0 ? 0 : totalCost/totalRequests));
        TerminalUtils.printClaudeStyleBox("CFO REPORT", report);
    }

    private void manageUsers() {
        if (!"ADMIN".equals(loggedInUser.getRole())) throw new NexusException("Access Denied");
        TerminalUtils.printHeader("USER ARCHIVE");
        System.out.println("1. List All Users");
        System.out.println("2. Search User by Username");
        System.out.println("3. Delete User (DANGER)");
        System.out.print("Action: ");
        String opt = scanner.nextLine();

        switch (opt) {
            case "1":
                for (User u : userService.getAllUsers()) System.out.println(u);
                break;
            case "2":
                System.out.print("Search term: ");
                for (User u : userService.searchUsers(scanner.nextLine())) System.out.println(u);
                break;
            case "3":
                System.out.print("User ID to purge: ");
                int id = Integer.parseInt(scanner.nextLine());
                userService.deleteUser(id);
                TerminalUtils.printSuccess("User wiped from vault.");
                break;
        }
    }

    private void manageModels() {
        if (!"ADMIN".equals(loggedInUser.getRole())) throw new NexusException("Access Denied");
        TerminalUtils.printHeader("MODEL REGISTRY");
        System.out.println("1. Register New Model");
        System.out.println("2. Retire Model (Delete)");
        System.out.print("Command: ");
        String opt = scanner.nextLine();

        if ("1".equals(opt)) {
            System.out.print("Model Name: ");
            String name = scanner.nextLine();
            System.out.print("Provider: ");
            String provider = scanner.nextLine();
            System.out.print("Cost per 1k: ");
            double cost = Double.parseDouble(scanner.nextLine());
            modelDao.create(new LlmModel(null, name, provider, cost, null));
            TerminalUtils.printSuccess("Model registered in autopilot fleet.");
        } else if ("2".equals(opt)) {
            System.out.print("Model ID to retire: ");
            modelDao.delete(Integer.parseInt(scanner.nextLine()));
            TerminalUtils.printSuccess("Model decommissioned.");
        }
    }
}
