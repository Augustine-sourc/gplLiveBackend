package com.augustine.gplfantasyleaague.domain.prediction.entity;

import com.augustine.gplfantasyleaague.domain.auth.entity.User;
import com.augustine.gplfantasyleaague.domain.gameweek.entity.Fixture;
import com.augustine.gplfantasyleaague.domain.gameweek.entity.Gameweek;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "predictions")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Prediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "fixture_id")
    private Fixture fixture;

    // Denormalized off fixture.gameweek purely so the DB can enforce
    // "one Banker per user per gameweek" via a partial unique index
    // (see V31__predictions.sql) without a join.
    @ManyToOne
    @JoinColumn(name = "gameweek_id")
    private Gameweek gameweek;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Stored as plain VARCHAR (not a Postgres native enum type, unlike
    // FixtureStatus/ClubStatus) - EnumType.STRING alone handles that.
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false)
    private PredictionOutcome outcome;

    @Column(name = "exact_home_goals", nullable = false)
    private Integer exactHomeGoals;

    @Column(name = "exact_away_goals", nullable = false)
    private Integer exactAwayGoals;

    @Builder.Default
    @Column(name = "is_banker", nullable = false)
    private Boolean isBanker = false;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    // Null until the fixture is scored (see PredictionService.scoreFixture).
    @Column(name = "points_earned")
    private Integer pointsEarned;

    @Builder.Default
    @Column(name = "scored", nullable = false)
    private Boolean scored = false;
}
