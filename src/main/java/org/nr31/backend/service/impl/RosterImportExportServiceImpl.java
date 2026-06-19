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
    public void importFromExcel(MultipartFile file) {
        rosterExcelImporter.importFromExcel(file);
    }

    @Override
    @Transactional(readOnly = true)
    public void exportToExcel(OutputStream out) {
        rosterExcelExporter.exportToExcel(out);
    }

    @Override
    @Transactional
    public void uploadTemplate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileStorageException("Template file cannot be empty");
        }

        // Store file with ATTACHMENT scope so it is kept in system uploads
        FileUploadResponse response = fileStorageService.storeFile(file, "admin", FileScope.ATTACHMENT);
        UUID fileId = response.getId();

        AppConfigDto configDto = AppConfigDto.builder()
                .name(AppConfigKey.ROSTER_EXPORT_TEMPLATE_FILE_ID.getKey())
                .configValue(new ObjectMapper().valueToTree(fileId.toString()))
                .build();

        appConfigService.updateConfig(AppConfigKey.ROSTER_EXPORT_TEMPLATE_FILE_ID.getKey(), configDto);
        log.info("Roster export template uploaded successfully with file metadata ID: {}", fileId);
    }
}
