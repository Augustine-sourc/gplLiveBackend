package com.augustine.gplfantasyleaague.config;

import com.augustine.gplfantasyleaague.domain.auth.repository.PendingRegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// Registration attempts that nobody ever verified (code expired 15 minutes
// after signup, and the person never came back to retry) would otherwise
// sit in pending_registrations forever. Sweeps them out once a day so the
// table only ever holds genuinely in-progress signups.
@Component
public class PendingRegistrationCleanupScheduler {
    private static final Logger log = LoggerFactory.getLogger(PendingRegistrationCleanupScheduler.class);
    private final PendingRegistrationRepository pendingRegistrationRepository;

    public PendingRegistrationCleanupScheduler(PendingRegistrationRepository pendingRegistrationRepository) {
        this.pendingRegistrationRepository = pendingRegistrationRepository;
    }

    @Scheduled(cron = "0 30 0 * * *") // 00:30 daily - offset from GameweekScheduler's midnight run
    public void removeExpiredPendingRegistrations() {
        try {
            pendingRegistrationRepository.deleteByVerificationCodeExpiresAtBefore(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Failed to clean up expired pending registrations", e);
        }
    }
}
