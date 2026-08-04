package com.augustine.gplfantasyleaague.domain.league.controller;

import com.augustine.gplfantasyleaague.domain.league.dtos.LeagueCreateRequest;
import com.augustine.gplfantasyleaague.domain.league.dtos.LeagueLeaderboardEntry;
import com.augustine.gplfantasyleaague.domain.league.dtos.LeagueMemberResponse;
import com.augustine.gplfantasyleaague.domain.league.dtos.LeagueResponse;
import com.augustine.gplfantasyleaague.domain.league.service.LeagueService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leagues")
public class LeagueController {
    private final LeagueService leagueService;

    public LeagueController(LeagueService leagueService) {
        this.leagueService = leagueService;
    }

    private String currentEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @PostMapping
    public ResponseEntity<LeagueResponse> createLeague(@RequestBody @Valid LeagueCreateRequest request) {
        return ResponseEntity.ok(leagueService.createLeague(request, currentEmail()));
    }

    // Blank/omitted query returns every league (public AND private) - the
    // Search screen's "browse" state before the user types anything.
    @GetMapping("/search")
    public ResponseEntity<List<LeagueResponse>> searchLeagues(
            @RequestParam(required = false) String query
    ) {
        return ResponseEntity.ok(leagueService.searchLeagues(query, currentEmail()));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<LeagueResponse>> getMyLeagues() {
        return ResponseEntity.ok(leagueService.getMyLeagues(currentEmail()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeagueResponse> getLeague(@PathVariable Integer id) {
        return ResponseEntity.ok(leagueService.getLeague(id, currentEmail()));
    }

    // Works for either kind of league - ACTIVE immediately if public,
    // PENDING (awaiting the creator) if private. Used from a search result
    // or the league detail screen's Join button.
    @PostMapping("/{id}/join")
    public ResponseEntity<LeagueResponse> joinLeague(@PathVariable Integer id) {
        return ResponseEntity.ok(leagueService.joinLeague(id, currentEmail()));
    }

    // Works for either kind of league - ACTIVE immediately if public,
    // PENDING (awaiting the creator) if private.
    @PostMapping("/join/{code}")
    public ResponseEntity<LeagueResponse> joinByCode(@PathVariable String code) {
        return ResponseEntity.ok(leagueService.joinByCode(code, currentEmail()));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<LeagueMemberResponse>> getMembers(@PathVariable Integer id) {
        return ResponseEntity.ok(leagueService.getMembers(id, currentEmail()));
    }

    // Owner-only - who's waiting to be let into a private league.
    @GetMapping("/{id}/requests")
    public ResponseEntity<List<LeagueMemberResponse>> getPendingRequests(@PathVariable Integer id) {
        return ResponseEntity.ok(leagueService.getPendingRequests(id, currentEmail()));
    }

    @PatchMapping("/{id}/requests/{userId}/accept")
    public ResponseEntity<Void> acceptRequest(@PathVariable Integer id, @PathVariable Integer userId) {
        leagueService.acceptRequest(id, userId, currentEmail());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/requests/{userId}/reject")
    public ResponseEntity<Void> rejectRequest(@PathVariable Integer id, @PathVariable Integer userId) {
        leagueService.rejectRequest(id, userId, currentEmail());
        return ResponseEntity.noContent().build();
    }

    // Self-leave. The creator can't leave their own league this way (has to
    // delete it instead) - enforced in LeagueService.
    @DeleteMapping("/{id}/membership")
    public ResponseEntity<Void> leaveLeague(@PathVariable Integer id) {
        leagueService.leaveLeague(id, currentEmail());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLeague(@PathVariable Integer id) {
        leagueService.deleteLeague(id, currentEmail());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/leaderboard/predictions")
    public ResponseEntity<List<LeagueLeaderboardEntry>> getPredictionLeaderboard(@PathVariable Integer id) {
        return ResponseEntity.ok(leagueService.getPredictionLeaderboard(id, currentEmail()));
    }

    @GetMapping("/{id}/leaderboard/fantasy")
    public ResponseEntity<List<LeagueLeaderboardEntry>> getFantasyLeaderboard(@PathVariable Integer id) {
        return ResponseEntity.ok(leagueService.getFantasyLeaderboard(id, currentEmail()));
    }
}
