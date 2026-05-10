package org.nr31.backend.service.impl;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import tools.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonSchemaModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;
import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.dto.ErrorCode;
import org.nr31.backend.dto.cms.WidgetDto;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.service.WidgetSchemaService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WidgetSchemaServiceImpl implements WidgetSchemaService {

    private final Map<String, Class<? extends WidgetDto>> typeMap;
    private final Map<String, JsonNode> schemaCache = new ConcurrentHashMap<>();
    private final SchemaGenerator generator;

    public WidgetSchemaServiceImpl() {
        this.typeMap = Collections.unmodifiableMap(discoverWidgetTypes());
        
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
                SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON
        );

        configBuilder.with(new JacksonSchemaModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED));
        configBuilder.with(new JakartaValidationModule(
                JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED,
                JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS
        ));

        SchemaGeneratorConfig config = configBuilder.build();
        this.generator = new SchemaGenerator(config);
        
        log.info("Initialized WidgetSchemaService with {} types: {}", typeMap.size(), typeMap.keySet());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Class<? extends WidgetDto>> discoverWidgetTypes() {
        Map<String, Class<? extends WidgetDto>> map = new HashMap<>();
        JsonSubTypes subTypes = WidgetDto.class.getAnnotation(JsonSubTypes.class);
        if (subTypes != null) {
            for (JsonSubTypes.Type type : subTypes.value()) {
                if (WidgetDto.class.isAssignableFrom(type.value())) {
                    map.put(type.name(), (Class<? extends WidgetDto>) type.value());
                }
            }
        }
        return map;
    }

    @Override
    public JsonNode getSchema(String type) {
        return schemaCache.computeIfAbsent(type, t -> {
            Class<? extends WidgetDto> clazz = typeMap.get(t);
            if (clazz == null) {
                throw new ElementNotFoundException(
                        "Unknown widget type: " + t,
                        ErrorCode.ELEMENT_NOT_FOUND,
                        Map.of("type", t)
                );
            }
            log.debug("Generating JSON schema for widget type: {}", t);
            return generator.generateSchema(clazz);
        });
    }

    @Override
    public Map<String, JsonNode> getAllSchemas() {
        return typeMap.keySet().stream()
                .collect(Collectors.toMap(type -> type, this::getSchema));
    }

    @Override
    public Set<String> getWidgetTypes() {
        return typeMap.keySet();
    }
}
