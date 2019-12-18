package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.Collection;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class RLSUMIResultCodeConverterTest extends ResultCodeConverterTest {

    @Parameterized.Parameter
    public String lumpSumAmountTerminalEntry;

    @Parameterized.Parameter(1)
    public String instalmentAmountTerminalEntry;

    @Parameterized.Parameter(2)
    public String frequencyTerminalEntry;

    @Parameterized.Parameter(3)
    public String startDateTerminalEntry;

    @Parameterized.Parameters(name ="Terminal entries with value {0}, {1}, {2}, {3} should match to the same prompt values")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "60", "15", "monthly", "2016-02-28"},
                { "70", "25", "fortnightly", "2016-02-28"},
                { "80", "35", "weekly", "2016-02-28"},
        });
    }

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void shouldConvertReserveTermLumpSumInstalmentsResult() {
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
