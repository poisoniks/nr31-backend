# Coding Standards

## OpenAPI Annotations

All REST controllers and DTOs must be thoroughly annotated with OpenAPI/Swagger annotations.

**Controllers:**
- Annotate each controller with `@Tag(name = "...", description = "...")`
- Annotate each endpoint with `@Operation(summary = "...", description = "...")`
- Document all possible responses with `@ApiResponse(responseCode = "...", description = "...")`
- Use `@Parameter` for path/query parameters where the name or description isn't self-evident

**DTOs:**
- Annotate the class with `@Schema(description = "...")`
- Annotate each field with `@Schema(description = "...", example = "...")`
- Mark optional fields with `@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)`

Example:
```java
@Tag(name = "Calendar", description = "Manage calendar events")
@RestController
@RequestMapping("/api/v1/calendar")
public class CalendarController {

    @Operation(summary = "Create a new event")
    @ApiResponse(responseCode = "201", description = "Event created")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @PostMapping
    public ResponseEntity<CalendarEventDTO> createEvent(@RequestBody CreateEventRequest request) { ... }
}

@Schema(description = "Request body for creating a calendar event")
public class CreateEventRequest {

    @Schema(description = "Localized event title keyed by locale code", example = "{\"en\": \"Battle\"}")
    private Map<String, String> title;
}
```

---

## Request Validation

All request DTOs must use Bean Validation annotations from `jakarta.validation`.

- Use `@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Max`, `@Pattern`, etc. as appropriate
- Controllers must declare `@Valid` on `@RequestBody` parameters to trigger validation
- Validation errors are handled globally by `GlobalExceptionHandler` and return a `ValidationErrorResponse`

Example:
```java
@NotBlank(message = "Username must not be blank")
@Size(min = 3, max = 50)
private String username;

// In controller:
public ResponseEntity<?> register(@Valid @RequestBody AuthRequest request) { ... }
```

---

## Layered Architecture

Strictly follow the controller → service → repository pattern:

- **Controllers** handle HTTP concerns only: request mapping, input validation delegation, response building. No business logic.
- **Services** contain all business logic. Defined as interfaces in `service/`, implemented in `service/impl/`. Inject repositories, not other controllers.
- **Repositories** are Spring Data JPA interfaces only. No business logic.
- Cross-cutting concerns (security checks, caching) belong in services or dedicated components, not controllers.
- Direct repository access from controllers is not allowed.

---

## Testing

For every new feature and bug fix, Cucumber BDD tests must be created.

- Feature files go in `src/test/resources/features/`
- Step definitions go in `src/test/java/org/nr31/backend/cucumber/steps/`
- Tests run against a real PostgreSQL instance via TestContainers
- Scenarios should cover the happy path and key error cases (e.g. 400, 401, 403, 404)
- Run with: `./gradlew cucumber`
