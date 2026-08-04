package com.augustine.gplfantasyleaague.domain.league.service;

import com.augustine.gplfantasyleaague.domain.auth.entity.User;
import com.augustine.gplfantasyleaague.domain.auth.repository.UserRepository;
import com.augustine.gplfantasyleaague.domain.engagement.dtos.NotificationRequest;
import com.augustine.gplfantasyleaague.domain.engagement.entity.NotificationType;
import com.augustine.gplfantasyleaague.domain.engagement.service.NotificationService;
import com.augustine.gplfantasyleaague.domain.fantasy.entity.FantasyTeam;
import com.augustine.gplfantasyleaague.domain.fantasy.repository.FantasyTeamRepository;
import com.augustine.gplfantasyleaague.domain.league.dtos.LeagueCreateRequest;
import com.augustine.gplfantasyleaague.domain.league.dtos.LeagueLeaderboardEntry;
import com.augustine.gplfantasyleaague.domain.league.dtos.LeagueMemberResponse;
import com.augustine.gplfantasyleaague.domain.league.dtos.LeagueResponse;
import com.augustine.gplfantasyleaague.domain.league.entity.League;
import com.augustine.gplfantasyleaague.domain.league.entity.LeagueMembership;
import com.augustine.gplfantasyleaague.domain.league.entity.MembershipStatus;
import com.augustine.gplfantasyleaague.domain.league.repository.LeagueMembershipRepository;
import com.augustine.gplfantasyleaague.domain.league.repository.LeagueRepository;
import com.augustine.gplfantasyleaague.exception.InvalidLeagueException;
import com.augustine.gplfantasyleaague.exception.ResourceNotFoundException;
import com.augustine.gplfantasyleaague.exception.UnauthorizedAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

// A league is a group of users with their own scoped leaderboards (both
// Prediction points and Fantasy points - see getPredictionLeaderboard/
// getFantasyLeaderboard below, both computed live from data that already
// exists elsewhere, nothing duplicated here).
//
// PUBLIC leagues are discoverable by name search and joined instantly.
// PRIVATE leagues are hidden from search - the only way in is the invite
// code, and joining that way creates a PENDING request the creator has to
// accept before it counts as real membership. See V33__leagues.sql.
@Service
public class LeagueService {
    private static final int DEFAULT_MEMBER_LIMIT = 20;
    private static final int MIN_MEMBER_LIMIT = 2;
    private static final int MAX_MEMBER_LIMIT = 200;
    private static final int INVITE_CODE_LENGTH = 6;
    // No 0/O or 1/I - both look alike when someone's reading a code off a
    // phone screen to type into another one.
    private static final String INVITE_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int MAX_INVITE_CODE_ATTEMPTS = 10;

    private final LeagueRepository leagueRepository;
    private final LeagueMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final FantasyTeamRepository fantasyTeamRepository;
    private final NotificationService notificationService;
    private final SecureRandom random = new SecureRandom();

