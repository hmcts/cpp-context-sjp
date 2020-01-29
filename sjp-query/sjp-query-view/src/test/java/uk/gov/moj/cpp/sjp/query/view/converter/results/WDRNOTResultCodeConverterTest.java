package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class       )
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
                        .add("promptDefinitionId", "1d20aad5-9dc2-42a2-9498-5687f3e5ce33")
                        .add("value", "Prosecutor's request")
                )
                .build();
    }
}
