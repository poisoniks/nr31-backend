package org.nr31.backend.service;

import org.springframework.web.multipart.MultipartFile;

public interface RosterImportExportService {
    void importFromExcel(MultipartFile file);
    byte[] exportToExcel();
    void uploadTemplate(MultipartFile file);
}
