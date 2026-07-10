package uk.gov.moj.cpp.sjp.query.api.decorator;

import static java.util.Objects.nonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import java.time.LocalDate;
import java.util.stream.Stream;

import javax.json.JsonObjectBuilder;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class OffenceNotInEffectHelperTest {

    private static final LocalDate DATE = LocalDate.of(2019, 01, 01);
    private final OffenceHelper offenceHelper = new OffenceHelper();

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(DATE, DATE.minusDays(1), DATE.plusDays(1), false),
                Arguments.of(DATE, DATE.minusDays(1), DATE, false),
                Arguments.of(DATE, DATE, DATE.plusDays(1), false),
                Arguments.of(DATE, DATE, DATE, false),
                Arguments.of(DATE, DATE.plusDays(1), DATE.plusDays(2), true),
                Arguments.of(DATE, DATE.minusDays(2), DATE.minusDays(1), true),
                Arguments.of(DATE, DATE.plusDays(2), DATE.minusDays(1), true),
                Arguments.of(DATE, null, null, false),
                Arguments.of(DATE, null, DATE.plusDays(1), false),
                Arguments.of(DATE, null, DATE, false),
                Arguments.of(DATE, DATE.minusDays(1), null, false)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldCalculateOffenceNotInEffectFlag(LocalDate offenceStartDate, LocalDate offenceDefinitionStartDate, LocalDate offenceDefinitionEndDate, boolean expectedOffenceNotInEffect) {
        final JsonObjectBuilder offenceDefinition = createObjectBuilder();
        if (nonNull(offenceDefinitionStartDate)) {
            offenceDefinition.add("offenceStartDate", offenceDefinitionStartDate.toString());
        }
        if (nonNull(offenceDefinitionEndDate)) {
            offenceDefinition.add("offenceEndDate", offenceDefinitionEndDate.toString());
        }

        final JsonObjectBuilder offenceInstance = createObjectBuilder()
                .add("startDate", offenceStartDate.toString());

        final boolean actualOffenceNotInEffect = offenceHelper.isOffenceNotInEffect(offenceInstance.build(), offenceDefinition.build());
        assertThat(actualOffenceNotInEffect, is(expectedOffenceNotInEffect));
    }
}
