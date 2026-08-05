package com.augustine.gplfantasyleaague.domain.engagement.service;

import com.augustine.gplfantasyleaague.domain.auth.entity.User;
import com.augustine.gplfantasyleaague.domain.auth.repository.UserRepository;
import com.augustine.gplfantasyleaague.domain.engagement.dtos.DiscussionRequest;
import com.augustine.gplfantasyleaague.domain.engagement.dtos.DiscussionResponse;
import com.augustine.gplfantasyleaague.domain.engagement.dtos.DiscussionStatusResponse;
import com.augustine.gplfantasyleaague.domain.engagement.entity.Discussion;
import com.augustine.gplfantasyleaague.domain.engagement.repository.DiscussionRepository;
import com.augustine.gplfantasyleaague.domain.gameweek.entity.Fixture;
import com.augustine.gplfantasyleaague.domain.gameweek.entity.FixtureStatus;
import com.augustine.gplfantasyleaague.domain.gameweek.entity.Gameweek;
import com.augustine.gplfantasyleaague.domain.gameweek.repository.FixtureRepository;
import com.augustine.gplfantasyleaague.domain.subscription.service.SubscriptionService;
import com.augustine.gplfantasyleaague.exception.InvalidFixtureException;
import com.augustine.gplfantasyleaague.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DiscussionService {
    private final UserRepository userRepository;
    private final DiscussionRepository discussionRepository;
    private final FixtureRepository fixtureRepository;
    private final SubscriptionService subscriptionService;

    public DiscussionService(UserRepository userRepository, DiscussionRepository discussionRepository, FixtureRepository fixtureRepository, SubscriptionService subscriptionService) {
        this.userRepository = userRepository;
        this.discussionRepository = discussionRepository;
        this.fixtureRepository = fixtureRepository;
        this.subscriptionService = subscriptionService;
    }

    public DiscussionResponse addDiscussion(DiscussionRequest request, String email){
        User user = userRepository.findByEmail(email).orElseThrow(()-> new ResourceNotFoundException("User with email " + email + " not found"));
        Fixture fixture = fixtureRepository.findById(request.getFixtureId()).orElseThrow(()-> new ResourceNotFoundException("Fixture with ID " + request.getFixtureId() + " not found"));
        DiscussionStatusResponse status = computeStatus(fixture);
        if (!status.isOpen()) {
            throw new InvalidFixtureException(status.getReason());
        }
        Discussion discussion = addTodiscussionDatabase(user,fixture, request);
        return mapToResponse(discussion);
    }

    // Lets the frontend check ahead of time whether to show the composer at
    // all (and what to say instead) rather than only discovering a closed
    // discussion when a post fails.
    public DiscussionStatusResponse getDiscussionStatus(Integer fixtureId){
        Fixture fixture = fixtureRepository.findById(fixtureId).orElseThrow(()-> new ResourceNotFoundException("Fixture with ID " + fixtureId + " not found"));
        return computeStatus(fixture);
    }

    // Discussions used to be open the moment a fixture was scheduled, which
    // meant threads for matches weeks away sat there empty and inviting
    // premature "who's playing" chatter. Tying the open window to the
    // gameweek's deadline (the same lock-in point used for transfers/chips)
    // means a fixture's discussion opens once squads for that gameweek are
    // actually locked in, and closing it once the match has finished keeps
    // the thread from lingering as a target for spam long after the final
    // whistle.
    private DiscussionStatusResponse computeStatus(Fixture fixture){
        Integer fixtureId = fixture.getId();

        if (fixture.getFixtureStatus() == FixtureStatus.FINISHED) {
            return DiscussionStatusResponse.builder()
                    .fixtureId(fixtureId)
                    .open(false)
                    .reason("This discussion is closed - the match has finished.")
                    .build();
        }

        if (fixture.getFixtureStatus() == FixtureStatus.POSTPONED) {
            return DiscussionStatusResponse.builder()
                    .fixtureId(fixtureId)
                    .open(false)
                    .reason("This match has been postponed - discussion is closed until it's rescheduled.")
                    .build();
        }

        Gameweek gameweek = fixture.getGameweek();
        LocalDateTime deadline = gameweek != null ? gameweek.getDeadline() : null;

        // No deadline set (e.g. older/incompletely-seeded gameweek data) -
        // fail open rather than permanently locking a discussion no admin
        // action can ever unlock.
        if (deadline == null || !LocalDateTime.now().isBefore(deadline)) {
            return DiscussionStatusResponse.builder()
                    .fixtureId(fixtureId)
                    .open(true)
                    .build();
        }

        return DiscussionStatusResponse.builder()
                .fixtureId(fixtureId)
                .open(false)
                .opensAt(deadline)
                .reason("Discussion opens once the gameweek deadline passes.")
                .build();
    }

    public List<DiscussionResponse> getDiscussionsByFixture(Integer id){
        Fixture fixture = fixtureRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Fixture with ID " + id + " not found"));
        return discussionRepository.findByFixtureId(fixture.getId()).stream()
                .map(discussion -> mapToResponse(discussion))
                .toList();
    }

    private Discussion addTodiscussionDatabase(User user, Fixture fixture, DiscussionRequest request){
        Discussion discussion = Discussion.builder()
                .message(request.getMessage())
                .createdAt(LocalDateTime.now())
                .fixture(fixture)
                .user(user)
                .build();
        discussionRepository.save(discussion);
        return discussion;
    }

    private DiscussionResponse mapToResponse(Discussion discussion){
        return DiscussionResponse.builder()
                .id(discussion.getId())
                .fixtureId(discussion.getFixture().getId())
                .userId(discussion.getUser().getId())
                .username(discussion.getUser().getUsername())
                .userPremium(subscriptionService.isPremium(discussion.getUser().getId()))
                .message(discussion.getMessage())
                .createdAt(discussion.getCreatedAt())
                .build();
    }
}
