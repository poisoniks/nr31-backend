package org.nr31.backend.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nr31.backend.repository.FileMetadataRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackgroundImageIdValidatorTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private ConstraintValidatorContext context;

    private BackgroundImageIdValidator validator;

    @BeforeEach
    void setUp() {
        validator = new BackgroundImageIdValidator(fileMetadataRepository);
    }

    @Test
    void shouldReturnTrueForNullValue() {
        boolean result = validator.isValid(null, context);
        assertThat(result).isTrue();
        verifyNoInteractions(fileMetadataRepository);
    }

    @Test
    void shouldReturnTrueWhenFileMetadataExists() {
        UUID existingId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        when(fileMetadataRepository.existsById(existingId)).thenReturn(true);

        boolean result = validator.isValid(existingId, context);

        assertThat(result).isTrue();
        verify(fileMetadataRepository).existsById(existingId);
        verifyNoInteractions(context);
    }

    @Test
    void shouldReturnFalseWhenFileMetadataDoesNotExist() {
        UUID nonExistingId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        when(fileMetadataRepository.existsById(nonExistingId)).thenReturn(false);

        boolean result = validator.isValid(nonExistingId, context);

        assertThat(result).isFalse();
        verify(fileMetadataRepository).existsById(nonExistingId);
        verifyNoInteractions(context);
    }
}