    public LeagueService(LeagueRepository leagueRepository, LeagueMembershipRepository membershipRepository,
                          UserRepository userRepository, FantasyTeamRepository fantasyTeamRepository,
                          NotificationService notificationService) {
        this.leagueRepository = leagueRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.fantasyTeamRepository = fantasyTeamRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public LeagueResponse createLeague(LeagueCreateRequest request, String email) {
        User creator = requireUser(email);
        boolean isPublic = request.getIsPublic() == null || request.getIsPublic();

        League league = League.builder()
                .name(request.getName().trim())
                .isPublic(isPublic)
                .inviteCode(generateUniqueInviteCode())
                .memberLimit(clampMemberLimit(request.getMemberLimit()))
                .creator(creator)
                .createdAt(LocalDateTime.now())
                .build();
        leagueRepository.save(league);

        LeagueMembership ownerMembership = LeagueMembership.builder()
                .league(league)
                .user(creator)
                .status(MembershipStatus.ACTIVE)
                .requestedAt(LocalDateTime.now())
                .build();
        membershipRepository.save(ownerMembership);

        return mapToResponse(league, creator);
    }

    // Blank/omitted query still works - "Containing" against "" matches
    // every public league, so this doubles as a "browse public leagues"
    // list when the search box is empty.
    public List<LeagueResponse> searchPublicLeagues(String query, String email) {
        User me = requireUser(email);
        String q = query == null ? "" : query.trim();
        return leagueRepository.findByIsPublicTrueAndNameContainingIgnoreCase(q).stream()
                .map(league -> mapToResponse(league, me))
                .toList();
    }

    public List<LeagueResponse> getMyLeagues(String email) {
        User me = requireUser(email);
        return membershipRepository.findByUserIdAndStatus(me.getId(), MembershipStatus.ACTIVE).stream()
                .map(membership -> mapToResponse(membership.getLeague(), me))
                .sorted(Comparator.comparing(LeagueResponse::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public LeagueResponse getLeague(Integer leagueId, String email) {
        User me = requireUser(email);
        League league = requireLeague(leagueId);
        requireViewable(league, me);
        return mapToResponse(league, me);
    }

    // Joining a PUBLIC league from search results, by id - straight to
    // ACTIVE, no approval needed. Rejects outright if the league turns out
    // to be private (shouldn't happen via the UI, since private leagues
    // never appear in search, but the endpoint itself has to be safe
    // regardless of how it's called).
    @Transactional
    public LeagueResponse joinPublicLeague(Integer leagueId, String email) {
        User me = requireUser(email);
        League league = requireLeague(leagueId);
        if (!Boolean.TRUE.equals(league.getIsPublic())) {
            throw new InvalidLeagueException("This league is private - ask the owner for an invite code instead.");
        }
        joinOrRequest(league, me, MembershipStatus.ACTIVE);
        return mapToResponse(league, me);
    }

    // Joining/requesting via invite code - works for either kind of league.
    // Public: straight to ACTIVE, same as joining by id. Private: creates a
    // PENDING request and notifies the creator.
    @Transactional
    public LeagueResponse joinByCode(String inviteCode, String email) {
        User me = requireUser(email);
        String code = inviteCode == null ? "" : inviteCode.trim().toUpperCase();
        League league = leagueRepository.findByInviteCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("No league found for that code."));

        MembershipStatus targetStatus = Boolean.TRUE.equals(league.getIsPublic())
                ? MembershipStatus.ACTIVE
                : MembershipStatus.PENDING;
        joinOrRequest(league, me, targetStatus);

        if (targetStatus == MembershipStatus.PENDING) {
            notificationService.sendNotification(notification(
                    league.getCreator().getId(),
                    me.getUsername() + " wants to join \"" + league.getName() + "\".",
                    NotificationType.LEAGUE));
        }
        return mapToResponse(league, me);
    }

    private void joinOrRequest(League league, User user, MembershipStatus targetStatus) {
        if (league.getCreator().getId().equals(user.getId())) {
            throw new InvalidLeagueException("You already own this league.");
        }
        Optional<LeagueMembership> existing = membershipRepository.findByLeagueIdAndUserId(league.getId(), user.getId());
        if (existing.isPresent()) {
            boolean alreadyActive = existing.get().getStatus() == MembershipStatus.ACTIVE;
            throw new InvalidLeagueException(alreadyActive
                    ? "You're already a member of this league."
                    : "Your request to join is already pending.");
        }
        if (targetStatus == MembershipStatus.ACTIVE) {
            requireRoomInLeague(league);
        }
        membershipRepository.save(LeagueMembership.builder()
                .league(league)
                .user(user)
                .status(targetStatus)
                .requestedAt(LocalDateTime.now())
                .build());
    }

    private void requireRoomInLeague(League league) {
        long activeCount = membershipRepository.countByLeagueIdAndStatus(league.getId(), MembershipStatus.ACTIVE);
        if (activeCount >= league.getMemberLimit()) {
            throw new InvalidLeagueException("This league is full.");
        }
    }

    public List<LeagueMemberResponse> getPendingRequests(Integer leagueId, String email) {
        League league = requireOwnedLeague(leagueId, email);
        return membershipRepository.findByLeagueIdAndStatus(league.getId(), MembershipStatus.PENDING).stream()
                .sorted(Comparator.comparing(LeagueMembership::getRequestedAt))
                .map(this::mapToMemberResponse)
                .toList();
    }

    public List<LeagueMemberResponse> getMembers(Integer leagueId, String email) {
        User me = requireUser(email);
        League league = requireLeague(leagueId);
        requireViewable(league, me);
        return membershipRepository.findByLeagueIdAndStatus(league.getId(), MembershipStatus.ACTIVE).stream()
                .map(this::mapToMemberResponse)
                .toList();
    }

    @Transactional
    public void acceptRequest(Integer leagueId, Integer userId, String email) {
        League league = requireOwnedLeague(leagueId, email);
        LeagueMembership membership = requirePendingMembership(league, userId);
        requireRoomInLeague(league);

        membership.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(membership);

        notificationService.sendNotification(notification(
                membership.getUser().getId(),
                "You're in! Your request to join \"" + league.getName() + "\" was accepted.",
                NotificationType.LEAGUE));
    }

    @Transactional
    public void rejectRequest(Integer leagueId, Integer userId, String email) {
        League league = requireOwnedLeague(leagueId, email);
        LeagueMembership membership = requirePendingMembership(league, userId);

        membershipRepository.delete(membership);

        notificationService.sendNotification(notification(
                membership.getUser().getId(),
                "Your request to join \"" + league.getName() + "\" was declined.",
                NotificationType.LEAGUE));
    }

    private LeagueMembership requirePendingMembership(League league, Integer userId) {
        LeagueMembership membership = membershipRepository.findByLeagueIdAndUserId(league.getId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("No join request found for that user."));
        if (membership.getStatus() != MembershipStatus.PENDING) {
            throw new InvalidLeagueException("That request has already been handled.");
        }
        return membership;
    }

    @Transactional
    public void leaveLeague(Integer leagueId, String email) {
        User me = requireUser(email);
        League league = requireLeague(leagueId);
        if (league.getCreator().getId().equals(me.getId())) {
            throw new InvalidLeagueException("You created this league - delete it instead of leaving it.");
        }
        LeagueMembership membership = membershipRepository.findByLeagueIdAndUserId(leagueId, me.getId())
                .orElseThrow(() -> new ResourceNotFoundException("You're not a member of this league."));
        membershipRepository.delete(membership);
    }

    @Transactional
    public void deleteLeague(Integer leagueId, String email) {
        League league = requireOwnedLeague(leagueId, email);
        // Memberships cascade via League.memberships (orphanRemoval + ALL)
        // and the DB's own ON DELETE CASCADE (V33) - belt and braces.
        leagueRepository.delete(league);
    }

    // ---- leaderboards ----

    public List<LeagueLeaderboardEntry> getPredictionLeaderboard(Integer leagueId, String email) {
        League league = viewableLeague(leagueId, email);
        List<User> members = activeMembers(league);
        List<User> ranked = members.stream()
                .sorted(Comparator.comparingInt((User u) -> nz(u.getPredictionPoints())).reversed())
                .toList();
        return rank(ranked, User::getId, User::getUsername, u -> nz(u.getPredictionPoints()), u -> nz(u.getPredictionStreak()));
    }

    public List<LeagueLeaderboardEntry> getFantasyLeaderboard(Integer leagueId, String email) {
        League league = viewableLeague(leagueId, email);
        List<User> members = activeMembers(league);
        List<Integer> memberIds = members.stream().map(User::getId).toList();

        Map<Integer, Integer> pointsByUserId = fantasyTeamRepository.findByUserIdIn(memberIds).stream()
                .collect(Collectors.toMap(team -> team.getUser().getId(), team -> nz(team.getTotalPoints())));

        List<User> ranked = members.stream()
                .sorted(Comparator.comparingInt((User u) -> pointsByUserId.getOrDefault(u.getId(), 0)).reversed())
                .toList();
        // No streak concept for Fantasy points - always null on this one.
        return rank(ranked, User::getId, User::getUsername, u -> pointsByUserId.getOrDefault(u.getId(), 0), u -> null);
    }

    // Shared "1224" competition ranking (ties share a rank, the next
    // distinct score picks up at its true position) - same pattern as
    // PredictionService.getLeaderboard, generalized here so both of a
    // league's leaderboards can use it.
    private List<LeagueLeaderboardEntry> rank(
            List<User> sortedDescending,
            Function<User, Integer> idFn,
            Function<User, String> nameFn,
            ToIntFunction<User> pointsFn,
            Function<User, Integer> streakFn
    ) {
        List<LeagueLeaderboardEntry> entries = new ArrayList<>();
        int rank = 0;
        Integer previousPoints = null;
        for (int i = 0; i < sortedDescending.size(); i++) {
            User user = sortedDescending.get(i);
            int points = pointsFn.applyAsInt(user);
            if (previousPoints == null || previousPoints != points) {
                rank = i + 1;
            }
            previousPoints = points;

            entries.add(LeagueLeaderboardEntry.builder()
                    .rank(rank)
                    .userId(idFn.apply(user))
                    .username(nameFn.apply(user))
                    .points(points)
                    .streak(streakFn.apply(user))
                    .build());
        }
        return entries;
    }

    private League viewableLeague(Integer leagueId, String email) {
        User me = requireUser(email);
        League league = requireLeague(leagueId);
        requireViewable(league, me);
        return league;
    }

    private List<User> activeMembers(League league) {
        List<Integer> memberIds = membershipRepository.findByLeagueIdAndStatus(league.getId(), MembershipStatus.ACTIVE).stream()
                .map(m -> m.getUser().getId())
                .toList();
        return userRepository.findAllById(memberIds);
    }

    private int nz(Integer value) {
        return value == null ? 0 : value;
    }

    // ---- shared lookups/guards ----

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + email + " not found"));
    }

