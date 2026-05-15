package org.nr31.backend.service.impl;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import org.nr31.backend.model.SupportedLocale;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.ArrayNode;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonSchemaModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;
import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.dto.ErrorCode;
import org.nr31.backend.dto.cms.WidgetDto;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.repository.SupportedLocaleRepository;
import org.nr31.backend.service.WidgetSchemaService;
import org.nr31.backend.annotation.ImageField;
import org.nr31.backend.annotation.LocalizedField;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WidgetSchemaServiceImpl implements WidgetSchemaService {

    private final SupportedLocaleRepository supportedLocaleRepository;
    private final Map<String, Class<? extends WidgetDto>> typeMap;
    private final Map<String, JsonNode> schemaCache = new ConcurrentHashMap<>();
    private final SchemaGenerator generator;

    @Autowired
    public WidgetSchemaServiceImpl(SupportedLocaleRepository supportedLocaleRepository) {
        this.supportedLocaleRepository = supportedLocaleRepository;
        this.typeMap = Collections.unmodifiableMap(discoverWidgetTypes());

        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
                SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON);

        configBuilder.with(new JacksonSchemaModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED));
        configBuilder.with(new JakartaValidationModule(
                JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED,
                JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS));

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
                        Map.of("type", t));
            }
            log.debug("Generating JSON schema for widget type: {}", t);
            JsonNode schema = generator.generateSchema(clazz);
            return enhanceSchemaWithCustomMetadata(schema, clazz);
        });
    }

    private JsonNode enhanceSchemaWithCustomMetadata(JsonNode schema, Class<? extends WidgetDto> clazz) {
        if (!schema.isObject()) {
            return schema;
        }

        ObjectNode schemaObj = (ObjectNode) schema;
        JsonNode properties = schemaObj.get("properties");

        if (properties == null || !properties.isObject()) {
            return schema;
        }

        ObjectNode propertiesObj = (ObjectNode) properties;
        List<String> supportedLocales = getSupportedLocaleCodes();

        for (Field field : getAllFields(clazz)) {
            String fieldName = field.getName();
            JsonNode fieldSchema = propertiesObj.get(fieldName);

            if (fieldSchema != null && fieldSchema.isObject()) {
                ObjectNode fieldSchemaObj = (ObjectNode) fieldSchema;

                if (field.isAnnotationPresent(LocalizedField.class)) {
                    fieldSchemaObj.put("x-localized", true);
                    fieldSchemaObj.put("x-widget", "localized-text-input");
                    ArrayNode localesArray = fieldSchemaObj.putArray("x-supported-locales");
                    supportedLocales.forEach(localesArray::add);
                    log.trace("Enhanced field '{}' with localization metadata", fieldName);
                }

                if (field.isAnnotationPresent(ImageField.class)) {
                    fieldSchemaObj.put("x-image", true);
                    fieldSchemaObj.put("x-widget", "image-picker");
                    log.trace("Enhanced field '{}' with image metadata", fieldName);
                }
            }
        }

        return schemaObj;
    }

    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;

        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }

        return fields;
    }

    private List<String> getSupportedLocaleCodes() {
        return supportedLocaleRepository.findAll().stream()
                .map(SupportedLocale::getCode)
                .sorted()
                .collect(Collectors.toList());
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
