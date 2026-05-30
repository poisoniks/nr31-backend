package org.nr31.backend.config;

import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonSchemaConfig {

    @Bean
    public SchemaRegistry schemaRegistry() {
        return SchemaRegistry.withDefaultDialect(
                SpecificationVersion.DRAFT_7,
                builder -> {}
        );
    }
}
