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
public class RINSTLResultCodeConverterTest extends ResultCodeConverterTest {

    @Parameterized.Parameter()
    public String instalmentAmountTerminalEntry;

    @Parameterized.Parameter(1)
    public String frequencyTerminalEntry;

    @Parameterized.Parameter(2)
    public String startDateTerminalEntry;

    @Parameterized.Parameters(name ="Terminal entries with value {0}, {1}, {2} should match to the same prompt values")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "15", "monthly", "2016-02-28"},
                { "25", "fortnightly", "2016-02-28"},
                { "35", "weekly", "2016-02-28"},
        });
    }

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void shouldConvertReserveTermInstalmentsResult() {
        super.testResultCode();
    }

    @Override
    protected JsonObject givenResult() {
        return createObjectBuilder()
                .add("code", "RINSTL")
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
                        .add("promptDefinitionId", "a507cdea-336e-465a-9bf2-0c7bb5a3d9c7")
                        .add("value", instalmentAmountTerminalEntry)
                )
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "0ed44d89-51b7-451d-8b80-49678c40b1b9")
                        .add("value", frequencyTerminalEntry)
                )
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "e091af2e-43d0-495d-b3b0-432010358a45")
                        .add("value", startDateTerminalEntry)
                )
                .build();
    }
}
