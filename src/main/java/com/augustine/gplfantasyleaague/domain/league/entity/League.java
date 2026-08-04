package com.augustine.gplfantasyleaague.domain.league.entity;

import com.augustine.gplfantasyleaague.domain.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "leagues")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class League {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    @Builder.Default
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

    // Short shareable code (see LeagueService.generateUniqueInviteCode) -
    // how private leagues get joined, and an alternate way into a public one.
    @Column(name = "invite_code", nullable = false, unique = true)
    private String inviteCode;

    @Builder.Default
    @Column(name = "member_limit", nullable = false)
    private Integer memberLimit = 20;

    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "league", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LeagueMembership> memberships = new ArrayList<>();
}
