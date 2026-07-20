package com.augustine.gplfantasyleaague.domain.scoring.service;

import com.augustine.gplfantasyleaague.domain.auth.entity.User;
import com.augustine.gplfantasyleaague.domain.auth.repository.UserRepository;
import com.augustine.gplfantasyleaague.domain.fantasy.entity.ChipType;
import com.augustine.gplfantasyleaague.domain.fantasy.entity.FantasyTeam;
import com.augustine.gplfantasyleaague.domain.fantasy.repository.ChipRepository;
import com.augustine.gplfantasyleaague.domain.fantasy.repository.FantasyTeamRepository;
import com.augustine.gplfantasyleaague.domain.gameweek.entity.Gameweek;
import com.augustine.gplfantasyleaague.domain.gameweek.repository.GameweekRepository;
import com.augustine.gplfantasyleaague.domain.scoring.dtos.ChipResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ChipService {
    private final FantasyTeamRepository fantasyTeamRepository;
    private final GameweekRepository gameweekRepository;
    private final UserRepository userRepository;
    private final ChipRepository chipRepository;

    public ChipService(FantasyTeamRepository fantasyTeamRepository, GameweekRepository gameweekRepository, UserRepository userRepository, ChipRepository chipRepository) {
        this.fantasyTeamRepository = fantasyTeamRepository;
        this.gameweekRepository = gameweekRepository;
        this.userRepository = userRepository;
        this.chipRepository = chipRepository;
    }

    public ChipResponse activateTripleCaptain(Integer fantasyTeamId, Integer gameweekId, String email){
        User user = userRepository.findByEmail(email).orElseThrow(()-> new RuntimeException("Email not found"));
        FantasyTeam team = fantasyTeamRepository.findById(fantasyTeamId).orElseThrow(()-> new RuntimeException("Team not found"));
        Gameweek gameweek = gameweekRepository.findById(gameweekId).orElseThrow(()-> new RuntimeException("Gameweek not found"));

        if(gameweek.getDeadline().isBefore(LocalDateTime.now())){
            throw new RuntimeException("Gameweek has already ended");
        }

        if(chipRepository.findByFantasyTeamIdAndChipType(team.getId(), ChipType.TRIPLE_CAPTAIN)){
            throw new RuntimeException("You have already used triple captain for this season");
        }

        

    }
}
