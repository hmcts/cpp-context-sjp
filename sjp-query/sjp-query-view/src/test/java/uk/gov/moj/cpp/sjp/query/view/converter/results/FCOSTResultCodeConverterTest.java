package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.query.view.util.JsonHelper.readJsonFromFile;

import java.util.Optional;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FCOSTResultCodeConverterTest extends ResultCodeConverterTest {

    @Test
    public void shouldConvertCostResult() {
        when(referenceDataService.getAllFixedList(sourceEnvelope)).thenReturn(Optional.of(readJsonFromFile("data/referencedata.get-all-fixed-list.json")));

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
                        .add("promptDefinitionId", "af921cf4-06e7-4f6b-a4ea-dcb58aab0dbe")
                        .add("value", "Transport for London")
                )
                .build();
    }

    @Override
    protected String getProsecutingAuthority() {
        return "TFL";
    }
}
