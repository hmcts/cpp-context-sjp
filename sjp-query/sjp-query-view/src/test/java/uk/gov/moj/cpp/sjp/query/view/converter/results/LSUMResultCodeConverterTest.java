package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.MockitoAnnotations.initMocks;

import uk.gov.justice.services.common.converter.LocalDates;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class LSUMResultCodeConverterTest extends ResultCodeConverterTest {

    @Parameterized.Parameter
    public String lumpSumAmountTerminalEntry;

    @Parameterized.Parameter(1)
    public String lumpSumWithinTerminalEntry;


    @Parameterized.Parameter(2)
    public LocalDate payByDatePrompt;

    @Parameterized.Parameters(name ="Terminal entries with value {1} should be converted to prompt {2}, the {0} is ignored")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "300", "Lump sum within 14 days", now.toLocalDate().plusDays(14)}, { "400", "Lump sum within 28 days", now.toLocalDate().plusDays(28) }
        });
    }

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void shouldConvertLumpSumResult() {
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
