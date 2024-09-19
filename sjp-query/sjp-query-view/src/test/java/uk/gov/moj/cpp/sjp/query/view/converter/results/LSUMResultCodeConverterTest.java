package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.justice.services.common.converter.LocalDates;

import java.time.LocalDate;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;

public class LSUMResultCodeConverterTest extends ResultCodeConverterTest {

    public String lumpSumAmountTerminalEntry;

    public String lumpSumWithinTerminalEntry;


    public LocalDate payByDatePrompt;

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("300", "Lump sum within 14 days", now.toLocalDate().plusDays(14)),
                        Arguments.of("400", "Lump sum within 28 days", now.toLocalDate().plusDays(28) )
        );
    }

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldConvertLumpSumResult(String lumpSumAmountTerminalEntry, String lumpSumWithinTerminalEntry, LocalDate payByDatePrompt) {
        this.lumpSumAmountTerminalEntry = lumpSumAmountTerminalEntry;
        this.lumpSumWithinTerminalEntry = lumpSumWithinTerminalEntry;
        this.payByDatePrompt = payByDatePrompt;
        super.testResultCode();
    }

    @Override
    protected JsonObject givenResult() {
        return createObjectBuilder()
                .add("code", "LSUM")
                .add("terminalEntries",
                        createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("index", 5)
                                        .add("value", lumpSumAmountTerminalEntry)
                                )
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
                        .add("promptDefinitionId", "ee7d253a-c629-11e8-a355-529269fb1459")
                        .add("value", LocalDates.to(payByDatePrompt))
                )
                .build();
    }

}
