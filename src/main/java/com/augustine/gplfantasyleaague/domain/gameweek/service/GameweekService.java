package com.augustine.gplfantasyleaague.domain.gameweek.service;

import com.augustine.gplfantasyleaague.domain.fantasy.repository.ChipRepository;
import com.augustine.gplfantasyleaague.domain.fantasy.repository.FreeHitSnapShotRepository;
import com.augustine.gplfantasyleaague.domain.fantasy.repository.TransferRepository;
import com.augustine.gplfantasyleaague.domain.gameweek.dtos.GameweekRequest;
import com.augustine.gplfantasyleaague.domain.gameweek.dtos.GameweekResponse;
import com.augustine.gplfantasyleaague.domain.gameweek.entity.Gameweek;
import com.augustine.gplfantasyleaague.domain.gameweek.repository.FixtureRepository;
import com.augustine.gplfantasyleaague.domain.gameweek.repository.GameweekRepository;
import com.augustine.gplfantasyleaague.domain.player.repository.PlayerPriceRepository;
import com.augustine.gplfantasyleaague.domain.scoring.repository.FantasyTeamGameWeekRepository;
import com.augustine.gplfantasyleaague.exception.GameweekAlreadyExistsException;
import com.augustine.gplfantasyleaague.exception.InvalidGameweekException;
import com.augustine.gplfantasyleaague.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GameweekService {
    private final GameweekRepository gameweekRepository;
    private final FixtureRepository fixtureRepository;
    private final ChipRepository chipRepository;
    private final TransferRepository transferRepository;
    private final PlayerPriceRepository playerPriceRepository;
    private final FantasyTeamGameWeekRepository fantasyTeamGameWeekRepository;
    private final FreeHitSnapShotRepository freeHitSnapShotRepository;

    public GameweekService(GameweekRepository gameweekRepository, FixtureRepository fixtureRepository,
                            ChipRepository chipRepository, TransferRepository transferRepository,
                            PlayerPriceRepository playerPriceRepository,
                            FantasyTeamGameWeekRepository fantasyTeamGameWeekRepository,
                            FreeHitSnapShotRepository freeHitSnapShotRepository) {
        this.gameweekRepository = gameweekRepository;
        this.fixtureRepository = fixtureRepository;
        this.chipRepository = chipRepository;
        this.transferRepository = transferRepository;
        this.playerPriceRepository = playerPriceRepository;
        this.fantasyTeamGameWeekRepository = fantasyTeamGameWeekRepository;
        this.freeHitSnapShotRepository = freeHitSnapShotRepository;
    }

    private GameweekResponse mapToResponse(Gameweek gameweek){
        return GameweekResponse.builder()
                .id(gameweek.getId())
                .season(gameweek.getSeason())
                .startDate(gameweek.getStartDate())
                .endDate(gameweek.getEndDate())
                .deadline(gameweek.getDeadline())
                .isCurrent(gameweek.getIsCurrent())
                .gameweekNumber(gameweek.getGameweekNumber())
                .build();

    }

    public List<GameweekResponse> getAllGameweeks(){
        return gameweekRepository.findAll().stream()
                .map(gameweek -> mapToResponse(gameweek) )
                .toList();
    }

    public GameweekResponse getCurrentGameweek(){
        Gameweek gameweek = gameweekRepository.findByIsCurrentTrue().orElseThrow(()-> new ResourceNotFoundException("No current Gameweek is active"));
        return mapToResponse(gameweek);
    }

    public List<GameweekResponse> getGameweekBySeason(String season){
        return gameweekRepository.findBySeason(season).stream()
                .map(gameweek -> mapToResponse(gameweek))
                .toList();
    }

    public GameweekResponse createGameweek(GameweekRequest request){
        Gameweek saveGameweek = saveToDatabase(request);
        return mapToResponse(saveGameweek);
    }

    public GameweekResponse setCurrentGameweek(Integer id){
        //find and deactivate the current gameweek
        gameweekRepository.findByIsCurrentTrue().ifPresent(current -> {
            current.setIsCurrent(false);
            gameweekRepository.save(current);
        });

        // activate the new one
        Gameweek gameweek = gameweekRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gameweek with ID " + id + " not found"));
        gameweek.setIsCurrent(true);
        gameweekRepository.save(gameweek);

        return mapToResponse(gameweek);
    }

    // Edits an existing gameweek in place - the app-facing alternative to
    // the raw SQL UPDATEs this has needed so far (e.g. fixing GW34's bad
    // end_date, or GW1's deadline having already passed by the time anyone
    // tried to use a chip against it).
    public GameweekResponse updateGameweek(Integer id, GameweekRequest request){
        Gameweek gameweek = gameweekRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gameweek with ID " + id + " not found"));

        validateDateOrdering(request);

        // Same season+number duplicate guard as creation, but excluding
        // this row itself - otherwise saving a gameweek without changing
        // its season/number would immediately collide with itself.
        gameweekRepository.findBySeasonAndGameweekNumber(request.getSeason(), request.getGameweekNumber())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new GameweekAlreadyExistsException(
                            "Gameweek " + request.getGameweekNumber() + " for season " + request.getSeason() + " already exists");
                });

        gameweek.setSeason(request.getSeason());
        gameweek.setGameweekNumber(request.getGameweekNumber());
        gameweek.setStartDate(request.getStartDate());
        gameweek.setEndDate(request.getEndDate());
        gameweek.setDeadline(request.getDeadline());
        gameweekRepository.save(gameweek);

        return mapToResponse(gameweek);
    }

    // The app-facing alternative to the raw SQL DELETEs this session has
    // needed for the duplicate GW1 rows. Deliberately conservative: refuses
    // to delete the current gameweek (the app would have no current
    // gameweek at all afterwards) and refuses to delete anything that
    // already has real data attached, rather than relying on the DB's mixed
    // CASCADE/RESTRICT behavior across fixtures/prices/scores (CASCADE) vs
    // chips/transfers/free_hit_snapshots (RESTRICT) - CASCADE in particular
    // would silently wipe out real fixture/score history with no warning.
    public void deleteGameweek(Integer id){
        Gameweek gameweek = gameweekRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gameweek with ID " + id + " not found"));

        if (Boolean.TRUE.equals(gameweek.getIsCurrent())) {
            throw new InvalidGameweekException("Cannot delete the current gameweek - set a different gameweek as current first");
        }

        boolean hasDependents = fixtureRepository.existsByGameweekId(id)
                || chipRepository.existsByGameweekId(id)
                || transferRepository.existsByGameweekId(id)
                || playerPriceRepository.existsByGameweekId(id)
                || fantasyTeamGameWeekRepository.existsByGameweekId(id)
                || freeHitSnapShotRepository.existsByGameweekId(id);

        if (hasDependents) {
            throw new InvalidGameweekException(
                    "Cannot delete this gameweek - it already has fixtures, chips, transfers, prices, or scores recorded against it");
        }

        gameweekRepository.delete(gameweek);
    }

    // Ordering checks that must always hold, regardless of whether real
    // time has moved past these dates yet - applied on both create and
    // update.
    private void validateDateOrdering(GameweekRequest request){
        if (!request.getStartDate().isBefore(request.getEndDate())) {
            throw new InvalidGameweekException("Start date must be before end date");
        }
        if (request.getDeadline().isAfter(request.getStartDate())) {
            throw new InvalidGameweekException("Deadline must be at or before the start date - it locks transfers/chips before matches kick off, not after");
        }
    }

    // Catches the class of admin data-entry mistake that caused GW1's
    // deadline (18:25) to already be in the past relative to real time by
    // the time anyone tried to use it, even though the gameweek itself
    // (start_date -> end_date) was still very much current. Only applied on
    // creation, not update - fixing an already-broken historical gameweek
    // (like GW34's bad end_date) legitimately requires setting dates that
    // wouldn't pass a "deadline must be in the future" check.
    private void validateGameweekDates(GameweekRequest request){
        validateDateOrdering(request);
        if (request.getDeadline().isBefore(LocalDateTime.now())) {
            throw new InvalidGameweekException("Deadline can't be in the past - this gameweek would be unusable (chips/transfers already locked) the moment it's created");
        }
    }

    private Gameweek saveToDatabase(GameweekRequest request){
        validateGameweekDates(request);

        gameweekRepository.findBySeasonAndGameweekNumber(request.getSeason(), request.getGameweekNumber())
                .ifPresent(existing -> {
                    throw new GameweekAlreadyExistsException(
                            "Gameweek " + request.getGameweekNumber() + " for season " + request.getSeason() + " already exists");
                });

        Gameweek savedGameweek = Gameweek.builder()
                .season(request.getSeason())
                .gameweekNumber(request.getGameweekNumber())
                .deadline(request.getDeadline())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();
        gameweekRepository.save(savedGameweek);
        return savedGameweek;
    }

}
