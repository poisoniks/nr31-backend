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

@Component
@RequiredArgsConstructor
public class NewsFeedItemCountValidator implements ConstraintValidator<ValidNewsFeedItemCount, Integer> {

    private final AppConfigService appConfigService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        AppConfigDto config = appConfigService.getConfig("cms.newsfeed.max_items");
        int maxItems;

        try {
            JsonNode configValueNode = objectMapper.readTree(config.getConfigValue());
            maxItems = configValueNode.asInt();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse cms.newsfeed.max_items config", e);
        }

        if (value > maxItems) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("Item count exceeds maximum allowed of %d (actual: %d)", maxItems, value))
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
