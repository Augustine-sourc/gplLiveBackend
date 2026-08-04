package com.augustine.gplfantasyleaague.domain.prediction.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PredictionRequest {
    @NotNull
    private Integer fixtureId;

    // Wire format matches the frontend's existing lowercase
    // 'home' | 'draw' | 'away' Prediction['outcome'] union - kept lowercase
    // here rather than forcing the frontend to send the backend's uppercase
    // PredictionOutcome enum names.
    @NotNull
    @Pattern(regexp = "(?i)home|draw|away", message = "must be 'home', 'draw', or 'away'")
    private String outcome;

    @NotNull
    @Min(0)
    private Integer exactHomeGoals;

    @NotNull
    @Min(0)
    private Integer exactAwayGoals;

    // Optional - defaults to false server-side if omitted.
    private Boolean isBanker;
}
