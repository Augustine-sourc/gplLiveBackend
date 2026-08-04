package com.augustine.gplfantasyleaague.domain.auth.entity;

import com.augustine.gplfantasyleaague.domain.club.entity.Club;
import com.augustine.gplfantasyleaague.domain.engagement.entity.Discussion;
import com.augustine.gplfantasyleaague.domain.engagement.entity.MotmVotes;
import com.augustine.gplfantasyleaague.domain.engagement.entity.Notification;
import com.augustine.gplfantasyleaague.domain.fantasy.entity.FantasyTeam;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "favourite_club_id")
    private Club favouriteClub;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = true;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "verification_code_expires_at")
    private LocalDateTime verificationCodeExpiresAt;

    // Forgot-password flow - separate from verificationCode above so a
    // password reset request can never interfere with (or be confused with)
    // an in-progress registration email-verification.
    @Column(name = "reset_code")
    private String resetCode;

    @Column(name = "reset_code_expires_at")
    private LocalDateTime resetCodeExpiresAt;

    // Running total for the Predictions leaderboard - incremented by
    // PredictionService.scoreFixture() each time one of this user's
    // predictions is scored (see domain/prediction).
    @Builder.Default
    @Column(name = "prediction_points", nullable = false)
    private Integer predictionPoints = 0;

    // Consecutive correct-outcome predictions (in the order they're scored) -
    // drives the 1.25x/1.5x streak multiplier. Resets to 0 the moment a
    // wrong-outcome prediction is scored.
    @Builder.Default
    @Column(name = "prediction_streak", nullable = false)
    private Integer predictionStreak = 0;

    @OneToOne(mappedBy = "user")
    private FantasyTeam fantasyTeam;

    @OneToMany(mappedBy = "user")
    private List<MotmVotes> motmVotesList = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Discussion> discussions  = new ArrayList<>();
}
