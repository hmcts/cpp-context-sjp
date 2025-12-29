package uk.gov.moj.cpp.sjp.query.api.decorator;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;

import java.util.stream.Stream;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class OffenceOutOfTimeHelperTest {

    private static LocalDate DATE = LocalDate.of(2019, 01, 01);
    private final OffenceHelper offenceHelper = new OffenceHelper();

    
    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(DATE, DATE, "6", false),
                Arguments.of(DATE, DATE.plusMonths(6), "6", false),
                Arguments.of(DATE, DATE.plusMonths(6).plusDays(1), "6", true),
                Arguments.of(DATE, DATE, "0", false),
                Arguments.of(DATE, DATE.plusMonths(10), "6", true)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldCalculateOffenceOutOfTimeFlag(LocalDate offenceStartDate, LocalDate offenceChargeDate, String prosecutionTimeLimit, boolean expectedOffenceOutOfTime) {
        final JsonObjectBuilder offenceDefinition = createObjectBuilder();
        if (nonNull(prosecutionTimeLimit)) {
            offenceDefinition.add("prosecutionTimeLimit", prosecutionTimeLimit);
        }

        final JsonObjectBuilder offenceInstance = createObjectBuilder()
                .add("startDate", offenceStartDate.toString())
                .add("chargeDate", offenceChargeDate.toString());

        final boolean actualOffenceOutOfTime = offenceHelper.isOffenceOutOfTime(offenceInstance.build(), offenceDefinition.build());
        assertThat(actualOffenceOutOfTime, is(expectedOffenceOutOfTime));
    }
}
