package com.augustine.gplfantasyleaague.exception;

// Covers league business-rule violations that aren't "not found" or
// "not yours": already a member/already pending, league is full, trying to
// instant-join a private league without going through the invite-code
// request flow, an invalid member limit, etc.
public class InvalidLeagueException extends RuntimeException {
    public InvalidLeagueException(String message) {
        super(message);
    }
}
