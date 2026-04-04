# Nexus Autopilot v1.0.0

**Autopilot for LLM usage — observes everything, remembers everything, optimizes over time.**

Nexus is an intelligent routing and observability system built with Java. It helps developers reduce LLM costs and increase response quality by learning from historical execution performance.

## Key Features
- **Intelligent Routing**: Automatically selects the best model (GPT-4, Claude, etc.) based on task type and performance history.
- **Claude Code-Inspired Terminal**: Premium CLI experience with ANSI colors, boxes, and ASCII banners.
- **Role-Based Access Control (RBAC)**: Secure authentication with password hashing and specific permissions for Admins and Users.
- **Observability Dashboard**: Search and filter execution history to understand model performance.
- **Financial Intelligence**: Generate token spend reports and find cost-saving opportunities.

## Technical Architecture
Built with a multi-layered Java architecture:
1. **Presentation Layer**: `TerminalUtils` & `NexusApp`
2. **Service Layer**: `RoutingEngine` (with HashMap-based optimization) & `UserService`
3. **Data Access Layer**: JDBC-based DAOs with Search/Filter capabilities.
4. **Domain Layer**: Rich OOP hierarchy with inheritance (`BaseEntity` -> `User` -> `AdminUser`) and interfaces (`Auditable`).

## Build & Run
### Requirements
- Java 17+
- Maven

### Execution
```bash
mvn clean package
java -jar target/nexus-autopilot-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Default Credentials
- **Admin**: `admin` / `admin123`
