package uk.gov.moj.cpp.sjp.query.api.decorator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import java.util.stream.Stream;

import javax.json.JsonObjectBuilder;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class OffenceImprisonableHelperTest {

    private final OffenceHelper offenceHelper = new OffenceHelper();

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("CA03013","SNONIMP",false),
                Arguments.of("RR84227","STRAFF",false),
                Arguments.of("RT88584B","SIMP",true),
                Arguments.of("CD98070","EWAY",true)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldCalculateOffenceOutOfTimeFlag(String offenceCode, String modeOfTrial, boolean expectedOffenceIsImprisonable) {
        final JsonObjectBuilder offenceDefinition = createObjectBuilder();
        offenceDefinition.add("modeOfTrial", modeOfTrial);

        final boolean actualOffenceOutOfTime = offenceHelper.isOffenceImprisonable(offenceDefinition.build());
        assertThat(actualOffenceOutOfTime, is(expectedOffenceIsImprisonable));
    }
}
