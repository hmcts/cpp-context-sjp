package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DTResultCodeConverterTest extends ResultCodeConverterTest {

    @Test
    public void shouldConvertDismissResult() {
        super.testResultCode();
    }

    @Override
    protected JsonObject givenResult() {
        return createObjectBuilder()
                .add("code", "D")
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
