package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AEOCResultCodeConverterTest extends ResultCodeConverterTest {

    @Test
    public void shouldConvertAttachToEarningsResult() {
        super.testResultCode();
    }

    @Override
    protected JsonObject givenResult() {
        return createObjectBuilder()
                .add("code", "AEOC")
                .add("terminalEntries",
                        createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("index", 15)
                                        .add("value", "650")
                                )
                                .add(createObjectBuilder()
                                        .add("index", 10)
                                        .add("value", "Defendant working so attaching")
                                )
                )
                .build();
    }

    @Override
    protected JsonArray getExpectedPrompts() {
        // The employer data comes from sjp.query.employer.json mocked in the base class
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "a289b1bd-06c8-4da3-b117-0bae6017857c")
                        .add("value", "Defendant working so attaching")
                )
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "485f7d22-718c-4f47-bbd5-f8d934417a03")
                        .add("value", "McDonald's")
                )
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "86854563-b404-4cd4-9c05-50f1e61a0bfe")
                        .add("value", "14 Tottenham Court Road")
                )
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "ef87b9fe-1d9d-46a2-b094-ce5ba24a9835")
                        .add("value", "London")
                )
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "df1d55f8-29dd-42d2-8f99-cb4d369cb38b")
                        .add("value", "England")
                )
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "d3d8e5dc-4ced-4d57-bcbb-5de8f91a580a")
                        .add("value", "UK")
                )
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "a812f1c7-96df-4d8d-9684-25e4e227b2e2")
                        .add("value", "Greater London")
                )
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "4ff32fe8-5508-4b7e-8e6f-b3e79763a9fe")
                        .add("value", "W1T 1JY")
                )
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "eb1d0fdc-2e51-4e98-9f6e-ee0daa14c157")
                        .add("value", "12345")
                )
                .build();
    }

    @Override
    protected String getProsecutingAuthority() {
        return "TFL";
    }
}
