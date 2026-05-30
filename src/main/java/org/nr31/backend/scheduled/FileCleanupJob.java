package org.nr31.backend.scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.service.FileStorageService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileCleanupJob {

    private final FileStorageService fileStorageService;

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupFiles() {
        log.info("Starting scheduled file cleanup...");

        Instant threshold = Instant.now().minus(24, ChronoUnit.HOURS);
        fileStorageService.purgeOrphanedAttachments(threshold);

        fileStorageService.purgeOrphanedPhysicalFiles();

        log.info("Scheduled file cleanup finished.");
    }
}
