package com.augustine.gplfantasyleaague.domain.auth.repository;

import com.augustine.gplfantasyleaague.domain.auth.entity.PendingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, Integer> {
    Optional<PendingRegistration> findByEmail(String email);

    // Used by the daily cleanup job (see PendingRegistrationCleanupScheduler)
    // to drop attempts nobody ever came back to verify, so this table
    // doesn't grow unbounded from one-off abandoned signups.
    void deleteByVerificationCodeExpiresAtBefore(LocalDateTime cutoff);
}
