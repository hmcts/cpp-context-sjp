package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WDRNOTResultCodeConverterTest extends ResultCodeConverterTest {
    @Test
    public void shouldConvertWithdrawResult() {
        super.testResultCode();
    }

    @Override
    protected JsonObject givenResult() {
        return createObjectBuilder()
                .add("code", "WDRNNOT")
                .add("terminalEntries",
                        createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("index", 2)
                                        .add("value", "Prosecutor's request")
                                )
                )
                .build();
    }

    @Override
    protected JsonArray getExpectedPrompts() {
        return  createArrayBuilder()
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "318c9eb2-cf3c-4592-a353-1b2166c15f81")
                        .add("value", "Prosecutor's request")
                )
                .build();
    }
}
