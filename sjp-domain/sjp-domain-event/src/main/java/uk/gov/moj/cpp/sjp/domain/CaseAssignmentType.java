package uk.gov.moj.cpp.sjp.domain;

import java.util.Arrays;
import java.util.Optional;

public enum CaseAssignmentType {

    MAGISTRATE_DECISION ("for-magistrate-decision"),
    DELEGATED_POWERS_DECISION ("for-delegated-powers-decision"),
    UNKNOWN ("unknown");

    private final String text;

    CaseAssignmentType(String text) {
        this.text = text;
    }

    public static Optional<CaseAssignmentType> from(final String text) {
        return Arrays.stream(values()).filter(x -> x.text.equalsIgnoreCase(text)).findFirst();
    }

    @Override
    public String toString() {
        return text;
    }
}
