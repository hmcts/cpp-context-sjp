package uk.gov.moj.cpp.sjp.query.view.converter.fixedlists;

import static java.util.Optional.of;

import java.util.Optional;

public class NoActionFixedListConverter implements FixedListConverter {

    @Override
    public Optional<String> convert(final String value) {
        return of(value);
    }
}
