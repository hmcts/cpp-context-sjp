package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FCOMPResultCodeConverterTest extends ResultCodeConverterTest {

    @Test
    public void shouldConvertCompensationResult() {
        super.testResultCode();
    }

    @Override
    protected JsonObject givenResult() {
        return createObjectBuilder()
                .add("code", "FCOMP")
                .add("terminalEntries",
                        createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("index", 1)
                                        .add("value", "30")
                                )
                )
                .build();
    }

    @Override
    protected JsonArray getExpectedPrompts() {
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "26985e5b-fe1f-4d7d-a21a-57207c5966e7")
                        .add("value", "30")
                )
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "af921cf4-06e7-4f6b-a4ea-dcb58aab0dbe")
                        .add("value", "Driver and Vehicle Licensing Agency")
                )
                .build();
    }
}
