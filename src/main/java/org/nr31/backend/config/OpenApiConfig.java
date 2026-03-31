package org.nr31.backend.config;

import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer pageableCustomizer() {
        return openApi -> {
            Schema pageableSchema = openApi.getComponents().getSchemas().get("Pageable");
            if (pageableSchema != null) {
                pageableSchema.setRequired(List.of("page", "size"));
            }

            Schema pageMetadataSchema = openApi.getComponents().getSchemas().get("PageMetadata");
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
}
