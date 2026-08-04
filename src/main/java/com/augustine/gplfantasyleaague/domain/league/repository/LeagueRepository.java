package com.augustine.gplfantasyleaague.domain.league.repository;

import com.augustine.gplfantasyleaague.domain.league.entity.League;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeagueRepository extends JpaRepository<League, Integer> {
    Optional<League> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);

    // Search screen's league lookup - only ever searches PUBLIC leagues,
    // private ones are only reachable via their invite code. Empty/blank
    // query still works here (returns every public league) since
    // "Containing" against "" matches everything - powers a plain "browse
    // public leagues" view with no separate method needed.
    List<League> findByIsPublicTrueAndNameContainingIgnoreCase(String name);
}
