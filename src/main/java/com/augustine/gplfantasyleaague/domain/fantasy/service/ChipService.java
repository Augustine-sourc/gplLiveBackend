package com.augustine.gplfantasyleaague.domain.fantasy.service;

import com.augustine.gplfantasyleaague.domain.auth.entity.User;
import com.augustine.gplfantasyleaague.domain.auth.repository.UserRepository;
import com.augustine.gplfantasyleaague.domain.fantasy.dto.ChipRequest;
import com.augustine.gplfantasyleaague.domain.fantasy.entity.*;
import com.augustine.gplfantasyleaague.domain.fantasy.repository.ChipRepository;
import com.augustine.gplfantasyleaague.domain.fantasy.repository.FantasyTeamPlayerRepository;
import com.augustine.gplfantasyleaague.domain.fantasy.repository.FantasyTeamRepository;
import com.augustine.gplfantasyleaague.domain.fantasy.repository.FreeHitSnapShotRepository;
import com.augustine.gplfantasyleaague.domain.gameweek.entity.Gameweek;
import com.augustine.gplfantasyleaague.domain.gameweek.repository.GameweekRepository;
import com.augustine.gplfantasyleaague.domain.fantasy.dto.ChipResponse;
import com.augustine.gplfantasyleaague.domain.scoring.repository.FantasyTeamGameWeekRepository;
import com.augustine.gplfantasyleaague.domain.scoring.service.ScoringService;
import com.augustine.gplfantasyleaague.exception.InvalidSquadException;
import com.augustine.gplfantasyleaague.exception.ResourceNotFoundException;
import com.augustine.gplfantasyleaague.exception.UnauthorizedAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ChipService {
    // Matches official FPL rules: Bench Boost and Triple Captain are just a
    // flag on your saved team, so they can be cancelled any time up to the
    // Gameweek deadline. Wildcard and Free Hit are tied to a transfer
    // transaction (and, here, a squad/budget snapshot for Free Hit) that's
    // final the moment it's confirmed - neither can be undone.
    private static final Set<ChipType> CANCELLABLE_CHIP_TYPES = EnumSet.of(ChipType.BENCH_BOOST, ChipType.TRIPLE_CAPTAIN);

    private final FantasyTeamRepository fantasyTeamRepository;
    private final GameweekRepository gameweekRepository;
    private final UserRepository userRepository;
    private final ChipRepository chipRepository;
    private final FantasyTeamPlayerRepository fantasyTeamPlayerRepository;
    private final FreeHitSnapShotRepository freeHitSnapShotRepository;
    private final FantasyTeamGameWeekRepository fantasyTeamGameWeekRepository;
    private final ScoringService scoringService;

    public ChipService(FantasyTeamRepository fantasyTeamRepository, GameweekRepository gameweekRepository, UserRepository userRepository, ChipRepository chipRepository, FantasyTeamPlayerRepository fantasyTeamPlayerRepository, FreeHitSnapShotRepository freeHitSnapShotRepository, FantasyTeamGameWeekRepository fantasyTeamGameWeekRepository, ScoringService scoringService) {
        this.fantasyTeamRepository = fantasyTeamRepository;
        this.gameweekRepository = gameweekRepository;
        this.userRepository = userRepository;
        this.chipRepository = chipRepository;
        this.fantasyTeamPlayerRepository = fantasyTeamPlayerRepository;
        this.freeHitSnapShotRepository = freeHitSnapShotRepository;
        this.fantasyTeamGameWeekRepository = fantasyTeamGameWeekRepository;
        this.scoringService = scoringService;
    }

    public ChipResponse activateTripleCaptain(ChipRequest request, String email){
        User user = userRepository.findByEmail(email).orElseThrow(()-> new ResourceNotFoundException("User not found"));
        FantasyTeam team = fantasyTeamRepository.findById(request.getFantasyTeamId()).orElseThrow(()-> new ResourceNotFoundException("Team not found"));
        Gameweek gameweek = gameweekRepository.findById(request.getGameweekId()).orElseThrow(()-> new ResourceNotFoundException("Gameweek not found"));

        if(!team.getUser().getId().equals(user.getId())){
            throw new UnauthorizedAccessException("This team doesn't belong to you");
        }

        if(gameweek.getDeadline().isBefore(LocalDateTime.now())){
            // Not "the gameweek has ended" - fixtures for this gameweek can
            // still be days from finishing. This is the pre-kickoff lock
            // (same one cancelChip below checks), so say that instead.
            throw new InvalidSquadException("The Gameweek deadline has passed - chips lock once the first match kicks off.");
        }

        if(chipRepository.existsByFantasyTeamIdAndChipType(team.getId(), ChipType.TRIPLE_CAPTAIN)){
            throw new InvalidSquadException("You have already used triple captain for this season");
        }

        Optional<Chip> activeChip = chipRepository.findByFantasyTeamIdAndGameweekId(team.getId(), gameweek.getId());

        if(activeChip.isPresent()){
            throw new InvalidSquadException("You have already used a chip for this Gameweek");
        }
        Chip saveChip = saveToChipdatabase(team,gameweek,ChipType.TRIPLE_CAPTAIN);
        return mapToResponse(saveChip);
    }

    public ChipResponse activateBenchBoost(ChipRequest request, String email){
        User user = userRepository.findByEmail(email).orElseThrow(()-> new ResourceNotFoundException("User not found"));
        FantasyTeam team = fantasyTeamRepository.findById(request.getFantasyTeamId()).orElseThrow(()-> new ResourceNotFoundException("Team not found"));
        Gameweek gameweek = gameweekRepository.findById(request.getGameweekId()).orElseThrow(()-> new ResourceNotFoundException("Gameweek not found"));

        if(!team.getUser().getId().equals(user.getId())){
            throw new UnauthorizedAccessException("This team doesn't belong to you");
        }

        if(gameweek.getDeadline().isBefore(LocalDateTime.now())){
            // Not "the gameweek has ended" - fixtures for this gameweek can
            // still be days from finishing. This is the pre-kickoff lock
            // (same one cancelChip below checks), so say that instead.
            throw new InvalidSquadException("The Gameweek deadline has passed - chips lock once the first match kicks off.");
        }

        if(chipRepository.existsByFantasyTeamIdAndChipType(team.getId(), ChipType.BENCH_BOOST)){
            throw new InvalidSquadException("You have already use Bench Boost for this season");
        }

        Optional<Chip> activeChip = chipRepository.findByFantasyTeamIdAndGameweekId(team.getId(), gameweek.getId());

        if(activeChip.isPresent()){
            throw new InvalidSquadException("You have already used a chip for this Gameweek");
        }
        Chip saveChip = saveToChipdatabase(team,gameweek,ChipType.BENCH_BOOST);
        return mapToResponse(saveChip);
    }

    public ChipResponse activateWildCard(ChipRequest request, String email){
        User user = userRepository.findByEmail(email).orElseThrow(()-> new ResourceNotFoundException("User not found"));
        FantasyTeam team = fantasyTeamRepository.findById(request.getFantasyTeamId()).orElseThrow(()-> new ResourceNotFoundException("Team not found"));
        Gameweek gameweek = gameweekRepository.findById(request.getGameweekId()).orElseThrow(()-> new ResourceNotFoundException("Gameweek not found"));

        if(!team.getUser().getId().equals(user.getId())){
            throw new UnauthorizedAccessException("This team doesn't belong to you");
        }

        if(gameweek.getDeadline().isBefore(LocalDateTime.now())){
            // Not "the gameweek has ended" - fixtures for this gameweek can
            // still be days from finishing. This is the pre-kickoff lock
            // (same one cancelChip below checks), so say that instead.
            throw new InvalidSquadException("The Gameweek deadline has passed - chips lock once the first match kicks off.");
        }

        if(chipRepository.existsByFantasyTeamIdAndChipType(team.getId(), ChipType.WILDCARD)){
            throw new InvalidSquadException("You have already use Wildcard for this specified time");
        }

        Optional<Chip> activeChip = chipRepository.findByFantasyTeamIdAndGameweekId(team.getId(), gameweek.getId());

        if(activeChip.isPresent()){
            throw new InvalidSquadException("You have already used a chip for this Gameweek");
        }

        // For Wildcard 1
        if(gameweek.getGameweekNumber() > 19){
            throw new InvalidSquadException("Wildcard 1 can only be used in gameweeks 1-19");
        }

        Chip saveChip = saveToChipdatabase(team,gameweek,ChipType.WILDCARD);
        return mapToResponse(saveChip);
    }

    public ChipResponse activateWildCard2(ChipRequest request, String email){
        User user = userRepository.findByEmail(email).orElseThrow(()-> new ResourceNotFoundException("User not found"));
        FantasyTeam team = fantasyTeamRepository.findById(request.getFantasyTeamId()).orElseThrow(()-> new ResourceNotFoundException("Team not found"));
        Gameweek gameweek = gameweekRepository.findById(request.getGameweekId()).orElseThrow(()-> new ResourceNotFoundException("Gameweek not found"));

        if(!team.getUser().getId().equals(user.getId())){
            throw new UnauthorizedAccessException("This team doesn't belong to you");
        }

        if(gameweek.getDeadline().isBefore(LocalDateTime.now())){
            // Not "the gameweek has ended" - fixtures for this gameweek can
            // still be days from finishing. This is the pre-kickoff lock
            // (same one cancelChip below checks), so say that instead.
            throw new InvalidSquadException("The Gameweek deadline has passed - chips lock once the first match kicks off.");
        }


        if(chipRepository.existsByFantasyTeamIdAndChipType(team.getId(), ChipType.WILDCARD_2)){
            throw new InvalidSquadException("You have already use Wildcard for this season");
        }

        Optional<Chip> activeChip = chipRepository.findByFantasyTeamIdAndGameweekId(team.getId(), gameweek.getId());

        if(activeChip.isPresent()){
            throw new InvalidSquadException("You have already used a chip for this Gameweek");
        }

        // For Wildcard 2
        if(gameweek.getGameweekNumber() <= 19){
            throw new InvalidSquadException("Wildcard 2 can only be used in gameweeks 20-38");
        }

        Chip saveChip = saveToChipdatabase(team,gameweek,ChipType.WILDCARD_2);
        return mapToResponse(saveChip);
    }

    public ChipResponse activateFreeHit(ChipRequest request, String email){
        User user = userRepository.findByEmail(email).orElseThrow(()-> new ResourceNotFoundException("Email not found"));
        FantasyTeam team = fantasyTeamRepository.findById(request.getFantasyTeamId()).orElseThrow(()-> new ResourceNotFoundException("Team not found"));
        Gameweek gameweek = gameweekRepository.findById(request.getGameweekId()).orElseThrow(()-> new ResourceNotFoundException("Gameweek not found"));

        if(!team.getUser().getId().equals(user.getId())){
            throw new UnauthorizedAccessException("This team doesn't belong to you");
        }

        if(gameweek.getDeadline().isBefore(LocalDateTime.now())){
            // Not "the gameweek has ended" - fixtures for this gameweek can
            // still be days from finishing. This is the pre-kickoff lock
            // (same one cancelChip below checks), so say that instead.
            throw new InvalidSquadException("The Gameweek deadline has passed - chips lock once the first match kicks off.");
        }

        if(chipRepository.existsByFantasyTeamIdAndChipType(team.getId(), ChipType.FREEHIT)){
            throw new InvalidSquadException("You have already used Free Hit for this season");
        }

        Optional<Chip> activeChip = chipRepository.findByFantasyTeamIdAndGameweekId(team.getId(), gameweek.getId());

        if(activeChip.isPresent()){
            throw new InvalidSquadException("You have already used a chip for this Gameweek");
        }

        fantasyTeamPlayerRepository.findByFantasyTeamId(team.getId())
                .forEach(player -> mapToFreeHit(player, gameweek));

        team.setFreeHitBudgetSnapshot(team.getBudgetRemaining());
        fantasyTeamRepository.save(team);


        Chip saveChip = saveToChipdatabase(team,gameweek,ChipType.FREEHIT);
        return mapToResponse(saveChip);
    }

    @Transactional
    public ChipResponse restoreFreeHit(Integer fantasyTeamId, Integer gameweekId){
        FantasyTeam team = fantasyTeamRepository.findById(fantasyTeamId).orElseThrow(()-> new ResourceNotFoundException("Team not found"));
        Gameweek gameweek = gameweekRepository.findById(gameweekId).orElseThrow(()-> new ResourceNotFoundException("Gameweek not found"));


        List<FreeHitSnapShot> freeHitSnapShotList = freeHitSnapShotRepository.findByFantasyTeamIdAndGameweekId(team.getId(), gameweek.getId());


        if(freeHitSnapShotList.isEmpty() || team.getFreeHitBudgetSnapshot() == null){
            throw new ResourceNotFoundException(
                    "No valid Free Hit snapshot found for team " + team.getId() + " in gameweek " + gameweek.getId() +
                            ". Skipping restore to avoid wiping the current squad.");
        }

        fantasyTeamPlayerRepository.deleteByFantasyTeamId(team.getId());

        freeHitSnapShotList.stream()
                .forEach(freeHitSnapShot -> mapToFantasyTeamPlayer(freeHitSnapShot));
        team.setBudgetRemaining(team.getFreeHitBudgetSnapshot());
        team.setFreeHitBudgetSnapshot(null);
        fantasyTeamRepository.save(team);
        freeHitSnapShotRepository.deleteByFantasyTeamIdAndGameweekId(team.getId(), gameweek.getId());

        Chip saveChip = chipRepository.findByFantasyTeamIdAndGameweekId(team.getId(), gameweek.getId()).orElseThrow(() -> new ResourceNotFoundException("Free Hit chip not found"));

        return mapToResponse(saveChip);
    }

    // Undo a Bench Boost or Triple Captain before the Gameweek locks -
    // same rule the real FPL uses (those two are just a flag on your saved
    // team; Wildcard/Free Hit are tied to a transfer that's final once
    // confirmed, so they're deliberately excluded here).
    @Transactional
    public void cancelChip(Integer fantasyTeamId, Integer gameweekId, String email){
        User user = userRepository.findByEmail(email).orElseThrow(()-> new ResourceNotFoundException("User not found"));
        FantasyTeam team = fantasyTeamRepository.findById(fantasyTeamId).orElseThrow(()-> new ResourceNotFoundException("Team not found"));
        Gameweek gameweek = gameweekRepository.findById(gameweekId).orElseThrow(()-> new ResourceNotFoundException("Gameweek not found"));

        if(!team.getUser().getId().equals(user.getId())){
            throw new UnauthorizedAccessException("This team doesn't belong to you");
        }

        Chip chip = chipRepository.findByFantasyTeamIdAndGameweekId(team.getId(), gameweek.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No chip is active for this team in this Gameweek"));

        if(!CANCELLABLE_CHIP_TYPES.contains(chip.getChipType())){
            throw new InvalidSquadException(chip.getChipType() == ChipType.FREEHIT
                    ? "Free Hit can't be cancelled once played."
                    : "Wildcard can't be cancelled once played.");
        }

        if(gameweek.getDeadline().isBefore(LocalDateTime.now())){
            throw new InvalidSquadException("The Gameweek deadline has passed - this chip can no longer be cancelled.");
        }

        chipRepository.delete(chip);

        // Scoring normally only runs after the deadline (once results come
        // in), so this is usually a no-op - but if a score for this team's
        // Gameweek was already computed for any reason, recompute it now
        // that the chip's multiplier/bench-boost effect is gone.
        if(fantasyTeamGameWeekRepository.findByFantasyTeamIdAndGameweekId(team.getId(), gameweek.getId()).isPresent()){
            scoringService.calculateGameweekScore(team.getId(), gameweek.getId());
        }
    }


    //helper methods
    private Chip saveToChipdatabase(FantasyTeam team, Gameweek gameweek, ChipType type){
        Chip saveChip = Chip.builder()
                .fantasyTeam(team)
                .gameweek(gameweek)
                .chipType(type)
                .usedAt(LocalDateTime.now())
                .build();
        chipRepository.save(saveChip);
        return saveChip;
    }

    private ChipResponse mapToResponse(Chip chip){
        return ChipResponse.builder()
                .id(chip.getId())
                .fantasyTeamName(chip.getFantasyTeam().getTeamName())
                .chipType(chip.getChipType())
                .usedAt(chip.getUsedAt())
                .gameweekNumber(chip.getGameweek().getGameweekNumber())
                .build();
    }

    private FreeHitSnapShot mapToFreeHit(FantasyTeamPlayer player, Gameweek gameweek){
        FreeHitSnapShot freeHitSnapShot =  FreeHitSnapShot.builder()
                .isCaptain(player.getIsCaptain())
                .currentPrice(player.getCurrentPrice())
                .purchasePrice(player.getPurchasePrice())
                .isPartOfXI(player.getIsPartOfXI())
                .isViceCaptain(player.getIsViceCaptain())
                .player(player.getPlayer())
                .gameweek(gameweek)
                .fantasyTeam(player.getFantasyTeam())
                .build();
        freeHitSnapShotRepository.save(freeHitSnapShot);
        return freeHitSnapShot;
    }

    private FantasyTeamPlayer mapToFantasyTeamPlayer(FreeHitSnapShot freeHitSnapShot){
        FantasyTeamPlayer fantasyTeamPlayer = FantasyTeamPlayer.builder()
                .fantasyTeam(freeHitSnapShot.getFantasyTeam())
                .isCaptain(freeHitSnapShot.getIsCaptain())
                .isPartOfXI(freeHitSnapShot.getIsPartOfXI())
                .isViceCaptain(freeHitSnapShot.getIsViceCaptain())
                .purchasePrice(freeHitSnapShot.getPurchasePrice())
                .currentPrice(freeHitSnapShot.getCurrentPrice())
                .player(freeHitSnapShot.getPlayer())
                .build();
        fantasyTeamPlayerRepository.save(fantasyTeamPlayer);
        return fantasyTeamPlayer;
    }
}

