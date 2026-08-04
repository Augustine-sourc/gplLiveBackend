package com.augustine.gplfantasyleaague.domain.league.dtos;

import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeagueResponse {
    private Integer id;
    private String name;
    private Boolean isPublic;

    // Only ever populated for the creator or an ACTIVE/PENDING member -
    // LeagueService withholds this from public search results for leagues
    // the caller hasn't joined, so a stranger browsing public leagues can't
    // harvest invite codes for leagues they're not in yet (they don't need
    // one - public leagues join by id, not code).
    private String inviteCode;

    private Integer memberLimit;
    private Integer activeMemberCount;
    private String creatorUsername;
    private LocalDateTime createdAt;

    // Where the CALLING user stands relative to this league - drives the
    // frontend's button state (Join / Request to Join / Pending / Open).
    // One of: OWNER, ACTIVE, PENDING, NONE.
    private String callerStatus;
}
