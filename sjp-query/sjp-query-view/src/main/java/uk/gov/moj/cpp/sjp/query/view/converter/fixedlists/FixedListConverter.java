package uk.gov.moj.cpp.sjp.query.view.converter.fixedlists;

import java.util.Optional;

@FunctionalInterface
public interface FixedListConverter {
    Optional<String> convert(final String value);
}
