package com.augustine.gplfantasyleaague.domain.prediction.repository;

import com.augustine.gplfantasyleaague.domain.prediction.entity.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PredictionRepository extends JpaRepository<Prediction, Integer> {
    Optional<Prediction> findByUserIdAndFixtureId(Integer userId, Integer fixtureId);

    List<Prediction> findByUserIdAndGameweekId(Integer userId, Integer gameweekId);

    // Every user's pick for a fixture - scored in one batch when
    // FixtureResultsService.recordResults() marks it FINISHED.
    List<Prediction> findByFixtureIdAndScoredFalse(Integer fixtureId);

    // Clears any other Banker the user already has this gameweek before a
    // new one is set - the partial unique index in V31 is the hard backstop,
    // this is what makes "switch your Banker to a different fixture" work
    // without hitting that constraint.
    @Modifying
    @Query("UPDATE Prediction p SET p.isBanker = false " +
            "WHERE p.user.id = :userId AND p.gameweek.id = :gameweekId AND p.fixture.id <> :excludeFixtureId")
    void clearBankerForUserInGameweek(
            @Param("userId") Integer userId,
            @Param("gameweekId") Integer gameweekId,
            @Param("excludeFixtureId") Integer excludeFixtureId
    );
}
