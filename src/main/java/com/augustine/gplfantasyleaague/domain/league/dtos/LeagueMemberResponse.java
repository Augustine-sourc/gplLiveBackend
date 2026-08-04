package com.augustine.gplfantasyleaague.domain.league.dtos;

import lombok.*;

import java.time.LocalDateTime;

// Used both for the active member list and the owner's pending-requests
// list - `status` tells them apart.
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeagueMemberResponse {
    private Integer userId;
    private String username;
    private String status;
    private LocalDateTime requestedAt;
}
