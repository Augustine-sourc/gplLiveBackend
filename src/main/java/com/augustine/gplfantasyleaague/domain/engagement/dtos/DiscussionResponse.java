package com.augustine.gplfantasyleaague.domain.engagement.dtos;

import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionResponse {
    private Integer id;
    private Integer fixtureId;
    private Integer userId;
    private String username;
    private String message;
    private LocalDateTime createdAt;
}
