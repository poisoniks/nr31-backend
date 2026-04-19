# Nr.31 FKR Backend — Project Overview

This is the backend service for the **31st Feldkanonenregiment (Nr.31 FKR)**, a regiment in Mount & Blade Warband: Napoleonic Wars. It provides the API, authentication, calendar, file management, roster, and Discord integration for the regiment's web platform.

## Tech Stack

- **Language:** Java 17
- **Framework:** Spring Boot 4.1.0-M1
- **Build:** Gradle
- **Database:** PostgreSQL (via Flyway migrations, Hibernate JPA)
- **Security:** Stateless JWT (JJWT 0.13.0) + Spring Security RBAC
- **Caching:** Caffeine (app-level) + Hibernate L2 JCache (query-level)
- **Discord:** JDA 6.3.1 (bidirectional calendar sync)
- **Recurring Events:** lib-recur (RRULE support)
- **API Docs:** OpenAPI 3 / Swagger UI (disabled in prod)
- **Testing:** Cucumber BDD + TestContainers + JUnit Platform

## Package Structure

```
org.nr31.backend
├── annotation/       # @FeatureSwitch
├── config/           # Spring config (Security, Flyway, Jackson, OpenAPI, WebMvc, Tomcat)
├── controller/v1/    # REST controllers (Auth, Calendar, Roster, Files, Admin, Public)
├── dto/              # Request/response DTOs
├── exception/        # GlobalExceptionHandler + custom exceptions
├── integration/
│   └── discord/      # JDA bot manager + CalendarUpdateDiscordListener
├── interceptor/      # FeatureSwitchInterceptor
├── model/            # JPA entities
├── repository/       # Spring Data JPA repositories
├── scheduled/        # Cron jobs (FileCleanupJob, TokenCleanupJob)
├── security/         # JwtUtil, JwtAuthenticationFilter, entry point
├── service/          # Business logic interfaces + impl/
├── util/             # Utility classes
└── validation/       # LocalizedStringsValidator
```

## Key Entities

| Entity | Table | Notes |
|--------|-------|-------|
| User | users | Roles via user_roles join |
| Role | roles | Permissions, file upload quota, localized name (JSONB) |
| Permission | permissions | Granular (e.g. `roster:write`, `file:upload`) |
| CalendarEvent | events | JSONB title/description, RRULE, Discord sync |
| EventException | event_exceptions | Single-occurrence overrides |
| EventType | event_types | Categorization |
| UnitType | unit_types | Organizational units |
| FileMetadata | file_metadata | Scope, quota tracking |
| MediaFolder | media_folders | Hierarchical folders |
| RefreshToken | refresh_tokens | JWT refresh lifecycle |
| AppConfig | app_config | Runtime key-value config with optional JSON schema |

## Environment Variables (required in prod)

| Variable | Purpose |
|----------|---------|
| SPRING_DATASOURCE_URL | PostgreSQL JDBC URL |
| SPRING_DATASOURCE_USERNAME | DB username |
| SPRING_DATASOURCE_PASSWORD | DB password |
| JWT_SECRET | Base64-encoded HMAC secret |
| JWT_EXPIRATION | Access token TTL (ms) |
| JWT_REFRESH_EXPIRATION | Refresh token TTL (ms) |
| APP_STORAGE_PATH | File upload directory |
| DISCORD_BOT_TOKEN | JDA bot token |

## Profiles

| Profile | Use |
|---------|-----|
| `prod` | Production — Swagger disabled, optimized logging |
| `local` | Development — SQL logging, debug level, local DB |
| `test` | Automated tests — TestContainers PostgreSQL |

## Running Locally

```powershell
# Start PostgreSQL
docker run --name nr31-postgres -e POSTGRES_DB=nr31db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=password -p 5432:5432 -d postgres

# Run app
$env:SPRING_PROFILES_ACTIVE="local"; ./gradlew bootRun
```

## Testing

```bash
# Unit tests
./gradlew test

# Cucumber E2E tests (requires Docker)
./gradlew cucumber
```

## Deployment

CI/CD via `.github/workflows/prod-deploy.yml`:
1. Run tests (unit + Cucumber)
2. Build & push Docker image to GitHub Container Registry
3. Deploy to production via SSH
