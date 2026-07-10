package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import java.util.stream.Stream;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class RLSUMResultCodeConverterTest extends ResultCodeConverterTest {

    public String lumpSumWithinTerminalEntry;

    public String lumpSumWithinPrompt;

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of( "Lump sum within 14 days", "lump sum 14 days"),
                Arguments.of( "Lump sum within 28 days", "lump sum 28 days" )
        );
    }

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldConvertReserveTermLumpSumResult(String lumpSumWithinTerminalEntry, String lumpSumWithinPrompt) {
        this.lumpSumWithinTerminalEntry = lumpSumWithinTerminalEntry;
        this.lumpSumWithinPrompt = lumpSumWithinPrompt;
        super.testResultCode();
    }

    @Override
    protected JsonObject givenResult() {
        return createObjectBuilder()
                .add("code", "RLSUM")
                .add("terminalEntries",
                        createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("index", 6)
                                        .add("value", lumpSumWithinTerminalEntry)
                                )
                )
                .build();
    }

    @Override
    protected JsonArray getExpectedPrompts() {
        return  createArrayBuilder()
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "c131cab0-5dd6-11e8-9c2d-fa7ae01bbebc")
                        .add("value", lumpSumWithinPrompt)
                )
                .build();
    }

}
