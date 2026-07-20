package com.augustine.gplfantasyleaague.domain.scoring.dtos;

import com.augustine.gplfantasyleaague.domain.fantasy.entity.ChipType;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChipResponse {
    private Integer id;
    private String fantasyTeamName;
    private ChipType chipType;
    private LocalDateTime usedAt;
}
