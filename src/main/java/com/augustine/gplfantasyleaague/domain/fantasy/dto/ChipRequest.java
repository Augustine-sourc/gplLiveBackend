package com.augustine.gplfantasyleaague.domain.scoring.dtos;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChipRequest {
    private Integer fantasyTeamId;
    private Integer gameweekId;
}
