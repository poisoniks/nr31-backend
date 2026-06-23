package org.nr31.backend.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;

public interface RosterImportExportService {
    void importFromExcel(MultipartFile file, String uploaderUsername);
    void exportToExcel(OutputStream out);
    void uploadTemplate(MultipartFile file, String uploaderUsername);
}
