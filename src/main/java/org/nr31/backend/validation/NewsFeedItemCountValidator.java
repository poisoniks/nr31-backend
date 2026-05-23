package org.nr31.backend.validation;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.nr31.backend.model.AppConfigKey;
import org.nr31.backend.service.AppConfigService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NewsFeedItemCountValidator implements ConstraintValidator<ValidNewsFeedItemCount, Integer> {

    private final AppConfigService appConfigService;

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        JsonNode configValueNode = appConfigService.getConfig(AppConfigKey.CMS_NEWSFEED_MAX_ITEMS).getConfigValue();
        if (configValueNode == null || !configValueNode.isNumber()) {
            throw new RuntimeException("Unable to parse cms.newsfeed.max_items config: expected a number");
        }
        int maxItems = configValueNode.asInt();

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
