package uk.gov.moj.cpp.sjp.query.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

    private final UUID userId;

    public UserNotFoundException(final UUID userId) {
        super(String.format("User %s not found", userId));
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}
