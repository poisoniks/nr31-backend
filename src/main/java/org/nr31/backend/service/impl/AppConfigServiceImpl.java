package org.nr31.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.Error;
import lombok.RequiredArgsConstructor;
import org.nr31.backend.dto.AppConfigDto;
import org.nr31.backend.dto.ErrorCode;
import org.nr31.backend.exception.AppConfigException;
import org.nr31.backend.exception.AppConfigValidationException;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.model.AppConfig;
import org.nr31.backend.model.AppConfigKey;
import org.nr31.backend.repository.AppConfigRepository;
import org.nr31.backend.service.AppConfigService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppConfigServiceImpl implements AppConfigService {

    private final AppConfigRepository appConfigRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<AppConfigDto> getAllConfigs(Pageable pageable) {
        return appConfigRepository.findAll(pageable)
                .map(appConfig -> AppConfigDto.builder()
                        .name(appConfig.getConfigKey())
                        .description(appConfig.getDescription())
                        .configValue(appConfig.getConfigValue().toString())
                        .configSchema(appConfig.getConfigSchema() != null ? appConfig.getConfigSchema().toString() : null)
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "appConfig", key = "#name")
    public AppConfigDto getConfig(String name) {
        AppConfig appConfig = appConfigRepository.findByConfigKey(name)
                .orElseThrow(() -> new ElementNotFoundException("Config not found", ErrorCode.CONFIG_NOT_FOUND, Map.of("name", name)));

        return AppConfigDto.builder()
                .name(appConfig.getConfigKey())
                .description(appConfig.getDescription())
                .configValue(appConfig.getConfigValue().toString())
                .configSchema(appConfig.getConfigSchema().toString())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "appConfig", key = "#key.key")
    public AppConfigDto getConfig(AppConfigKey key) {
        AppConfig appConfig = appConfigRepository.findByConfigKey(key.getKey())
                .orElseThrow(() -> new ElementNotFoundException("Config not found", ErrorCode.CONFIG_NOT_FOUND, Map.of("name", key.getKey())));

        return AppConfigDto.builder()
                .name(appConfig.getConfigKey())
                .description(appConfig.getDescription())
                .configValue(appConfig.getConfigValue().toString())
                .configSchema(appConfig.getConfigSchema().toString())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = "appConfig", key = "#appConfigDto.name")
    public AppConfigDto updateConfig(String name, AppConfigDto appConfigDto) {
        AppConfig appConfig = appConfigRepository.findByConfigKey(name)
                .orElseThrow(() -> new ElementNotFoundException("Config not found", ErrorCode.CONFIG_NOT_FOUND, Map.of("name", name)));

        JsonNode schemaNode = appConfig.getConfigSchema();
        JsonNode valueNode;
        try {
            valueNode = objectMapper.readTree(appConfigDto.getConfigValue());
        } catch (JsonProcessingException e) {
            throw new AppConfigException("Invalid app config value");
        }

        validateJson(valueNode, schemaNode);

        appConfig.setDescription(appConfigDto.getDescription());
        appConfig.setConfigValue(valueNode);
        appConfig.setConfigSchema(schemaNode);

        AppConfig saved = appConfigRepository.save(appConfig);
        return AppConfigDto.builder()
                .name(saved.getConfigKey())
                .description(saved.getDescription())
                .configValue(valueNode.toString())
                .configSchema(schemaNode.toString())
                .build();
    }

    private void validateJson(JsonNode jsonNode, JsonNode schemaNode) {
        if (schemaNode == null || schemaNode.isNull()) {
            throw new AppConfigValidationException("Json schema can not be null", Map.of("jsonSchema", "Schema can not be null"));
        }

        SchemaRegistryConfig config = SchemaRegistryConfig.builder()
                .locale(LocaleContextHolder.getLocale())
                .build();

        SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(
                SpecificationVersion.DRAFT_7,
                builder -> builder.schemaRegistryConfig(config));

        Schema schema = schemaRegistry.getSchema(schemaNode);

        List<Error> validationResult = schema.validate(jsonNode);

        if (!validationResult.isEmpty()) {
            Map<String, String> errors = validationResult.stream()
                    .collect(Collectors.toMap(
                            error -> error.getInstanceNode().toString(),
                            Error::getMessage,
                            (existingMsg, newMsg) -> existingMsg + "; " + newMsg,
                            LinkedHashMap::new));

            throw new AppConfigValidationException("JSON schema validation failed.", errors);
        }
    }
}
