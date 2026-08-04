package com.augustine.gplfantasyleaague.domain.fantasy.repository;

import com.augustine.gplfantasyleaague.domain.fantasy.entity.FreeHitSnapShot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FreeHitSnapShotRepository extends JpaRepository<FreeHitSnapShot, Integer> {
    List<FreeHitSnapShot> findByFantasyTeamIdAndGameweekId(Integer fantasyTeamId, Integer gameweekId);
    void deleteByFantasyTeamIdAndGameweekId(Integer fantasyTeamId, Integer gameweekId);
    boolean existsByGameweekId(Integer gameweekId);

    // Full team-deletion cleanup (see FantasyTeamService.deleteMyFantasyTeam)
    // - every snapshot row for this team, not just one gameweek's worth.
    void deleteByFantasyTeamId(Integer fantasyTeamId);
}