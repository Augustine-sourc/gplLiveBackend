package com.augustine.gplfantasyleaague.domain.prediction.service;

import com.augustine.gplfantasyleaague.domain.auth.entity.User;
import com.augustine.gplfantasyleaague.domain.auth.repository.UserRepository;
import com.augustine.gplfantasyleaague.domain.gameweek.entity.Fixture;
import com.augustine.gplfantasyleaague.domain.gameweek.entity.FixtureStatus;
import com.augustine.gplfantasyleaague.domain.gameweek.entity.Gameweek;
import com.augustine.gplfantasyleaague.domain.gameweek.repository.FixtureRepository;
import com.augustine.gplfantasyleaague.domain.gameweek.repository.GameweekRepository;
import com.augustine.gplfantasyleaague.domain.prediction.dto.PredictionLeaderboardEntry;
import com.augustine.gplfantasyleaague.domain.prediction.dto.PredictionRequest;
import com.augustine.gplfantasyleaague.domain.prediction.dto.PredictionResponse;
import com.augustine.gplfantasyleaague.domain.prediction.entity.Prediction;
import com.augustine.gplfantasyleaague.domain.prediction.entity.PredictionOutcome;
import com.augustine.gplfantasyleaague.domain.prediction.repository.PredictionRepository;
import com.augustine.gplfantasyleaague.exception.InvalidPredictionException;
import com.augustine.gplfantasyleaague.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PredictionService {
    private final PredictionRepository predictionRepository;
    private final FixtureRepository fixtureRepository;
    private final GameweekRepository gameweekRepository;
    private final UserRepository userRepository;

    public PredictionService(PredictionRepository predictionRepository, FixtureRepository fixtureRepository, GameweekRepository gameweekRepository, UserRepository userRepository) {
        this.predictionRepository = predictionRepository;
        this.fixtureRepository = fixtureRepository;
        this.gameweekRepository = gameweekRepository;
        this.userRepository = userRepository;
    }

    // Submits (or amends) one prediction. Predictions can be changed freely
    // right up until kickoff - "locked" only becomes true once the fixture's
    // matchDate has passed or an admin has moved it out of SCHEDULED, at
    // which point this throws instead of silently no-op'ing.
    @Transactional
    public PredictionResponse submitPrediction(PredictionRequest request, String email){
        User user = userRepository.findByEmail(email).orElseThrow(()-> new ResourceNotFoundException("User not found"));
        Fixture fixture = fixtureRepository.findById(request.getFixtureId())
                .orElseThrow(()-> new ResourceNotFoundException("Fixture with ID " + request.getFixtureId() + " not found"));

        if(isLocked(fixture)){
            throw new InvalidPredictionException("Predictions are locked for this fixture - kickoff has passed");
        }

        PredictionOutcome outcome = parseOutcome(request.getOutcome());
        boolean wantsBanker = Boolean.TRUE.equals(request.getIsBanker());

        Prediction prediction = predictionRepository.findByUserIdAndFixtureId(user.getId(), fixture.getId())
                .orElseGet(() -> Prediction.builder()
                        .fixture(fixture)
                        .gameweek(fixture.getGameweek())
                        .user(user)
                        .build());

        if(wantsBanker){
            // Only one Banker per gameweek - moving it here first, so the
            // partial unique index in V31 never actually gets tripped in
            // normal use.
            predictionRepository.clearBankerForUserInGameweek(user.getId(), fixture.getGameweek().getId(), fixture.getId());
        }

        prediction.setOutcome(outcome);
        prediction.setExactHomeGoals(request.getExactHomeGoals());
        prediction.setExactAwayGoals(request.getExactAwayGoals());
        prediction.setIsBanker(wantsBanker);
        prediction.setSubmittedAt(LocalDateTime.now());
        predictionRepository.save(prediction);

        return mapToResponse(prediction);
    }

    @Transactional
    public List<PredictionResponse> submitPredictions(List<PredictionRequest> requests, String email){
        return requests.stream()
                .map(request -> submitPrediction(request, email))
                .toList();
    }

    public List<PredictionResponse> getMyPredictions(String email, Integer gameweekId){
        User user = userRepository.findByEmail(email).orElseThrow(()-> new ResourceNotFoundException("User not found"));
        Gameweek gameweek = resolveGameweek(gameweekId);
        return predictionRepository.findByUserIdAndGameweekId(user.getId(), gameweek.getId()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<PredictionLeaderboardEntry> getLeaderboard(){
        List<User> ranked = userRepository.findAllByOrderByPredictionPointsDesc();
        return java.util.stream.IntStream.range(0, ranked.size())
                .mapToObj(i -> {
                    User user = ranked.get(i);
                    return PredictionLeaderboardEntry.builder()
                            .rank(i + 1)
                            .userId(user.getId())
                            .username(user.getUsername())
                            .predictionPoints(user.getPredictionPoints())
                            .predictionStreak(user.getPredictionStreak())
                            .build();
                })
                .toList();
    }

    // Called from FixtureResultsService.recordResults() once a fixture is
    // marked FINISHED - scores every not-yet-scored prediction for it and
    // updates each user's running prediction_points/prediction_streak.
    //
    // Stacking order (confirmed product decision, see V31 migration comment
    // and PredictionService tests if/when added):
    //   finalPoints = (basePoints + derbyBonus) * bankerMultiplier * streakMultiplier + earlyBonus
    // - derbyBonus only applies when basePoints > 0 (a wrong pick on a derby
    //   still scores 0, it doesn't get "bonused" into a positive score).
    // - earlyBonus is unconditional (+1 for submitting 24h+ before kickoff)
    //   since it rewards submission timing, not accuracy.
    // - streakMultiplier is based on the streak *entering* this fixture
    //   (i.e. before this result updates it).
    @Transactional
    public void scoreFixture(Fixture fixture, Integer actualHomeGoals, Integer actualAwayGoals){
        if(actualHomeGoals == null || actualAwayGoals == null){
            return;
        }

        List<Prediction> predictions = predictionRepository.findByFixtureIdAndScoredFalse(fixture.getId());
        PredictionOutcome actualOutcome = resolveOutcome(actualHomeGoals, actualAwayGoals);

        for(Prediction prediction : predictions){
            User user = prediction.getUser();

            int basePoints = calculateBasePoints(prediction, actualHomeGoals, actualAwayGoals, actualOutcome);
            boolean outcomeCorrect = basePoints > 0;

            int precedingStreak = user.getPredictionStreak() == null ? 0 : user.getPredictionStreak();
            double streakMultiplier = streakMultiplierFor(precedingStreak);

            int derbyBonus = (Boolean.TRUE.equals(fixture.getIsDerby()) && basePoints > 0) ? 2 : 0;
            double bankerMultiplier = Boolean.TRUE.equals(prediction.getIsBanker()) ? 2.0 : 1.0;
            int earlyBonus = isEarlySubmission(prediction, fixture) ? 1 : 0;

            int finalPoints = (int) Math.round((basePoints + derbyBonus) * bankerMultiplier * streakMultiplier) + earlyBonus;

            prediction.setPointsEarned(finalPoints);
            prediction.setScored(true);
            predictionRepository.save(prediction);

            user.setPredictionPoints((user.getPredictionPoints() == null ? 0 : user.getPredictionPoints()) + finalPoints);
            user.setPredictionStreak(outcomeCorrect ? precedingStreak + 1 : 0);
            userRepository.save(user);
        }
    }

    // ---- helpers ----

    private boolean isLocked(Fixture fixture){
        if(fixture.getFixtureStatus() != FixtureStatus.SCHEDULED){
            return true;
        }
        LocalDateTime matchDate = fixture.getMatchDate();
        return matchDate != null && !matchDate.isAfter(LocalDateTime.now());
    }

    private boolean isEarlySubmission(Prediction prediction, Fixture fixture){
        if(fixture.getMatchDate() == null || prediction.getSubmittedAt() == null){
            return false;
        }
        return prediction.getSubmittedAt().isBefore(fixture.getMatchDate().minusHours(24));
    }

    private double streakMultiplierFor(int precedingStreak){
        if(precedingStreak >= 6) return 1.5;
        if(precedingStreak >= 3) return 1.25;
        return 1.0;
    }

    private int calculateBasePoints(Prediction prediction, int actualHome, int actualAway, PredictionOutcome actualOutcome){
        boolean exact = prediction.getExactHomeGoals().equals(actualHome) && prediction.getExactAwayGoals().equals(actualAway);
        if(exact){
            return 7;
        }

        if(prediction.getOutcome() != actualOutcome){
            return 0;
        }

        int predictedDiff = prediction.getExactHomeGoals() - prediction.getExactAwayGoals();
        int actualDiff = actualHome - actualAway;
        if(predictedDiff == actualDiff){
            return 4;
        }

        return 2;
    }

    private PredictionOutcome resolveOutcome(int home, int away){
        if(home > away) return PredictionOutcome.HOME;
        if(home < away) return PredictionOutcome.AWAY;
        return PredictionOutcome.DRAW;
    }

    private PredictionOutcome parseOutcome(String raw){
        try {
            return PredictionOutcome.valueOf(raw.trim().toUpperCase());
        } catch (Exception e){
            throw new InvalidPredictionException("Invalid outcome '" + raw + "' - must be 'home', 'draw', or 'away'");
        }
    }

    private Gameweek resolveGameweek(Integer gameweekId){
        if(gameweekId != null){
            return gameweekRepository.findById(gameweekId)
                    .orElseThrow(()-> new ResourceNotFoundException("Gameweek with ID " + gameweekId + " not found"));
        }
        return gameweekRepository.findByIsCurrentTrue()
                .orElseThrow(()-> new ResourceNotFoundException("No current gameweek set"));
    }

    private PredictionResponse mapToResponse(Prediction prediction){
        Fixture fixture = prediction.getFixture();
        return PredictionResponse.builder()
                .id(prediction.getId())
                .fixtureId(fixture.getId())
                .gameweekNumber(fixture.getGameweek().getGameweekNumber())
                .homeClubName(fixture.getHomeClub().getFullName())
                .awayClubName(fixture.getAwayClub().getFullName())
                .outcome(prediction.getOutcome().name().toLowerCase())
                .exactHomeGoals(prediction.getExactHomeGoals())
                .exactAwayGoals(prediction.getExactAwayGoals())
                .isBanker(prediction.getIsBanker())
                .isDerby(fixture.getIsDerby())
                .submittedAt(prediction.getSubmittedAt())
                .locked(isLocked(fixture))
                .scored(prediction.getScored())
                .pointsEarned(prediction.getPointsEarned())
                .build();
    }
}
