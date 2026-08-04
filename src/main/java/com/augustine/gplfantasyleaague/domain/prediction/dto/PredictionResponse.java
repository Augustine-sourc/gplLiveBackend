package com.augustine.gplfantasyleaague.domain.prediction.dto;

import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionResponse {
    private Integer id;
    private Integer fixtureId;
    private Integer gameweekNumber;
    private String homeClubName;
    private String awayClubName;
    private String outcome; // lowercase - see PredictionRequest
    private Integer exactHomeGoals;
    private Integer exactAwayGoals;
    private Boolean isBanker;
    private Boolean isDerby;
    private LocalDateTime submittedAt;
    // True once the fixture kicks off - the frontend disables editing at
    // that point regardless of whether points have been scored yet.
    private Boolean locked;
    private Boolean scored;
    private Integer pointsEarned;
}
