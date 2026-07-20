package com.augustine.gplfantasyleaague.domain.fantasy;

import com.augustine.gplfantasyleaague.domain.gameweek.Gameweek;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chips")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Chip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "fantasy_team_id")
    private FantasyTeam fantasyTeam;

    @ManyToOne
    @JoinColumn(name = "gameweek_id")
    private Gameweek gameweek;

    @Enumerated(EnumType.STRING)
    @Column(name = "chip_type")
    private ChipType chipType;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

}
