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
public class COLLOResultCodeConverterTest extends ResultCodeConverterTest {

    @Parameterized.Parameter
    public String paymentMethodTerminalEntry;

    @Parameterized.Parameter(1)
    public String reason;

    @Parameterized.Parameter(2)
    public String paymentMethodPrompt;

    @Parameterized.Parameters(name ="Terminal entries with value {0} should be converted to prompt {2}, {1} should stay the same")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "Pay directly to court", "No information from defendant", "Make payments as ordered"},
                { "Deduct from benefits", "Defendant on benefits", "Deductions from benefit" },
                { "Attach to earnings", "Defendant employed", "Attachment of earnings" }
        });
    }

    @Before
    public void setup() {
        initMocks(this);
    }
    @Test
    public void shouldConvertCollectionOrderMadeResult() {
        super.testResultCode();
    }

    @Override
    protected JsonObject givenResult() {
        return createObjectBuilder()
                .add("code", "COLLO")
                .add("terminalEntries",
                        createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("index", 4)
                                        .add("value", paymentMethodTerminalEntry)
                                )
                                .add(createObjectBuilder()
                                        .add("index", 9)
                                        .add("value", reason)
                                )
                )
                .build();
    }

    @Override
    protected JsonArray getExpectedPrompts() {
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "6b36e5ff-e116-4dc3-b438-8c02d493959e")
                        .add("value", paymentMethodPrompt)
                )
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "369b6e22-4678-4b04-9fe9-5bb53bed5067")
                        .add("value", reason)
                )
                .build();
    }
}
