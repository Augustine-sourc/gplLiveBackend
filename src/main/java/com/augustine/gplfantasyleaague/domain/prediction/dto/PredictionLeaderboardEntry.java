package com.augustine.gplfantasyleaague.domain.prediction.dto;

import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionLeaderboardEntry {
    private Integer rank;
    private Integer userId;
    private String username;
    private Integer predictionPoints;
    private Integer predictionStreak;
}
