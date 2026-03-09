# 31st Feldkanonenregiment (Nr.31 FKR) - Backend Services

Welcome to the backend service for the 31st Feldkanonenregiment (Nr.31 FKR), a Ukrainian regiment in Mount & Blade Warband: Napoleonic Wars.

## Project Purpose
This project serves as the central hub and administrative backbone for the regiment's soldiers, officers, and recruits. It provides essential services such as:
- Access to the regimental roster
- An event calendar
- Management of training materials and administrative controls

## Technologies Used
The application is built on a modern Spring Boot stack. Core technologies include:
- **Framework:** Spring Boot (v4.1.0-M1) and Java 17
- **Database:** PostgreSQL with Hibernate ORM
- **Authentication:** Stateless JWT (JSON Web Tokens)
- **Caching:** Caffeine Cache (used with `@Cacheable` in services) and Hibernate L2 (Second-Level) Cache for optimized database queries
- **Documentation:** OpenAPI / Swagger UI
- **Deployment:** Docker
- **CI/CD:** GitHub Actions

## Features & APIs
- **Authentication API:** (`AuthController.java`) Manages secure user logins and issues stateless JWTs.
- **Calendar API:** (`CalendarController.java`) Provides endpoints to manage scheduled events.
- **Admin Control API:** (`AdminPanelController.java`) Secured endpoints for officers to manage configurations, caches, and administrative tasks.

## Security & Architecture
- **Deny by Default:** The application employs a strict "deny-by-default" security policy (`SecurityConfig.java`), ensuring endpoints must be actively whitelisted or role-restricted.
- **Exception Handling:** Centralized exception handling ensures consistent, clean error responses (`GlobalExceptionHandler.java`). 
- **Async Scheduled Jobs:** Background tasks automatically manage the system, such as `TokenCleanupJob.java` which prunes expired authentication tokens.
- **API Documentation:** Interactive Swagger UI and OpenAPI documentation are automatically generated and available when running the application on `local` profile.

## Database Migrations
We utilize **Flyway** for robust database schema versioning and initial data population. 
- Global migrations reside in `src/main/resources/db/migration`.
- Profile-specific local mock data resides in `src/main/resources/db/migrationlocal` and is only evaluated when running the application locally.

## Testing Strategy
- **End-to-End (E2E) Tests:** Comprehensive automated testing is implemented using Cucumber. You can execute these tests via:
  ```bash
  ./gradlew cucumber
  ```
  Results can be found in `build/reports/cucumber/cucumber-report.html`.
- **Unit Tests:** While end-to-end coverage exists, traditional unit tests are currently omitted but are **planned for future adoption**.

## Profiles & Configuration
The application leverages Spring Profiles to switch between environments seamlessly:
- **`prod`:** The primary production profile.
- **`local`:** Configured for local development, enabling SQL logging and loading mock test data from Flyway's `migrationlocal` directory.
- **`test`:** Dedicated isolated environment for the Cucumber automated test suite.

## Running Locally
To run the application on your machine, you must first have **Docker** installed. 

**1. Start the PostgreSQL Database**
Depending on your OS, use one of the following commands to spin up a PostgreSQL instance in Docker:

**Windows (PowerShell):**
```powershell
docker run --name nr31-postgres `
  -e POSTGRES_DB=nr31db `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=password `
  -p 5432:5432 `
  -d postgres
```

**Linux / macOS (Bash):**
```bash
docker run --name nr31-postgres \
  -e POSTGRES_DB=nr31db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  -d postgres
```

**2. Run the Application**
Ensure the `local` Spring profile is active when booting the backend API.
- **Via Gradle (Windows PowerShell):** 
  ```powershell
  $env:SPRING_PROFILES_ACTIVE="local"; ./gradlew bootRun
  ```
- **Via Gradle (Linux/macOS Bash):** 
  ```bash
  SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
  ```
- **Via IDE (IntelliJ / Eclipse):** Navigate to `Nr31BackendApplication.java`. Edit your Run/Debug Configuration to include `SPRING_PROFILES_ACTIVE=local` in your Environment Variables, then execute the `main` method.

## Deployment
Automated deployment is modeled via a GitHub Actions pipeline (`.github/workflows/prod-deploy.yml`). Pushes to the main branch trigger a workflow that builds the Docker image (`Dockerfile`), pushes it to the GitHub Container Registry, and securely deploys it to the production VPS via SSH. 

## License
This project is licensed under the [MIT License](LICENSE). Copyright (c) 2026 poisoniks.
