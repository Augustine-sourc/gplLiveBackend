package com.augustine.gplfantasyleaague.domain.gameweek.service;

import com.augustine.gplfantasyleaague.domain.gameweek.dtos.GameweekRequest;
import com.augustine.gplfantasyleaague.domain.gameweek.dtos.GameweekResponse;
import com.augustine.gplfantasyleaague.domain.gameweek.entity.Gameweek;
import com.augustine.gplfantasyleaague.domain.gameweek.repository.GameweekRepository;
import com.augustine.gplfantasyleaague.exception.GameweekAlreadyExistsException;
import com.augustine.gplfantasyleaague.exception.InvalidGameweekException;
import com.augustine.gplfantasyleaague.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GameweekService {
    private final GameweekRepository gameweekRepository;

    public GameweekService(GameweekRepository gameweekRepository) {
        this.gameweekRepository = gameweekRepository;
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

    // Catches the class of admin data-entry mistake that caused GW1's
    // deadline (18:25) to already be in the past relative to real time by
    // the time anyone tried to use it, even though the gameweek itself
    // (start_date -> end_date) was still very much current. Deliberately
    // only applied on creation - not wired into any update/PUT path - since
    // fixing an already-broken historical gameweek (like GW34's bad
    // end_date) legitimately requires setting dates that wouldn't pass a
    // "deadline must be in the future" check.
    private void validateGameweekDates(GameweekRequest request){
        if (!request.getStartDate().isBefore(request.getEndDate())) {
            throw new InvalidGameweekException("Start date must be before end date");
        }
        if (request.getDeadline().isAfter(request.getStartDate())) {
            throw new InvalidGameweekException("Deadline must be at or before the start date - it locks transfers/chips before matches kick off, not after");
        }
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
