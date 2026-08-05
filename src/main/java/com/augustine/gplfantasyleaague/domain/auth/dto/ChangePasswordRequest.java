package com.augustine.gplfantasyleaague.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChangePasswordRequest {
    @NotBlank
    private String currentPassword;

    @NotBlank
    // Mirrors MIN_PASSWORD_LENGTH in the frontend's authValidation.ts.
    @Size(min = 8, message = "Password must be at least 8 characters.")
    private String newPassword;
}
