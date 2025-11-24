package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.stream.Stream;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class INSTLResultCodeConverterTest extends ResultCodeConverterTest {

    public String instalmentAmountTerminalEntry;

    public String frequencyTerminalEntry;

    public String startDateTerminalEntry;

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of( "15", "monthly", "2016-02-28"),
                Arguments.of( "25", "fortnightly", "2016-02-28"),
                Arguments.of("35", "weekly", "2016-02-28")
        );
    }

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldConvertInstalmentsResult(String instalmentAmountTerminalEntry, String frequencyTerminalEntry, String startDateTerminalEntry) {
        this.instalmentAmountTerminalEntry = instalmentAmountTerminalEntry;
        this.frequencyTerminalEntry = frequencyTerminalEntry;
        this.startDateTerminalEntry = startDateTerminalEntry;
        super.testResultCode();
    }

    @Override
    protected JsonObject givenResult() {
        return createObjectBuilder()
                .add("code", "INSTL")
                .add("terminalEntries",
                        createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("index", 96)
                                        .add("value", instalmentAmountTerminalEntry)
                                )
                                .add(createObjectBuilder()
                                        .add("index", 97)
                                        .add("value", frequencyTerminalEntry)
                                )
                                .add(createObjectBuilder()
                                        .add("index", 98)
                                        .add("value", startDateTerminalEntry)
                                )
                )
                .build();
    }

    @Override
    protected JsonArray getExpectedPrompts() {
        return  createArrayBuilder()
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
