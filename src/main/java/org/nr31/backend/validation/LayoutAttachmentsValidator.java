package org.nr31.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.nr31.backend.dto.cms.LayoutDataDto;
import org.nr31.backend.dto.cms.SlotDto;
import org.nr31.backend.dto.cms.WidgetDto;
import org.nr31.backend.dto.cms.RichTextWidgetDto;
import org.nr31.backend.model.FileMetadata;
import org.nr31.backend.model.FileScope;
import org.nr31.backend.repository.FileMetadataRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LayoutAttachmentsValidator implements ConstraintValidator<ValidAttachments, LayoutDataDto> {

    private final FileMetadataRepository fileMetadataRepository;

    @Override
    public boolean isValid(LayoutDataDto layoutData, ConstraintValidatorContext context) {
        if (layoutData == null || layoutData.getSlots() == null) {
            return true;
        }

        Set<UUID> attachmentIds = new HashSet<>();
        for (SlotDto slot : layoutData.getSlots()) {
            if (slot.getWidgets() != null) {
                for (WidgetDto widget : slot.getWidgets()) {
                    if (widget instanceof RichTextWidgetDto) {
                        attachmentIds.addAll(((RichTextWidgetDto) widget).extractAttachmentIds());
                    }
                }
            }
        }

        if (attachmentIds.isEmpty()) {
            return true;
        }

        List<FileMetadata> files = fileMetadataRepository.findAllById(attachmentIds);
        if (files.size() != attachmentIds.size()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("One or more attached files do not exist.")
                    .addConstraintViolation();
            return false;
        }

        Set<UUID> alreadyReferencedIds = fileMetadataRepository.findReferencedAttachmentIds(attachmentIds);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = (auth != null) ? auth.getName() : null;

        for (FileMetadata file : files) {
            if (file.getScope() == FileScope.ATTACHMENT) {
                // If it's a newly added attachment we must ensure it was uploaded by the current user.
                if (!alreadyReferencedIds.contains(file.getId())) {
                    if (currentUsername == null || file.getUploader() == null ||
                            !currentUsername.equals(file.getUploader().getUsername())) {
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate(
                                String.format("You are not authorized to attach the file: %s", file.getOriginalName())
                        ).addConstraintViolation();
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
