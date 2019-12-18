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
public class INSTLResultCodeConverterTest extends ResultCodeConverterTest {

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
    public void shouldConvertInstalmentsResult() {
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
                        .add("promptDefinitionId", "be5a912b-f198-46bb-bae6-ffd0f722c689")
                        .add("value", instalmentAmountTerminalEntry)
                )
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "61c189f1-84b8-4d0e-a6d8-f0c2711d8139")
                        .add("value", frequencyTerminalEntry)
                )
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "3dd7663e-64c5-11e8-adc0-fa7ae01bbebc")
                        .add("value", startDateTerminalEntry)
                )
                .build();
    }
}
