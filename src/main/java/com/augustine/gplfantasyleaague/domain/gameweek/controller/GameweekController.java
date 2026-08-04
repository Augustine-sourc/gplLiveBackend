package com.augustine.gplfantasyleaague.domain.gameweek.controller;

import com.augustine.gplfantasyleaague.domain.gameweek.dtos.GameweekRequest;
import com.augustine.gplfantasyleaague.domain.gameweek.dtos.GameweekResponse;
import com.augustine.gplfantasyleaague.domain.gameweek.service.GameweekService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/gameweeks")
public class GameweekController {
    private final GameweekService gameweekService;

    public GameweekController(GameweekService gameweekService) {
        this.gameweekService = gameweekService;
    }

    @GetMapping
    public ResponseEntity<List<GameweekResponse>> getAllGameweeks(){
        return ResponseEntity.ok(gameweekService.getAllGameweeks());
    }

    @GetMapping("/current")
    public ResponseEntity<GameweekResponse> getCurrentGameweek(){
        return ResponseEntity.ok(gameweekService.getCurrentGameweek());
    }

    // Query param, not a path variable - season strings contain a literal
    // "/" (e.g. "2026/2027"), and Spring Security's default StrictHttpFirewall
    // rejects any request whose PATH contains an encoded slash (%2F), so
    // /gameweeks/season/2026%2F2027 would 400 before ever reaching this
    // method. Query params don't have that restriction.
    @GetMapping("/season")
    public ResponseEntity<List<GameweekResponse>> getGameweekBySeason(@RequestParam String season){
        return ResponseEntity.ok(gameweekService.getGameweekBySeason(season));
    }

    // Every season that has data, oldest first - lets the Fixtures/Table
    // screens build season chevrons and give a real "we don't have records
    // for that season" message instead of guessing from an empty list.
    @GetMapping("/seasons")
    public ResponseEntity<List<String>> getAllSeasons(){
        return ResponseEntity.ok(gameweekService.getAllSeasons());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<GameweekResponse> createGameWeek(@RequestBody @Valid GameweekRequest request){
        return ResponseEntity.ok(gameweekService.createGameweek(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/set-current")
    public ResponseEntity<GameweekResponse> setIsCuurent(@PathVariable Integer id){
        return ResponseEntity.ok(gameweekService.setCurrentGameweek(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<GameweekResponse> updateGameweek(@PathVariable Integer id, @RequestBody @Valid GameweekRequest request){
        return ResponseEntity.ok(gameweekService.updateGameweek(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGameweek(@PathVariable Integer id){
        gameweekService.deleteGameweek(id);
        return ResponseEntity.noContent().build();
    }
}
