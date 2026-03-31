package org.nr31.backend.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
public class FileCleanupJob {

    @Autowired
    private FileStorageService fileStorageService;

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOldPendingFiles() {
        log.info("Starting scheduled cleanup of old pending files...");

        Instant threshold = Instant.now().minus(24, ChronoUnit.HOURS);
        fileStorageService.deleteOldPendingFiles(threshold);

        log.info("Scheduled cleanup of old pending files finished.");
    }
}
