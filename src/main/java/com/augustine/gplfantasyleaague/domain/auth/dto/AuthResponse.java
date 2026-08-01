package com.augustine.gplfantasyleaague.domain.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {

    private String token;
    private String username;
    // "USER" or "ADMIN" - lets a client (e.g. the admin website) decide
    // whether to show admin-only UI without guessing from a 403 on some
    // other endpoint. The backend still enforces this independently via
    // @PreAuthorize on every admin endpoint - this field is a UX convenience
    // only, never a substitute for that server-side check.
    private String role;
}
