package com.augustine.gplfantasyleaague.domain.player;

import com.augustine.gplfantasyleaague.domain.player.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Integer> {
}