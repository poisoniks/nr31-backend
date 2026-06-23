package org.nr31.backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.dto.admin.AppConfigDto;
import org.nr31.backend.dto.media.FileUploadResponse;
import org.nr31.backend.model.AppConfigKey;
import org.nr31.backend.model.FileScope;
import org.nr31.backend.exception.FileStorageException;
import org.nr31.backend.service.AppConfigService;
import org.nr31.backend.service.FileStorageService;
import org.nr31.backend.service.RosterImportExportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.OutputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RosterImportExportServiceImpl implements RosterImportExportService {

    private final RosterExcelImporter rosterExcelImporter;
    private final RosterExcelExporter rosterExcelExporter;
    private final FileStorageService fileStorageService;
    private final AppConfigService appConfigService;

    @Override
    @Transactional
    public void importFromExcel(MultipartFile file, String uploaderUsername) {
        rosterExcelImporter.importFromExcel(file, uploaderUsername);
    }

    @Override
    @Transactional(readOnly = true)
    public void exportToExcel(OutputStream out) {
        rosterExcelExporter.exportToExcel(out);
    }

    @Override
    @Transactional
    public void uploadTemplate(MultipartFile file, String uploaderUsername) {
        if (file.isEmpty()) {
            throw new FileStorageException("Template file cannot be empty");
        }

        UUID oldFileId = null;
        try {
            AppConfigDto currentConfig = appConfigService.getConfig(AppConfigKey.ROSTER_EXPORT_TEMPLATE_FILE_ID);
            if (currentConfig.getConfigValue() != null && !currentConfig.getConfigValue().isNull()) {
                oldFileId = UUID.fromString(currentConfig.getConfigValue().asText());
            }
        } catch (Exception e) {
            log.debug("No existing roster template config found, skipping old file cleanup");
        }

        FileUploadResponse response = fileStorageService.storeFile(file, uploaderUsername, FileScope.SYSTEM);
        UUID newFileId = response.getId();

        AppConfigDto configDto = AppConfigDto.builder()
                .name(AppConfigKey.ROSTER_EXPORT_TEMPLATE_FILE_ID.getKey())
                .configValue(new ObjectMapper().valueToTree(newFileId.toString()))
                .build();

        appConfigService.updateConfig(AppConfigKey.ROSTER_EXPORT_TEMPLATE_FILE_ID.getKey(), configDto);

        if (oldFileId != null) {
            try {
                fileStorageService.deleteFile(oldFileId);
                log.info("Deleted previous roster template file: {}", oldFileId);
            } catch (Exception e) {
                log.warn("Failed to delete previous roster template file {}: {}", oldFileId, e.getMessage());
            }
        }

        log.info("Roster export template uploaded successfully with file metadata ID: {}", newFileId);
    }
}
