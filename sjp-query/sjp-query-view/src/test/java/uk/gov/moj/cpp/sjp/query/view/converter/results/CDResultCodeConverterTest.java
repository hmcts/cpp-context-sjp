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

public class CDResultCodeConverterTest extends ResultCodeConverterTest {

    private String durationTerminalEntry;
    private String periodTerminalEntry;
    private String expectedPrompt;

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("1", "Year(s)", "1 Years"), 
                Arguments.of("2", "Month(s)", "2 Months"), 
                Arguments.of("3", "Week(s)", "3 Weeks")
        );
    }

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldConvertConditionalDischargeResult(String durationTerminalEntry, String periodTerminalEntry, String expectedPrompt) {
        this.durationTerminalEntry = durationTerminalEntry;
        this.periodTerminalEntry = periodTerminalEntry;
        this.expectedPrompt = expectedPrompt;
        super.testResultCode();
    }

    @Override
    protected JsonObject givenResult() {
        return createObjectBuilder()
                .add("code", "CD")
                .add("terminalEntries",
                        createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("index", 1)
                                        .add("value", durationTerminalEntry)
                                )
                                .add(createObjectBuilder()
                                        .add("index", 2)
                                        .add("value", periodTerminalEntry)
                                )
                )
                .build();
    }

    @Override
    protected JsonArray getExpectedPrompts() {
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "d3205319-84cf-4c5b-9d7a-7e4bb1865054")
                        .add("value", expectedPrompt)
                )
                .build();
    }
}
