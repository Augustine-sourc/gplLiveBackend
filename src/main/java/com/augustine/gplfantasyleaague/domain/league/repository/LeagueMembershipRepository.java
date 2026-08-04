package com.augustine.gplfantasyleaague.domain.league.repository;

import com.augustine.gplfantasyleaague.domain.league.entity.LeagueMembership;
import com.augustine.gplfantasyleaague.domain.league.entity.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeagueMembershipRepository extends JpaRepository<LeagueMembership, Integer> {
    Optional<LeagueMembership> findByLeagueIdAndUserId(Integer leagueId, Integer userId);

    List<LeagueMembership> findByLeagueIdAndStatus(Integer leagueId, MembershipStatus status);

    // "My leagues" - every league this user is an accepted member of
    // (PENDING requests they've made elsewhere don't show up as "my
    // leagues" until accepted).
    List<LeagueMembership> findByUserIdAndStatus(Integer userId, MembershipStatus status);

    // Capacity check on join/accept - member_limit only counts ACTIVE
    // members, not pending requests.
    long countByLeagueIdAndStatus(Integer leagueId, MembershipStatus status);
}
