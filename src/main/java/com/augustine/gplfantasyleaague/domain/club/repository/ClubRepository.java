package com.augustine.gplfantasyleaague.domain.club;

import com.augustine.gplfantasyleaague.domain.club.entity.Club;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubRepository extends JpaRepository<Club, Integer> {
}