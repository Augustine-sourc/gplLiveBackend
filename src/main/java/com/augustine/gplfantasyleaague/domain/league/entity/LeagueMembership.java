package com.augustine.gplfantasyleaague.domain.league.entity;

import com.augustine.gplfantasyleaague.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// One row per (league, user). Stored as plain VARCHAR via EnumType.STRING
// (same pattern as Prediction.outcome) - no Postgres native enum type
// needed for this one.
@Entity
@Table(name = "league_memberships")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class LeagueMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "league_id", nullable = false)
    private League league;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MembershipStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;
}
