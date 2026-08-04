package com.augustine.gplfantasyleaague.domain.league.repository;

import com.augustine.gplfantasyleaague.domain.league.entity.League;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeagueRepository extends JpaRepository<League, Integer> {
    Optional<League> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);

    // Search screen's league lookup - returns BOTH public and private
    // leagues by name. Private ones are still discoverable this way (the
    // app targets an APK/manual-install audience, not app-store users, so
    // "hidden unless you already have the code" was more friction than the
    // product needs) - joining one still creates a PENDING request the
    // creator has to accept, same as ever. Empty/blank query still works
    // (returns every league) since "Containing" against "" matches
    // everything - powers a plain "browse leagues" view for free.
    List<League> findByNameContainingIgnoreCase(String name);

    // Per-user cap on how many leagues someone can create (see
    // LeagueService.MAX_LEAGUES_CREATED) - stops one account from spamming
    // the search/browse list with league after league.
    long countByCreatorId(Integer creatorId);
}
