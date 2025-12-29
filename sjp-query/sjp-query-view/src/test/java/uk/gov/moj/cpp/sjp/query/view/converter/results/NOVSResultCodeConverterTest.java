package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NOVSResultCodeConverterTest extends ResultCodeConverterTest {

    @Test
    public void shouldConvertNoVictimSurchargeResult() {
        super.testResultCode();
    }

    @Override
    protected JsonObject givenResult() {
        return createObjectBuilder()
                .add("code", "NOVS")
                .add("terminalEntries",
                        createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("index", 5)
                                        .add("value", "reduced")
                                )
                                .add(createObjectBuilder()
                                        .add("index", 10)
                                        .add("value", "Victim surcharge reduced because defendant was sorry")
                                )
                )
                .build();
    }

    @Override
    protected JsonArray getExpectedPrompts() {
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "042742a1-8d47-4558-9b3e-9f34b358e034")
                        .add("value", "Victim surcharge reduced because defendant was sorry")
                )
                .build();
    }
}
