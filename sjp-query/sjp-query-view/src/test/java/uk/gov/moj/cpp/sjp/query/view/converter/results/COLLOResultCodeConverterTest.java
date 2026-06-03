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

public class COLLOResultCodeConverterTest extends ResultCodeConverterTest {

    public String paymentMethodTerminalEntry;

    public String reason;

    public String paymentMethodPrompt;

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of( "Pay directly to court", "No information from defendant", "Make payments as ordered"),
                Arguments.of( "Deduct from benefits", "Defendant on benefits", "Deductions from benefit" ),
                Arguments.of( "Attach to earnings", "Defendant employed", "Attachment of earnings" )
        );
    }

    @BeforeEach
    public void setup() {
        initMocks(this);
    }
    @ParameterizedTest
    @MethodSource("data")
    public void shouldConvertCollectionOrderMadeResult(String paymentMethodTerminalEntry, String reason, String paymentMethodPrompt) {
        this.paymentMethodTerminalEntry = paymentMethodTerminalEntry;
        this.reason = reason;
        this.paymentMethodPrompt = paymentMethodPrompt;
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
