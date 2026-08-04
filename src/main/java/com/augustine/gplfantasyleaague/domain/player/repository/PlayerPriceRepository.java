package com.augustine.gplfantasyleaague.domain.player.repository;

import com.augustine.gplfantasyleaague.domain.player.entity.PlayerPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerPriceRepository extends JpaRepository<PlayerPrice, Integer> {
    Optional<PlayerPrice> findTopByPlayerIdOrderByRecordedAtDesc(Integer playerId);
    List<PlayerPrice> findByPlayerId(Integer playerId);
    boolean existsByGameweekId(Integer gameweekId);

    // The two most recent price rows for a player, newest first - element 0
    // is the live current price, element 1 (if present) is what it was
    // before that, so (0 - 1) gives the price-change arrow direction shown
    // in the UI. A list of size < 2 means there's no prior price to compare
    // against (a brand-new player), in which case no arrow is shown.
    List<PlayerPrice> findTop2ByPlayerIdOrderByRecordedAtDesc(Integer playerId);
}