    private League requireLeague(Integer leagueId) {
        return leagueRepository.findById(leagueId)
                .orElseThrow(() -> new ResourceNotFoundException("League with ID " + leagueId + " not found"));
    }

    // Owner-only actions (accept/reject/pending list/delete).
    private League requireOwnedLeague(Integer leagueId, String email) {
        User me = requireUser(email);
        League league = requireLeague(leagueId);
        if (!league.getCreator().getId().equals(me.getId())) {
            throw new UnauthorizedAccessException("Only this league's creator can do that.");
        }
        return league;
    }

    // Public leagues are visible to everyone; private ones only to the
    // creator or someone with an ACTIVE/PENDING membership row - a stranger
    // who just happens to guess a private league's id shouldn't be able to
    // see its member list or leaderboard.
    private void requireViewable(League league, User user) {
        if (Boolean.TRUE.equals(league.getIsPublic())) return;
        if (league.getCreator().getId().equals(user.getId())) return;
        membershipRepository.findByLeagueIdAndUserId(league.getId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("League with ID " + league.getId() + " not found"));
    }

    private int clampMemberLimit(Integer requested) {
        if (requested == null) return DEFAULT_MEMBER_LIMIT;
        return Math.max(MIN_MEMBER_LIMIT, Math.min(MAX_MEMBER_LIMIT, requested));
    }

