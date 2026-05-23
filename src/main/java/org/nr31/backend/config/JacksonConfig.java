package org.nr31.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper legacyObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public tools.jackson.databind.module.SimpleModule legacyJsonNodeModule() {
        tools.jackson.databind.module.SimpleModule module = new tools.jackson.databind.module.SimpleModule("LegacyJsonNodeModule");

        module.addSerializer(com.fasterxml.jackson.databind.JsonNode.class, new tools.jackson.databind.ValueSerializer<com.fasterxml.jackson.databind.JsonNode>() {
            @Override
            public void serialize(com.fasterxml.jackson.databind.JsonNode value, tools.jackson.core.JsonGenerator gen, tools.jackson.databind.SerializationContext serializers) {
                if (value == null) {
                    gen.writeNull();
                } else {
                    gen.writeRawValue(value.toString());
                }
            }
        });

        module.addDeserializer(com.fasterxml.jackson.databind.JsonNode.class, new tools.jackson.databind.ValueDeserializer<com.fasterxml.jackson.databind.JsonNode>() {
            private final ObjectMapper legacyMapper = new ObjectMapper();

            @Override
            public com.fasterxml.jackson.databind.JsonNode deserialize(tools.jackson.core.JsonParser p, tools.jackson.databind.DeserializationContext ctxt) {
                tools.jackson.databind.JsonNode jackson3Node = ctxt.readTree(p);
                if (jackson3Node == null) {
                    return null;
                }
                try {
                    return legacyMapper.readTree(jackson3Node.toString());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to deserialize Jackson 2 JsonNode from Jackson 3 JsonNode", e);
                }
            }
        });

        return module;
    }
}
