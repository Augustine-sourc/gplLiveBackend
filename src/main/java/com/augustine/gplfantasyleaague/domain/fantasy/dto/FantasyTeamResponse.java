package com.augustine.gplfantasyleaague.domain.fantasy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FantasyTeamResponse {
    private Integer id;
    private String teamName;
    private Integer totalPoints;
    private BigDecimal budgetRemaining;
    private Integer transferPoints;
    private String username;

    // Which chips have ever been used (one-time-per-season, except
    // wildcard/wildcard2 which are separate slots) - keyed by camelCase chip
    // name (tripleCaptain, benchBoost, wildcard, wildcard2, freeHit) to match
    // the frontend's ChipStatus shape directly. Was previously missing
    // entirely from this DTO, so the app could never actually show a chip as
    // used no matter what had been activated.
    private Map<String, Boolean> chips;

    // camelCase key (e.g. "wildcard") of whichever chip is already active
    // for the CURRENT gameweek, or null if none. Only one chip can be active
    // per team per gameweek (enforced server-side via the unique
    // (fantasy_team_id, gameweek_id) constraint on chips) - this lets the
    // client disable the other chip buttons for the week without the user
    // having to hit a 400 first to find out.
    private String activeChipKey;

}
