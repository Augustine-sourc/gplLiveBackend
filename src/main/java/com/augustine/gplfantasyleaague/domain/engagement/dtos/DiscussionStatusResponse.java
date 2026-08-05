package com.augustine.gplfantasyleaague.domain.engagement.dtos;

import lombok.*;

import java.time.LocalDateTime;

// Lets the frontend decide upfront whether to show the message composer at
// all, instead of only finding out a fixture's discussion is closed when a
// POST comes back with a 400 - see DiscussionService.getDiscussionStatus.
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionStatusResponse {
    private Integer fixtureId;
    private boolean open;
    // When present and `open` is false because the window hasn't started
    // yet, this is when it will (the fixture's gameweek deadline). Null once
    // the discussion has opened, or if it's closed because the match ended.
    private LocalDateTime opensAt;
    // Short, user-facing explanation ("Opens once the gameweek deadline
    // passes", "Closed - full time", etc).
    private String reason;
}
