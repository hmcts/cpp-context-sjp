package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NCOSTSResultCodeConverterTest extends ResultCodeConverterTest {

    @Test
    public void shouldConvertNoCostsResult() {
        super.testResultCode();
    }

    @Override
    protected JsonObject givenResult() {
        return createObjectBuilder()
                .add("code", "NCOSTS")
                .add("terminalEntries",
                        createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("index", 5)
                                        .add("value", "Costs removed because defendant was sorry")
                                )
                )
                .build();
    }

    @Override
    protected JsonArray getExpectedPrompts() {
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "be2a46db-709d-4e0d-9b63-aeb831564c1d")
                        .add("value", "Costs removed because defendant was sorry")
                )
                .build();
    }

    @Override
    protected String getProsecutingAuthority() {
        return "TFL";
    }
}
