package com.augustine.gplfantasyleaague.domain.fantasy.repository;

import com.augustine.gplfantasyleaague.domain.fantasy.entity.FantasyTeam;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FantasyTeamRepository extends JpaRepository<FantasyTeam, Integer> {
    boolean existsByUserId(Integer id);

    boolean existsByTeamName(String teamName);

    Optional<FantasyTeam> findByUserId(Integer id);

    // Powers a league's Fantasy-points leaderboard - one query for every
    // member's team instead of N. Not every member necessarily has a
    // FantasyTeam yet (haven't built a squad) - callers treat a missing
    // entry as 0 points rather than excluding that member.
    List<FantasyTeam> findByUserIdIn(List<Integer> userIds);
}