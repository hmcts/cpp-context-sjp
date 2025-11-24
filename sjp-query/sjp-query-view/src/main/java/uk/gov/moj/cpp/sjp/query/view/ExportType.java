package uk.gov.moj.cpp.sjp.query.view;

import static java.util.Objects.isNull;

import java.util.stream.Stream;

public enum ExportType {
    PRESS, PUBLIC;

    public static ExportType of(final String value) {
        if (isNull(value)) {
            return PUBLIC;
        }
        return Stream.of(values())
                .filter(e -> e.name().equals(value.toUpperCase().trim()))
                .findFirst()
                .orElse(PUBLIC);
    }
}
