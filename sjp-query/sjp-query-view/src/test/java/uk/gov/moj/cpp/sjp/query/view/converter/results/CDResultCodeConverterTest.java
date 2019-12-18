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
public class CDResultCodeConverterTest extends ResultCodeConverterTest {

    @Parameterized.Parameter
    public String durationTerminalEntry;

    @Parameterized.Parameter(1)
    public String periodTerminalEntry;

    @Parameterized.Parameter(2)
    public String expectedPrompt;


    @Parameterized.Parameters(name = "Terminal entries with values {0} {1} should be converted to prompt {2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"1", "Year(s)", "1 Years"}, {"2", "Month(s)", "2 Months"}, {"3", "Week(s)", "3 Weeks"}
        });
    }

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void shouldConvertConditionalDischargeResult() {
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
