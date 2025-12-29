package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.stream.Stream;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class RLSUMIResultCodeConverterTest extends ResultCodeConverterTest {

    public String lumpSumAmountTerminalEntry;

    public String instalmentAmountTerminalEntry;

    public String frequencyTerminalEntry;

    public String startDateTerminalEntry;

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("60", "15", "monthly", "2016-02-28"),
                Arguments.of("70", "25", "fortnightly", "2016-02-28"),
                Arguments.of("80", "35", "weekly", "2016-02-28")
        );
    }

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldConvertReserveTermLumpSumInstalmentsResult(String lumpSumAmountTerminalEntry, String instalmentAmountTerminalEntry, String frequencyTerminalEntry, String startDateTerminalEntry) {
        this.lumpSumAmountTerminalEntry = lumpSumAmountTerminalEntry;
        this.instalmentAmountTerminalEntry = instalmentAmountTerminalEntry;
        this.frequencyTerminalEntry = frequencyTerminalEntry;
        this.startDateTerminalEntry = startDateTerminalEntry;
        super.testResultCode();
    }

    @Override
    protected JsonObject givenResult() {
        return createObjectBuilder()
                .add("code", "RLSUMI")
                .add("terminalEntries",
                        createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("index", 96)
                                        .add("value", lumpSumAmountTerminalEntry)
                                )
                                .add(createObjectBuilder()
                                        .add("index", 98)
                                        .add("value", instalmentAmountTerminalEntry)
                                )
                                .add(createObjectBuilder()
                                        .add("index", 100)
                                        .add("value", frequencyTerminalEntry)
                                )
                                .add(createObjectBuilder()
                                        .add("index", 99)
                                        .add("value", startDateTerminalEntry)
                                )
                )
                .build();
    }

    @Override
    protected JsonArray getExpectedPrompts() {
        return  createArrayBuilder()
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "8e235a65-5ea2-4fff-ba3b-6cdb74195436")
                        .add("value", lumpSumAmountTerminalEntry)
                )
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "1393acda-7a35-4d65-859d-6298e1470cf1")
                        .add("value", instalmentAmountTerminalEntry)
                )
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "f2a61e80-c13e-4f44-8e91-8ce23e85596b")
                        .add("value", frequencyTerminalEntry)
                )
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "b487696e-dfc9-4c89-80d3-337a4319e925")
                        .add("value", startDateTerminalEntry)
                )
                .build();
    }
}
