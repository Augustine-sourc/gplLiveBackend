package com.augustine.gplfantasyleaague.domain.fantasy.service;

import com.augustine.gplfantasyleaague.domain.auth.entity.Role;
import com.augustine.gplfantasyleaague.domain.auth.entity.User;
import com.augustine.gplfantasyleaague.domain.auth.repository.UserRepository;
import com.augustine.gplfantasyleaague.domain.fantasy.dto.FantasyTeamRequest;
import com.augustine.gplfantasyleaague.domain.fantasy.dto.FantasyTeamResponse;
import com.augustine.gplfantasyleaague.domain.fantasy.entity.Chip;
import com.augustine.gplfantasyleaague.domain.fantasy.entity.ChipType;
import com.augustine.gplfantasyleaague.domain.fantasy.entity.FantasyTeam;
import com.augustine.gplfantasyleaague.domain.fantasy.repository.ChipRepository;
import com.augustine.gplfantasyleaague.domain.fantasy.repository.FantasyTeamPlayerRepository;
import com.augustine.gplfantasyleaague.domain.fantasy.repository.FantasyTeamRepository;
import com.augustine.gplfantasyleaague.domain.fantasy.repository.FreeHitSnapShotRepository;
import com.augustine.gplfantasyleaague.domain.fantasy.repository.TransferRepository;
import com.augustine.gplfantasyleaague.domain.gameweek.repository.GameweekRepository;
import com.augustine.gplfantasyleaague.domain.league.repository.LeagueMembershipRepository;
import com.augustine.gplfantasyleaague.domain.scoring.repository.FantasyTeamGameWeekRepository;
import com.augustine.gplfantasyleaague.exception.ResourceNotFoundException;
import com.augustine.gplfantasyleaague.exception.TeamCreationException;
import com.augustine.gplfantasyleaague.exception.UnauthorizedAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FantasyTeamService {
    private final UserRepository userRepository;
    private final FantasyTeamRepository fantasyTeamRepository;
    private final FantasyTeamPlayerRepository fantasyTeamPlayerRepository;
    private final TransferRepository transferRepository;
    private final ChipRepository chipRepository;
    private final FantasyTeamGameWeekRepository fantasyTeamGameWeekRepository;
    private final GameweekRepository gameweekRepository;
    private final FreeHitSnapShotRepository freeHitSnapShotRepository;
    private final LeagueMembershipRepository leagueMembershipRepository;


    public FantasyTeamService(UserRepository userRepository, FantasyTeamRepository fantasyTeamRepository,
                               FantasyTeamPlayerRepository fantasyTeamPlayerRepository, TransferRepository transferRepository,
                               ChipRepository chipRepository, FantasyTeamGameWeekRepository fantasyTeamGameWeekRepository,
                               GameweekRepository gameweekRepository, FreeHitSnapShotRepository freeHitSnapShotRepository,
                               LeagueMembershipRepository leagueMembershipRepository) {
        this.userRepository = userRepository;
        this.fantasyTeamRepository = fantasyTeamRepository;
        this.fantasyTeamPlayerRepository = fantasyTeamPlayerRepository;
        this.transferRepository = transferRepository;
        this.chipRepository = chipRepository;
        this.fantasyTeamGameWeekRepository = fantasyTeamGameWeekRepository;
        this.gameweekRepository = gameweekRepository;
        this.freeHitSnapShotRepository = freeHitSnapShotRepository;
        this.leagueMembershipRepository = leagueMembershipRepository;
    }

    public FantasyTeamResponse createFantasyTeam(FantasyTeamRequest request, String email){
        User user = userRepository.findByEmail(email).orElseThrow(()-> new ResourceNotFoundException("User not found"));
        if(fantasyTeamRepository.existsByUserId(user.getId())){
            throw new TeamCreationException("You already have a fantasy team");
        }

        if(fantasyTeamRepository.existsByTeamName(request.getTeamName())){
            throw new TeamCreationException("Team name '" + request.getTeamName() + "' already exists. Please choose another name");
        }

        FantasyTeam savedTeam = saveToDatabase(request, user);
        return mapToFantasyResponse(savedTeam);
    }

    public FantasyTeamResponse getFantasyTeamByUser(String email){
        User user = userRepository.findByEmail(email).orElseThrow(()-> new ResourceNotFoundException("User not found"));
        FantasyTeam fantasyTeam = fantasyTeamRepository.findByUserId(user.getId()).orElseThrow(()-> new ResourceNotFoundException("Fantasy team configuration not found for this profile"));
        return mapToFantasyResponse(fantasyTeam);
    }

    public FantasyTeamResponse getFantasyTeamById(Integer id, String email){
        FantasyTeam team = fantasyTeamRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Team with ID " + id + " not found"));
        User user = userRepository.findByEmail(email).orElseThrow(()-> new ResourceNotFoundException("User not found"));

        boolean isOwner = team.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;
        if(!isOwner && !isAdmin){
            throw new UnauthorizedAccessException("You are not authorized to view this team");
        }

        return mapToFantasyResponse(team);
    }

    public List<FantasyTeamResponse> getAllFantasyTeams(){
        return fantasyTeamRepository.findAll().stream()
                .map(team-> mapToFantasyResponse(team))
                .toList();
    }

    public FantasyTeamResponse updateTeamName(Integer id, FantasyTeamRequest request, String email){
        FantasyTeam team = fantasyTeamRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Team ID configuration missing"));
        User user = userRepository.findByEmail(email).orElseThrow(()-> new ResourceNotFoundException("User context not found"));

        if(!team.getUser().getId().equals(user.getId())){
            throw new UnauthorizedAccessException("You are not authorized to update this team configuration");
        }
        team.setTeamName(request.getTeamName());
        team.setUpdatedAt(LocalDateTime.now());

        FantasyTeam updatedTeam = fantasyTeamRepository.save(team);
        return mapToFantasyResponse(updatedTeam);
    }

    // Deletes children before the team row itself so this works regardless
    // of whether the DB's ON DELETE CASCADE (present on fantasy_team_players/
    // transfers/chips/fantasy_team_gameweek_scores/free_hit_snapshots per the
    // Flyway migrations) is relied on or not - explicit deletes here mean
    // Hibernate's persistence context stays in sync within this same
    // transaction too, not just the database underneath it. free_hit_snapshots
    // (V29) used to be skipped here because, at the time, no migration
    // created that table at all and touching it threw "relation
    // free_hit_snapshots does not exist" - V29 has existed for a while now,
    // so it's included below like everything else. Lets a user fully reset
    // and rebuild a squad from scratch, since createFantasyTeam otherwise
    // permanently blocks a second team per user.
    @Transactional
    public void deleteMyFantasyTeam(String email){
        User user = userRepository.findByEmail(email).orElseThrow(()-> new ResourceNotFoundException("User not found"));
        FantasyTeam team = fantasyTeamRepository.findByUserId(user.getId())
                .orElseThrow(()-> new ResourceNotFoundException("You don't have a fantasy team to delete"));

        // User.fantasyTeam is a @OneToOne(mappedBy = "user") - eagerly
        // loaded, so userRepository.findByEmail above already pulled this
        // exact FantasyTeam instance into the persistence context via the
        // User side too. Once fantasyTeamRepository.delete(team) marks it
        // removed, Hibernate's flush-time consistency check still finds
        // `user.fantasyTeam` pointing at that same (now-removed) instance
        // and throws TransientPropertyValueException, since it looks
        // unresolved from that association's point of view. Breaking the
        // in-memory back-reference first avoids the check entirely - it's
        // the inverse side of the relationship, so this doesn't need its
        // own save/column update.
        user.setFantasyTeam(null);

        Integer teamId = team.getId();
        fantasyTeamPlayerRepository.deleteByFantasyTeamId(teamId);
        transferRepository.deleteByFantasyTeamId(teamId);
        chipRepository.deleteByFantasyTeamId(teamId);
        fantasyTeamGameWeekRepository.deleteByFantasyTeamId(teamId);
        freeHitSnapShotRepository.deleteByFantasyTeamId(teamId);
        // Leagues now require a Fantasy team to be an active member (see
        // LeagueService.requireHasFantasyTeam) - without one there's no
        // Fantasy points to rank, so drop every membership row this user
        // has (ACTIVE and PENDING alike), in any league, including one they
        // created. This is keyed by user, not team, and is separate from
        // League.creator (its own FK to User) - a league they own keeps
        // existing and they keep admin rights over it (accept/reject/
        // delete), they just fall off its own leaderboard until they
        // rebuild a squad and rejoin.
        leagueMembershipRepository.deleteByUserId(user.getId());
        fantasyTeamRepository.delete(team);
    }

    // Standard fantasy-football rule: unused free transfers carry over, but
    // never past a bank of 2. Called by GameweekScheduler each time a new
    // gameweek becomes current, so every team gets its weekly allowance
    // back regardless of how many transfers they made (or didn't) the week
    // before.
    private static final int MAX_BANKED_FREE_TRANSFERS = 2;

    @Transactional
    public void grantWeeklyFreeTransfers(){
        List<FantasyTeam> teams = fantasyTeamRepository.findAll();
        for (FantasyTeam team : teams) {
            team.setTransferPoints(Math.min(team.getTransferPoints() + 1, MAX_BANKED_FREE_TRANSFERS));
        }
        fantasyTeamRepository.saveAll(teams);
    }

    private FantasyTeam saveToDatabase(FantasyTeamRequest request, User user){
        FantasyTeam savedFantasyTeam =  FantasyTeam.builder()
                    .teamName(request.getTeamName())
                    .user(user)
                    .createdAt(LocalDateTime.now())
                    .build();
        fantasyTeamRepository.save(savedFantasyTeam);
        return savedFantasyTeam;
    }

    private FantasyTeamResponse mapToFantasyResponse(FantasyTeam savedTeam){
        return FantasyTeamResponse.builder()
                .id(savedTeam.getId())
                .transferPoints(savedTeam.getTransferPoints())
                .budgetRemaining(savedTeam.getBudgetRemaining())
                .teamName(savedTeam.getTeamName())
                .username(savedTeam.getUser().getUsername())
                .totalPoints(savedTeam.getTotalPoints())
                .chips(buildChipStatus(savedTeam.getId()))
                .activeChipKey(findActiveChipKeyForCurrentGameweek(savedTeam.getId()))
                .build();
    }

    // camelCase key per chip type, matching the frontend's ChipStatus shape
    // exactly (tripleCaptain, benchBoost, wildcard, wildcard2, freeHit).
    private String chipTypeKey(ChipType chipType){
        return switch (chipType) {
            case TRIPLE_CAPTAIN -> "tripleCaptain";
            case BENCH_BOOST -> "benchBoost";
            case WILDCARD -> "wildcard";
            case WILDCARD_2 -> "wildcard2";
            case FREEHIT -> "freeHit";
        };
    }

    private Map<String, Boolean> buildChipStatus(Integer teamId){
        Map<String, Boolean> chips = new LinkedHashMap<>();
        for (ChipType type : ChipType.values()) {
            chips.put(chipTypeKey(type), chipRepository.existsByFantasyTeamIdAndChipType(teamId, type));
        }
        return chips;
    }

    // Only one chip can be active per team per gameweek (see ChipService's
    // activateChip guards + the unique constraint on chips). Surfacing which
    // one (if any) is active for the CURRENT gameweek lets the client lock
    // out the other chip buttons for the week instead of letting the user
    // tap one and only find out it's rejected after the fact.
    private String findActiveChipKeyForCurrentGameweek(Integer teamId){
        return gameweekRepository.findByIsCurrentTrue()
                .flatMap(gw -> chipRepository.findByFantasyTeamIdAndGameweekId(teamId, gw.getId()))
                .map(Chip::getChipType)
                .map(this::chipTypeKey)
                .orElse(null);
    }
}
