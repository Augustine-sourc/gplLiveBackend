package com.augustine.gplfantasyleaague.domain.league.entity;

public enum MembershipStatus {
    // Requested to join a PRIVATE league, awaiting the creator's decision.
    // Doesn't count toward member_limit and doesn't appear on the
    // leaderboard until it becomes ACTIVE.
    PENDING,
    // An actual member - counts toward member_limit, shows on the
    // leaderboard. Public-league joins go straight to ACTIVE; private-league
    // joins only get here once the creator accepts the PENDING request.
    ACTIVE
}
