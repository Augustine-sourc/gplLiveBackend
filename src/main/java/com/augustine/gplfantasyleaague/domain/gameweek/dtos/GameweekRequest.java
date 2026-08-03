package com.augustine.gplfantasyleaague.domain.gameweek.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
public class GameweekRequest {
    // Canonical "YYYY/YYYY" format only (e.g. "2026/2027") - prevents the
    // "2026/27" vs "2026/2027" bug where two strings meant the same season
    // but compared as different values everywhere (duplicate gameweeks,
    // scheduler season-scoping, etc).
    @NotBlank
    @Pattern(regexp = "^\\d{4}/\\d{4}$", message = "must be in the format YYYY/YYYY, e.g. 2026/2027")
    private String season;

    @NotNull
    private Integer gameweekNumber;

    @NotNull
    private LocalDateTime startDate;

    @NotNull
    private LocalDateTime endDate;

    @NotNull
    private LocalDateTime deadline;
}
