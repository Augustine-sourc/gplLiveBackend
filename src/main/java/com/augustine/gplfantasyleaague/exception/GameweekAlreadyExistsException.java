package com.augustine.gplfantasyleaague.exception;

public class GameweekAlreadyExistsException extends RuntimeException {
    public GameweekAlreadyExistsException(String message) {
        super(message);
    }
}