    private String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < MAX_INVITE_CODE_ATTEMPTS; attempt++) {
            String candidate = randomInviteCode();
            if (!leagueRepository.existsByInviteCode(candidate)) {
                return candidate;
            }
        }
        // Practically unreachable (33^6 ≈ 1.29 billion codes) - fails loudly
        // rather than silently saving a colliding/blank code.
        throw new IllegalStateException("Could not generate a unique invite code - try again.");
    }

    private String randomInviteCode() {
        StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            sb.append(INVITE_CODE_ALPHABET.charAt(random.nextInt(INVITE_CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    private NotificationRequest notification(Integer userId, String message, NotificationType type) {
        NotificationRequest request = new NotificationRequest();
        request.setUserId(userId);
        request.setMessage(message);
        request.setType(type);
        return request;
    }

    private LeagueResponse mapToResponse(League league, User caller) {
        long activeCount = membershipRepository.countByLeagueIdAndStatus(league.getId(), MembershipStatus.ACTIVE);
        String callerStatus = resolveCallerStatus(league, caller);
        // Withhold the invite code from anyone who isn't the owner or an
        // existing member/requester - a stranger browsing public search
        // results doesn't need it (public leagues join by id).
        boolean canSeeCode = "OWNER".equals(callerStatus) || "ACTIVE".equals(callerStatus) || "PENDING".equals(callerStatus);

        return LeagueResponse.builder()
                .id(league.getId())
                .name(league.getName())
                .isPublic(league.getIsPublic())
                .inviteCode(canSeeCode ? league.getInviteCode() : null)
                .memberLimit(league.getMemberLimit())
                .activeMemberCount((int) activeCount)
                .creatorUsername(league.getCreator().getUsername())
                .createdAt(league.getCreatedAt())
                .callerStatus(callerStatus)
                .build();
    }

    private String resolveCallerStatus(League league, User caller) {
        if (league.getCreator().getId().equals(caller.getId())) return "OWNER";
        return membershipRepository.findByLeagueIdAndUserId(league.getId(), caller.getId())
                .map(m -> m.getStatus().name())
                .orElse("NONE");
    }

    private LeagueMemberResponse mapToMemberResponse(LeagueMembership membership) {
        return LeagueMemberResponse.builder()
                .userId(membership.getUser().getId())
                .username(membership.getUser().getUsername())
                .status(membership.getStatus().name())
                .requestedAt(membership.getRequestedAt())
                .build();
    }
}
