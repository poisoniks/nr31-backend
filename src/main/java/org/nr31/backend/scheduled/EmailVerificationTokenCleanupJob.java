package org.nr31.backend.scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.repository.EmailVerificationTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailVerificationTokenCleanupJob {

    private final EmailVerificationTokenRepository tokenRepository;

    // Every day at 4AM
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupExpiredVerificationTokens() {
        log.info("Starting expired email verification tokens cleanup...");

        Instant now = Instant.now();
        int deletedCount = tokenRepository.deleteAllByExpiryDateBefore(now);

        log.info("Cleanup finished. Deleted {} expired email verification tokens.", deletedCount);
    }
}
