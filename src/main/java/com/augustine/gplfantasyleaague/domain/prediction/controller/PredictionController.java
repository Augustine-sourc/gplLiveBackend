package com.augustine.gplfantasyleaague.domain.prediction.controller;

import com.augustine.gplfantasyleaague.domain.prediction.dto.PredictionLeaderboardEntry;
import com.augustine.gplfantasyleaague.domain.prediction.dto.PredictionRequest;
import com.augustine.gplfantasyleaague.domain.prediction.dto.PredictionResponse;
import com.augustine.gplfantasyleaague.domain.prediction.service.PredictionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/predictions")
public class PredictionController {
    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    // Single-fixture submit/amend - allowed any number of times up until
    // kickoff.
    @PostMapping
    public ResponseEntity<PredictionResponse> submitPrediction(@RequestBody @Valid PredictionRequest request){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(predictionService.submitPrediction(request, email));
    }

    // Batch submit - matches the existing frontend shape of posting every
    // fixture's pick for a gameweek in one call.
    @PostMapping("/batch")
    public ResponseEntity<List<PredictionResponse>> submitPredictions(@RequestBody @Valid List<PredictionRequest> requests){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(predictionService.submitPredictions(requests, email));
    }

    // Defaults to the current gameweek when gameweekId is omitted.
    @GetMapping("/me")
    public ResponseEntity<List<PredictionResponse>> getMyPredictions(@RequestParam(required = false) Integer gameweekId){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(predictionService.getMyPredictions(email, gameweekId));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<PredictionLeaderboardEntry>> getLeaderboard(){
        return ResponseEntity.ok(predictionService.getLeaderboard());
    }
}
