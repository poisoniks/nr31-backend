package org.nr31.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.nr31.backend.repository.FileMetadataRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BackgroundImageIdValidator implements ConstraintValidator<ValidBackgroundImageId, UUID> {

    private final FileMetadataRepository fileMetadataRepository;

    @Override
    public boolean isValid(UUID value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        boolean exists = fileMetadataRepository.existsById(value);
        if (!exists) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("cms_validation.hero.background_image_not_found")
                    .addConstraintViolation();
        }
        return exists;
    }
}
