package com.augustine.gplfantasyleaague.domain.league.dtos;

import lombok.*;

// Shared shape for both of a league's leaderboards. `streak` is only ever
// populated on the predictions leaderboard - null on the Fantasy one, since
// Fantasy points don't have a streak concept.
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeagueLeaderboardEntry {
    private Integer rank;
    private Integer userId;
    private String username;
    private Integer points;
    private Integer streak;
}
