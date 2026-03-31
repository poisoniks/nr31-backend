# 31st Feldkanonenregiment (Nr.31 FKR) - Backend Services

Welcome to the backend service for the 31st Feldkanonenregiment (Nr.31 FKR), a Ukrainian regiment in Mount & Blade Warband: Napoleonic Wars.

## Project Purpose
This project serves as the central hub and administrative backbone for the regiment's soldiers, officers, and recruits. It provides essential services such as:
- **Regimental Roster:** Centralized management of active members, unit types, and event types.
- **File Management:** Scalable storage for regimental assets with quota control and automated cleanup.
- **Authentication Services:** Secure JWT-based access with token refreshing and session management.
- **Event Calendar:** A scheduling system supporting recursive events, exceptions, and integration with third-party platforms.
- **Administrative Controls:** Tools for officers to manage application state and configurations.

## Technologies Used
The application is built on the Spring Boot stack:
- **Backend:** Spring Boot (v4.1.0-M1) / Java 17
- **Database:** PostgreSQL for persistent storage.
- **ORM & Migrations:** Hibernate with Flyway for versioned schema management.
- **Security:** Stateless JWT-based authentication with role-based access control (RBAC).
- **Caching:** 
  - **Caffeine Cache:** In-memory application-level caching for calendar data.
  - **Hibernate L2 Cache:** SQL result caching for optimized database access.
- **Integrations:** JDA (Java Discord API) for guild event synchronization.
- **Documentation:** OpenAPI/Swagger UI.
- **Scheduled Tasks:** Automated system maintenance (e.g., file cleanup, token rotation).
- **CI/CD & Deployment:** Dockerized containers deployed via GitHub Actions.

## Core Features

### 1. Calendar API
The core of the application is a comprehensive calendar management system.
- **Recursive Events:** Supports complex recurring event logic (RRULE).
- **Exception Management:** Ability to handle single-occurrence exceptions, including rescheduling and cancellations.
- **Data Consistency:** Synchronized state across internal storage and integrated platforms.

### 2. Discord Synchronization
Provides an integration bridge with Discord Scheduled Guild Events.
- **Automated Update:** `CalendarUpdateDiscordListener` monitors Discord event lifecycle (creation, updates, deletions) and reflects changes in the application calendar.
- **Bidirectional Logic:** Handles event exceptions generated on Discord to ensure parity with the backend data model.
- **Startup Reconciliation:** Performs a full sync upon service initialization to resolve discrepancies.

### 3. Application Configuration
Managed via `AppConfig.java`, allowing for runtime settings adjustments.
- **Key-Value Store:** Database-backed JSON structure for configuration values.
- **Schema Validation:** Optional JSON schema enforcement for configuration updates.
- **Usage:** Management of integration parameters, feature toggles, and system thresholds.

### 4. Admin Panel
Located at `/api/v1/admin`, providing authorized users with system control:
- **Cache Management:** Eviction of application and Hibernate second-level caches.
- **Log Access:** Listing and retrieval of application log files from the `/app/logs` directory.
- **Integration Control:** Programmatic control of the Discord bot status (start/stop/status).
- **Config Management:** Endpoints for viewing and updating dynamic application settings.

### 5. File Management System
A storage solution for regimental assets.
- **Validated Uploads:** Supports image types (PNG, JPEG, WEBP) with strict 5MB size limits.
- **Quota Control:** Role-based file upload limits (configurable via Patch endpoints).
- **Lifecycle Management:** Automatic cleanup of stale or unlinked files via the `FileCleanupJob`.

### 6. Roster & Metadata Management
Centralized control over regimental organizational structure.
- **Unit & Event Types:** Full CRUD operations for categorizing regimental activities and organizational units.
- **Public Exposure:** Optimized read access for UI components with Hibernate L2 caching.

### 7. Authentication & Security
Industry-standard security implementation.
- **JWT Lifecycle:** Implementation of Access and Refresh token pairs.
- **Secure Handshakes:** Dedicated endpoints for login, logout, and token refreshing.
- **Granular Permissions:** Expanded authority model (e.g., `roster:write`, `file:upload`).
- **Deny-by-Default:** Security policy ensuring all endpoints require explicit authorization or whitelisting.
- **Standardized Error Handling:** Consistent JSON responses for all error states via a global exception handler.

## Testing Strategy
The project follows a tiered testing approach:
- **End-to-End (E2E):** Cucumber-based integration tests for full API flows (Calendar, Auth, Roster). Run via `./gradlew cucumber`.
- **Unit Testing:** Focus on component-level logic, such as event synchronization and configuration parsing (e.g., `CalendarUpdateDiscordListenerTest.java`).
- **Integration Tests:** Verification of database migrations and service-layer interactions.

## Profiles & Environment Management
- **`prod`:** Production settings and performance optimizations.
- **`local`:** Development environment with SQL logging and mock data.
- **`test`:** Isolated environment for automated test suites.

## Getting Started

### Prerequisites
- Java 17+
- Docker & Docker Compose

### 1. Start Infrastructure
```powershell
docker run --name nr31-postgres `
  -e POSTGRES_DB=nr31db `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=password `
  -p 5432:5432 `
  -d postgres
```

### 2. Run Application
```powershell
$env:SPRING_PROFILES_ACTIVE="local"; ./gradlew bootRun
```

## Deployment
Managed via `.github/workflows/prod-deploy.yml`. The pipeline includes:
1. Automated test execution (Cucumber and Unit tests).
2. Docker image build and push to GitHub Container Registry.
3. Production deployment via SSH.

## License
Licensed under the [MIT License](LICENSE). Copyright (c) 2026 poisoniks.
