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
public class LSUMIResultCodeConverterTest extends ResultCodeConverterTest {

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
    public void shouldConvertLumpSumInstalmentsResult() {
        super.testResultCode();
    }
    @Override
    protected JsonObject givenResult() {
        return createObjectBuilder()
                .add("code", "LSUMI")
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
                        .add("promptDefinitionId", "11ba492a-e2ad-11e8-9f32-f2801f1b9fd1")
                        .add("value", lumpSumAmountTerminalEntry)
                )
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "1393acda-7a35-4d65-859d-6298e1470cf1")
                        .add("value", instalmentAmountTerminalEntry)
                )
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "fb4f761c-29d0-4a8e-a947-3debf281dab0")
                        .add("value", frequencyTerminalEntry)
                )
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "e091af2e-43d0-495d-b3b0-432010358a45")
                        .add("value", startDateTerminalEntry)
                )
                .build();
    }

}
