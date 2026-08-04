package com.augustine.gplfantasyleaague.domain.auth.entity;

import com.augustine.gplfantasyleaague.domain.club.entity.Club;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Holds a registration attempt until its email is verified. Nothing here
// ever becomes a real `users` row until AuthService.verifyEmail() succeeds -
// previously every register() call created a permanent User row with
// emailVerified=false, so an abandoned/unverified signup attempt would
// squat that email address forever ("email already exists" for an account
// that was never actually usable). A second registration attempt for the
// same email just overwrites this row rather than being blocked.
@Entity
@Table(name = "pending_registrations")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class PendingRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "username", nullable = false)
    private String username;

    // Already bcrypt-encoded by the time it lands here - same as User.password.
    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @ManyToOne
    @JoinColumn(name = "favourite_club_id")
    private Club favouriteClub;

    @Column(name = "verification_code", nullable = false)
    private String verificationCode;

    @Column(name = "verification_code_expires_at", nullable = false)
    private LocalDateTime verificationCodeExpiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
