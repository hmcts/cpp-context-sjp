package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RSJPTResultCodeConverterTest extends ResultCodeConverterTest {

    @Test
    public void shouldConvertReferForFutureSJPSessionResult() {
        super.testResultCode();
    }

    @Override
    protected JsonObject givenResult() {
        return createObjectBuilder()
                .add("code", "RSJP")
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

}
