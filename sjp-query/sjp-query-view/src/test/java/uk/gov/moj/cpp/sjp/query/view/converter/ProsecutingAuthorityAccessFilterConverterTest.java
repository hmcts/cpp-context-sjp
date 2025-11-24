package uk.gov.moj.cpp.sjp.query.view.converter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityAccess;


import org.junit.jupiter.api.BeforeEach;

public class ProsecutingAuthorityAccessFilterConverterTest {

    private static final String PROSECUTING_AUTHORITY = "PROC1";

    private ProsecutingAuthorityAccessFilterConverter prosecutingAuthorityAccessFilterConverter;

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(ProsecutingAuthorityAccess.NONE, null),
                Arguments.of(ProsecutingAuthorityAccess.of(PROSECUTING_AUTHORITY), PROSECUTING_AUTHORITY),
                Arguments.of(ProsecutingAuthorityAccess.ALL, "%")
        );
    }

    @BeforeEach
    public void init() {
        prosecutingAuthorityAccessFilterConverter = new ProsecutingAuthorityAccessFilterConverter();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldConvertToProsecutingAuthorityAccessFilterValue(ProsecutingAuthorityAccess prosecutingAuthorityAccess, String expectedFilterValue) {
        assertThat(prosecutingAuthorityAccessFilterConverter.convertToProsecutingAuthorityAccessFilter(prosecutingAuthorityAccess),
                equalTo(expectedFilterValue));
    }
}
