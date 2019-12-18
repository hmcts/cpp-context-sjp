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
public class RLSUMResultCodeConverterTest extends ResultCodeConverterTest {

    @Parameterized.Parameter
    public String lumpSumWithinTerminalEntry;

    @Parameterized.Parameter(1)
    public String lumpSumWithinPrompt;

    @Parameterized.Parameters(name ="Terminal entries with value {0} should be converted to prompt {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "Lump sum within 14 days", "lump sum 14 days"}, { "Lump sum within 28 days", "lump sum 28 days" }
        });
    }

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void shouldConvertReserveTermLumpSumResult() {
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
