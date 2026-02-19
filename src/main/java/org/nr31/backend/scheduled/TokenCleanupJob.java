package org.nr31.backend.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
public class TokenCleanupJob {

    @Autowired
    private RefreshTokenRepository tokenRepository;

    // Every day at 3AM
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting expired tokens cleanup...");

        Instant now = Instant.now();
        int deletedCount = tokenRepository.deleteAllByExpiryDateBefore(now);

        log.info("Cleanup finished. Deleted {} expired tokens.", deletedCount);
    }
}
