package com.augustine.gplfantasyleaague.domain.scoring.repository;

import com.augustine.gplfantasyleaague.domain.scoring.entity.PlayerGameWeekStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerGameWeekStatsRepository extends JpaRepository<PlayerGameWeekStats, Integer> {

    boolean existsByPlayerIdAndFixtureId(Integer playerId, Integer fixtureId);

    List<PlayerGameWeekStats> findByFixtureId(Integer fixtureId);

    List<PlayerGameWeekStats> findByPlayerIdAndFixtureIdIn(Integer id, List<Integer> fixtureIds);
}