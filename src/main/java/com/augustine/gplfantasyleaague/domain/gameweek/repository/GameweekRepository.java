package com.augustine.gplfantasyleaague.domain.gameweek.repository;

import com.augustine.gplfantasyleaague.domain.gameweek.entity.Gameweek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GameweekRepository extends JpaRepository<Gameweek, Integer> {
    List<Gameweek> findBySeason(String season);
    Optional<Gameweek> findByIsCurrentTrue();
    List<Gameweek> findByIsCurrentTrueAndEndDateBefore(LocalDateTime now);
    Optional<Gameweek> findByGameweekNumber(Integer gameweekNumber);

    // Whether ANY gameweek has ever been created for this season - lets
    // callers tell "this season genuinely doesn't exist in our records"
    // (e.g. someone searches 2017/2018) apart from "this season exists but
    // hasn't got any results recorded yet" (e.g. 2026/2027 before kickoff).
    boolean existsBySeason(String season);

    // Every season that has at least one gameweek, oldest first (sorts
    // correctly as plain strings for this app's "YYYY/YYYY" convention) -
    // powers the Table/Fixtures screens' season chevron bounds and lets
    // them validate a searched-for season before hitting a season-specific
    // endpoint with it.
    @Query("SELECT DISTINCT g.season FROM Gameweek g ORDER BY g.season ASC")
    List<String> findDistinctSeasonsOrderBySeasonAsc();

    // Season-scoped lookup - season is a free-text field ("2026/2027"),
    // so gameweekNumber alone is not unique across seasons. Used by
    // GameweekScheduler and GameweekService to avoid the old bug where
    // findByGameweekNumber could match a gameweek from an unrelated season
    // (or throw on duplicate numbers within the same season).
    Optional<Gameweek> findBySeasonAndGameweekNumber(String season, Integer gameweekNumber);

    // Used by StandingsService to pick a default season when no gameweek is
    // flagged current (e.g. a season that has just ended) - falls back to
    // whichever season's gameweeks ran most recently, so the table follows
    // the data forward automatically once a new season is seeded.
    Optional<Gameweek> findTopByOrderByEndDateDesc();
}
