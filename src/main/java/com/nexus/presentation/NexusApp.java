package com.nexus.presentation;

import com.nexus.dao.LlmModelDao;
import com.nexus.dao.OutcomeMemoryDao;
import com.nexus.dao.SuitabilityDao;
import com.nexus.domain.LlmModel;
import com.nexus.domain.ModelSuitability;
import com.nexus.domain.OutcomeMemory;
import com.nexus.domain.TaskType;
import com.nexus.domain.User;
import com.nexus.service.RoutingEngine;
import com.nexus.service.UserService;

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
        app.start();
    }

    private void seedInitialData() {
        // Seed an admin if none exists
        if (userService.getAllUsers().isEmpty()) {
            userService.registerUser("admin", "admin123", "ADMIN");
        }
        
        // Seed models if empty
        if (modelDao.findAll().isEmpty()) {
            LlmModel gpt4 = new LlmModel(null, "GPT-4o", "OpenAI", 0.05, null);
            LlmModel claude = new LlmModel(null, "Claude-3.5-Sonnet", "Anthropic", 0.03, null);
            modelDao.create(gpt4);
            modelDao.create(claude);
            
            // Explicit initial suitability
            suitabilityDao.create(new ModelSuitability(null, gpt4.getId(), TaskType.CODE_GENERATION, 0.9, null));
            suitabilityDao.create(new ModelSuitability(null, claude.getId(), TaskType.CODE_GENERATION, 0.85, null));
            suitabilityDao.create(new ModelSuitability(null, claude.getId(), TaskType.CREATIVE_WRITING, 0.95, null));
        }
    }

    public void start() {
        System.out.println("=== Welcome to Nexus Autopilot ===");
        
        while (loggedInUser == null) {
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("Choose option: ");
            String opt = scanner.nextLine();
            
            if (opt.equals("1")) loginMenu();
            else if (opt.equals("2")) registerMenu();
            else if (opt.equals("3")) {
                System.out.println("Exiting...");
                return;
            }
        }
        
        mainMenu();
    }

    private void loginMenu() {
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        
        loggedInUser = userService.authenticate(username, password);
        if (loggedInUser == null) {
            System.out.println("Invalid credentials.");
        } else {
            System.out.println("Logged in successfully as " + loggedInUser.getEntityDisplayName());
        }
    }

    private void registerMenu() {
        System.out.print("New Username: ");
        String username = scanner.nextLine();
        System.out.print("New Password: ");
        String password = scanner.nextLine();
        try {
            userService.registerUser(username, password, "USER");
            System.out.println("Registration successful! You can now login.");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void mainMenu() {
        while (true) {
            System.out.println("\n=== Main Menu ===");
            System.out.println("1. View Available Models");
            System.out.println("2. Manage Users (Admin)");
            System.out.println("3. Simulate LLM Request (Router Engine)");
            System.out.println("4. View Request History");
            System.out.println("5. Logout");
            System.out.print("Choose option: ");
            String opt = scanner.nextLine();

            switch (opt) {
                case "1": viewModels(); break;
                case "2": manageUsers(); break;
                case "3": simulateRequest(); break;
                case "4": viewHistory(); break;
                case "5": 
                    loggedInUser = null; 
                    start(); 
                    return;
                default: System.out.println("Invalid option");
            }
        }
    }

    private void viewModels() {
        System.out.println("\n--- Registered Models ---");
        for (LlmModel config : modelDao.findAll()) {
            System.out.println(config.getEntityDisplayName() + " - $" + config.getCostPer1kTokens());
        }
    }

    private void manageUsers() {
        if (!"ADMIN".equals(loggedInUser.getRole())) {
            System.out.println("Access denied. Admins only.");
            return;
        }
        System.out.println("\n--- Users ---");
        for (User u : userService.getAllUsers()) {
            System.out.println(u.toString());
        }
    }

    private void simulateRequest() {
        System.out.println("\n--- Simulate LLM Routing Request ---");
        System.out.println("Available Tasks:");
        TaskType[] tasks = TaskType.values();
        for (int i = 0; i < tasks.length; i++) {
            System.out.println((i + 1) + ". " + tasks[i].name());
        }
        System.out.print("Select Task Type (1-" + tasks.length + "): ");
        int taskIdx = Integer.parseInt(scanner.nextLine()) - 1;
        TaskType selectedTask = tasks[taskIdx];

        System.out.println("Calling Router Intelligence...");
        try {
            LlmModel bestModel = routingEngine.selectOptimalModel(selectedTask);
            if (bestModel == null) {
                System.out.println("Failed to route to a proper model.");
                return;
            }
            System.out.println("Router explicitly chose: " + bestModel.getEntityDisplayName());
            System.out.println("Providing dummy response...");
            
            System.out.print("Enter quality score for this model's response (0.0 to 1.0): ");
            double quality = Double.parseDouble(scanner.nextLine());
            
            OutcomeMemory memory = new OutcomeMemory(null, loggedInUser.getId(), bestModel.getId(), 
                    selectedTask, bestModel.getCostPer1kTokens() * 0.2, 850, quality, null);
            
            outcomeDao.create(memory);
            System.out.println("Outcome Memory committed. Intelligence updated for next call.");
            
        } catch (Exception e) {
            System.out.println("Routing fault: " + e.getMessage());
        }
    }

    private void viewHistory() {
        System.out.println("\n--- Outcome History ---");
        List<OutcomeMemory> memories = outcomeDao.findAll();
        for (OutcomeMemory mem : memories) {
            String modelName = modelDao.read(mem.getModelId()).map(LlmModel::getName).orElse("Unknown");
            System.out.println("Task: " + mem.getTaskType() + " | Model: " + modelName 
                    + " | Quality: " + mem.getQualityScore());
        }
    }
}
