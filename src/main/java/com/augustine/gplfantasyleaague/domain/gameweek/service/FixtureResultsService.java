package com.augustine.gplfantasyleaague.domain.gameweek.service;

import com.augustine.gplfantasyleaague.domain.gameweek.dtos.FixtureResultsRequest;
import com.augustine.gplfantasyleaague.domain.gameweek.dtos.FixtureResultsResponse;
import com.augustine.gplfantasyleaague.domain.gameweek.entity.Fixture;
import com.augustine.gplfantasyleaague.domain.gameweek.entity.FixtureResults;
import com.augustine.gplfantasyleaague.domain.gameweek.entity.FixtureStatus;
import com.augustine.gplfantasyleaague.domain.gameweek.repository.FixtureRepository;
import com.augustine.gplfantasyleaague.domain.gameweek.repository.FixtureResultsRepository;
import com.augustine.gplfantasyleaague.domain.prediction.service.PredictionService;
import com.augustine.gplfantasyleaague.exception.InvalidFixtureException;
import com.augustine.gplfantasyleaague.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FixtureResultsService {
    private final FixtureRepository fixtureRepository;
    private final FixtureResultsRepository fixtureResultsRepository;
    private final PredictionService predictionService;

    public FixtureResultsService(FixtureRepository fixtureRepository, FixtureResultsRepository fixtureResultsRepository, PredictionService predictionService) {
        this.fixtureRepository = fixtureRepository;
        this.fixtureResultsRepository = fixtureResultsRepository;
        this.predictionService = predictionService;
    }

    public FixtureResultsResponse recordResults(FixtureResultsRequest request){
        Fixture fixture = fixtureRepository.findById(request.getFixtureId()).orElseThrow(()-> new ResourceNotFoundException("Fixture with ID " + request.getFixtureId() + " not found"));
        if(fixture.getFixtureStatus() == FixtureStatus.FINISHED){
            throw new InvalidFixtureException("Results already recorded for this fixture");
        }
        if(fixture.getFixtureStatus() == FixtureStatus.POSTPONED){
            throw new InvalidFixtureException("Cannot record results for a postponed fixture");
        }

        FixtureResults results = saveToFixtureResultsDatabase(request, fixture);
        fixture.setFixtureStatus(FixtureStatus.FINISHED);
        Fixture updateFixture = fixtureRepository.save(fixture);

        // Score every prediction made against this fixture now that the
        // real result is in - see PredictionService.scoreFixture for the
        // points formula.
        predictionService.scoreFixture(updateFixture, results.getHomeScore(), results.getAwayScore());

        return mapToResponse(results);
    }

    public FixtureResultsResponse getResultsByFixture(Integer fixtureId){
        Fixture fixture = fixtureRepository.findById(fixtureId).orElseThrow(()-> new ResourceNotFoundException("Fixture with ID " + fixtureId + " does not exist"));
        FixtureResults results = fixtureResultsRepository.findByFixtureId(fixture.getId()).orElseThrow(()-> new ResourceNotFoundException("No recorded results found for fixture ID " + fixtureId));
        return mapToResponse(results);
    }


    public List<FixtureResultsResponse> getAllResults(){
        return fixtureResultsRepository.findAll().stream()
                .map(result-> mapToResponse(result))
                .toList();
    }


    private FixtureResultsResponse mapToResponse(FixtureResults fixtureResults){
        return FixtureResultsResponse.builder()
                .id(fixtureResults.getId())
                .awayClubName(fixtureResults.getFixture().getAwayClub().getFullName())
                .homeClubName(fixtureResults.getFixture().getHomeClub().getFullName())
                .awayPossession(fixtureResults.getAwayPossession())
                .homePossession(fixtureResults.getHomePossession())
                .awayScore(fixtureResults.getAwayScore())
                .homeScore(fixtureResults.getHomeScore())
                .recordedAt(fixtureResults.getRecordedAt())
                .build();
    }

    private FixtureResults saveToFixtureResultsDatabase(FixtureResultsRequest request, Fixture fixture){
        FixtureResults results = FixtureResults.builder()
                .homeScore(request.getHomeScore())
                .homePossession(request.getHomePossession())
                .fixture(fixture)
                .recordedAt(LocalDateTime.now())
                .awayPossession(request.getAwayPossession())
                .awayScore(request.getAwayScore())
                .build();
        fixtureResultsRepository.save(results);
        return results;
    }
}
