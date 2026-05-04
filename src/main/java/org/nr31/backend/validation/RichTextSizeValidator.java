package org.nr31.backend.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.nr31.backend.dto.AppConfigDto;
import org.nr31.backend.service.AppConfigService;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RichTextSizeValidator implements ConstraintValidator<ValidRichTextSize, Map<String, JsonNode>> {

    private final AppConfigService appConfigService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean isValid(Map<String, JsonNode> value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        AppConfigDto config = appConfigService.getConfig("cms.richtext.max_size_bytes");
        long maxSizeBytes;
        
        try {
            JsonNode configValueNode = objectMapper.readTree(config.getConfigValue());
            maxSizeBytes = configValueNode.asLong();
        } catch (JsonProcessingException e) {
            // If config is invalid, use default of 1MB
            maxSizeBytes = 1048576L;
        }

        // Check each locale's JsonNode serialized size
        for (Map.Entry<String, JsonNode> entry : value.entrySet()) {
            String locale = entry.getKey();
            JsonNode content = entry.getValue();
            
            try {
                String serialized = objectMapper.writeValueAsString(content);
                long sizeBytes = serialized.getBytes(StandardCharsets.UTF_8).length;
                
                if (sizeBytes > maxSizeBytes) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(
                        String.format("Rich text content for locale '%s' exceeds maximum allowed size of %d bytes (actual: %d bytes)", 
                            locale, maxSizeBytes, sizeBytes)
                    ).addConstraintViolation();
                    return false;
                }
            } catch (JsonProcessingException e) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    String.format("Failed to serialize rich text content for locale '%s'", locale)
                ).addConstraintViolation();
                return false;
            }
        }

        return true;
    }
}
