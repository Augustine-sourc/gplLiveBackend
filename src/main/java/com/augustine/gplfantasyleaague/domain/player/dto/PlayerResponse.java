package com.augustine.gplfantasyleaague.domain.player.dto;

import com.augustine.gplfantasyleaague.domain.player.entity.Position;
import com.augustine.gplfantasyleaague.domain.player.entity.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerResponse {
    private Integer id;
    private String fullName;
    private Integer clubId;
    private String clubName;
    private Integer jerseyNumber;
    private Position position;
    private String photoUrl;
    private String nationality;
    private Status status;
    // Latest recorded price for this player, null if no price has been set yet.
    private BigDecimal currentPrice;

    // currentPrice minus the price before it - positive means the price
    // just went up, negative means it dropped, null means there's no prior
    // price to compare against yet. Powers the up/down arrow in the UI.
    private BigDecimal priceChange;
}
