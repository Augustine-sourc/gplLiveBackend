package com.augustine.gplfantasyleaague.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UpdateUsernameRequest {
    @NotBlank
    // Mirrors USERNAME_REGEX in the frontend's authValidation.ts (also used
    // at registration) - kept in sync so a value the client-side check
    // accepts never bounces off the server with a generic 400.
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "3-20 characters: letters, numbers, and underscores only.")
    private String username;
}
