package com.augustine.gplfantasyleaague.domain.player;

import com.augustine.gplfantasyleaague.domain.club.Club;
import com.augustine.gplfantasyleaague.domain.engagement.MotmVotes;
import com.augustine.gplfantasyleaague.domain.fantasy.FantasyTeamPlayer;
import com.augustine.gplfantasyleaague.domain.fantasy.Transfer;
import com.augustine.gplfantasyleaague.domain.scoring.PlayerGameWeekStats;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "players")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @ManyToOne
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false)
    private Position position;

    @Column(name = "jersey_number")
    private Integer jerseyNumber;

    @Column(name = "photo_url")
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @Column(name = "nationality", nullable = false)
    private String nationality;

    @OneToMany(mappedBy = "player")
    private List<FantasyTeamPlayer> fantasyTeamPlayers = new ArrayList<>();

    @OneToMany(mappedBy = "playerOut")
    private List<Transfer> transfersOut = new ArrayList<>();

    @OneToMany(mappedBy = "playerIn")
    private List<Transfer> transfersIn = new ArrayList<>();

    @OneToMany(mappedBy = "player")
    private List<MotmVotes> motmVotesList = new ArrayList<>();

    @OneToMany(mappedBy = "player")
    private List<PlayerGameWeekStats> playerGameWeekStats = new ArrayList<>();

}
