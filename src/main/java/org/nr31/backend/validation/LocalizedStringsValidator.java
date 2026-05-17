package org.nr31.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.nr31.backend.repository.SupportedLocaleRepository;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class LocalizedStringsValidator implements ConstraintValidator<ValidLocalizedString, Map<String, String>> {

    private final SupportedLocaleRepository supportedLocaleRepository;

    @Override
    public boolean isValid(Map<String, String> value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        Set<String> keys = value.keySet();
        long validCount = supportedLocaleRepository.countByCodeIn(keys);

        boolean isValid = validCount == keys.size();
        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("cms_validation.localized_string.invalid_locale_keys")
                    .addConstraintViolation();
        }
        return isValid;
    }
}
