package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FCOSTResultCodeConverterTest extends ResultCodeConverterTest {

    @Test
    public void shouldConvertCostResult() {
        super.testResultCode();
    }

    @Override
    protected JsonObject givenResult() {
        return createObjectBuilder()
                .add("code", "FCOST")
                .add("terminalEntries",
                        createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("index", 1)
                                        .add("value", "22")
                                )
                )
                .build();
    }

    @Override
    protected JsonArray getExpectedPrompts() {
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "db261fd9-c6bb-4e10-b93f-9fd98418f7b0")
                        .add("value", "22")
                )
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "22ebf965-8a1c-4229-9894-0df7f8117753")
                        .add("value", "Transport for London")
                )
                .build();
    }

    @Override
    protected String getProsecutingAuthority() {
        return "TFL";
    }
}
