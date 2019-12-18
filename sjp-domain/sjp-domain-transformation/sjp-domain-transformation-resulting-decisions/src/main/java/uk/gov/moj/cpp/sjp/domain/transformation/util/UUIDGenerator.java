package uk.gov.moj.cpp.sjp.domain.transformation.util;

import java.util.UUID;

public class UUIDGenerator {

    public UUIDGenerator() {
        // This is unused since this is merely a wrapper for testing purposes.
    }

    public String generateRandomUUID() {
        return UUID.randomUUID().toString();
    }
}
