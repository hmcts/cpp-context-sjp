package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ADTResultCodeConverterTest extends ResultCodeConverterTest {

    @Test
    public void shouldConvertCostResult() {
        super.testResultCode();
    }

    @Override
    protected JsonObject givenResult() {
        return createObjectBuilder()
                .add("code", "AD")
                .add("terminalEntries",
                        createArrayBuilder()
                )
                .build();
    }

    @Override
    protected JsonArray getExpectedPrompts() {
        return createArrayBuilder()
                .build();
    }

    @Override
    protected String getProsecutingAuthority() {
        return "TFL";
    }
}
