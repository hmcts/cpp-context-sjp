package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class       )
public class TFOOUTResultCodeConverterTest extends ResultCodeConverterTest {

    @Test
    public void shouldConvertTransferOfFineOUtResult() {
        super.testResultCode();
    }

    @Override
    protected JsonObject givenResult() {
        return createObjectBuilder()
                .add("code", "TFOOUT")
                .add("terminalEntries",
                        createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("index", 1)
                                        .add("value", "Lavender Hill Magistrate Court")
                                )
                )
                .build();
    }

    @Override
    protected JsonArray getExpectedPrompts() {
        return  createArrayBuilder()
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "5f589095-2986-4d2b-98fa-30ab00f675d4")
                        .add("value", "Lavender Hill Magistrate Court")
                ).build();
    }

}
