package com.augustine.gplfantasyleaague.domain.league.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeagueCreateRequest {
    @NotBlank
    @Size(max = 60)
    private String name;

    // Defaults to true (public) if omitted - matches League.isPublic's
    // own default, see LeagueService.createLeague.
    private Boolean isPublic;

    // Optional - LeagueService clamps this into
    // [MIN_MEMBER_LIMIT, MAX_MEMBER_LIMIT] and falls back to a sane default
    // if omitted, rather than rejecting the request outright.
    @Min(2)
    @Max(200)
    private Integer memberLimit;
}
