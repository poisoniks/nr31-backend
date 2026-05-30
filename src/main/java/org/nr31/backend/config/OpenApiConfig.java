package org.nr31.backend.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.nr31.backend.dto.common.ErrorCode;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@SecurityScheme(
        name = "Bearer Authentication",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class OpenApiConfig {

    private static final Pattern AUTHORITY_PATTERN = Pattern.compile("hasAuthority\\('([^']+)'\\)");

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"));
    }

    @Bean
    public OperationCustomizer securityOperationCustomizer() {
        return (operation, handlerMethod) -> {
            PreAuthorize methodAnnotation = handlerMethod.getMethodAnnotation(PreAuthorize.class);
            PreAuthorize classAnnotation = handlerMethod.getBeanType().getAnnotation(PreAuthorize.class);

            if (methodAnnotation == null && classAnnotation == null) {
                return operation;
            }

            Set<String> requirements = new LinkedHashSet<>();
            if (classAnnotation != null) {
                requirements.add(parseSecurityExpression(classAnnotation.value()));
            }
            if (methodAnnotation != null) {
                requirements.add(parseSecurityExpression(methodAnnotation.value()));
            }

            String existingDescription = operation.getDescription() != null ? operation.getDescription() : "";
            StringBuilder newDescription = new StringBuilder(existingDescription);
            
            if (!existingDescription.isEmpty()) {
                newDescription.append("\n\n");
            }
            
            newDescription.append("Security requirement:\n");
            for (String req : requirements) {
                newDescription.append("- ").append(req).append("\n");
            }

            operation.setDescription(newDescription.toString().trim());
            return operation;
        };
    }

    private String parseSecurityExpression(String expression) {
        Matcher matcher = AUTHORITY_PATTERN.matcher(expression);
        if (matcher.matches()) {
            return "Must have authority " + matcher.group(1);
        }
        return "Has security restrictions";
    }

    @Bean
    public OpenApiCustomizer pageableCustomizer() {
        return openApi -> {
            Schema<?> pageableSchema = openApi.getComponents().getSchemas().get("Pageable");
            if (pageableSchema != null) {
                pageableSchema.setRequired(List.of("page", "size"));
            }

            Schema<?> pageMetadataSchema = openApi.getComponents().getSchemas().get("PageMetadata");
            if (pageMetadataSchema != null) {
                pageMetadataSchema.setRequired(List.of("size", "number", "totalElements", "totalPages"));
            }

            if (openApi.getComponents().getSchemas() != null) {
                openApi.getComponents().getSchemas().forEach((name, schema) -> {
                    if (name.startsWith("PagedModel")) {
                        schema.setRequired(List.of("content", "page"));
                    }
                });
            }
        };
    }

    @Bean
    public OpenApiCustomizer errorResponseCustomizer() {
        return openApi -> {
            if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
                openApi.getComponents().getSchemas().forEach((schemaName, schema) -> {
                    if (schemaName.endsWith("ErrorResponse") && schema.getProperties() != null) {
                        Schema<?> metadataSchema = (Schema<?>) schema.getProperties().get("metadata");
                        if (metadataSchema != null) {
                            metadataSchema.setDescription(buildErrorCodeMetadataDocs());
                        }
                    }
                });
            }
        };
    }

    private String buildErrorCodeMetadataDocs() {
        StringBuilder sb = new StringBuilder();
        sb.append("Standardized error codes for the application:\n\n");
        for (ErrorCode code : ErrorCode.values()) {
            sb.append("* `").append(code.name()).append("`: ").append(code.getDescription()).append("\n");
        }
        return sb.toString();
    }
}
