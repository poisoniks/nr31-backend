package org.nr31.backend.scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.repository.PasswordResetTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class PasswordResetTokenCleanupJob {

    private final PasswordResetTokenRepository tokenRepository;

    // Every day at 4AM
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupExpiredResetTokens() {
        log.info("Starting expired password reset tokens cleanup...");

        Instant now = Instant.now();
        int deletedCount = tokenRepository.deleteAllByExpiryDateBefore(now);

        log.info("Cleanup finished. Deleted {} expired password reset tokens.", deletedCount);
    }
}
