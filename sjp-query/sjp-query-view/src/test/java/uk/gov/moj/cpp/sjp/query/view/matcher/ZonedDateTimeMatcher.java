package uk.gov.moj.cpp.sjp.query.view.matcher;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;


public class ZonedDateTimeMatcher extends TypeSafeDiagnosingMatcher<String> {

    private final ZonedDateTime expected;

    public static ZonedDateTimeMatcher isSameMoment(final ZonedDateTime expected) {
        return new ZonedDateTimeMatcher(expected);
    }

    private ZonedDateTimeMatcher(final ZonedDateTime expected) {
        Objects.requireNonNull(expected);
        this.expected = expected;
    }

    @Override
    protected boolean matchesSafely(final String actual, final Description mismatchDescription) {
        try {
            return expected.toInstant().equals(ZonedDateTime.parse(actual).toInstant());
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    @Override
    public void describeTo(final Description description) {
        description.appendValue(expected);
    }

}
