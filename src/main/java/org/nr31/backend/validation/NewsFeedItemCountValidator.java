package org.nr31.backend.validation;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.nr31.backend.dto.AppConfigDto;
import org.nr31.backend.model.AppConfigKey;
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

        AppConfigDto config = appConfigService.getConfig(AppConfigKey.CMS_NEWSFEED_MAX_ITEMS);
        int maxItems;

        try {
            JsonNode configValueNode = objectMapper.readTree(config.getConfigValue());
            if (configValueNode.isNumber()) {
                maxItems = configValueNode.asInt();
            } else {
                throw new IllegalArgumentException("Config is not a number");
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse cms.newsfeed.max_items config", e);
        }

        if (value > maxItems) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("cms_validation.newsfeed.item_count_exceeded|max=%d|actual=%d", maxItems, value))
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
