package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SUMRTOResultCodeConverterTest extends ResultCodeConverterTest {

    @Test
    public void shouldConvertTransferOfFineOUtResult() {
        super.testResultCode();
    }

    @Override
    protected JsonObject givenResult() {
        return createObjectBuilder()
                .add("code", "SUMRTO")
                .add("terminalEntries",
                        createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("index", 5)
                                        .add("value", "2018-01-01")
                                )
                                .add(createObjectBuilder()
                                        .add("index", 10)
                                        .add("value", "11:20")
                                )
                                .add(createObjectBuilder()
                                        .add("index", 15)
                                        .add("value", "Coventry Magistrates' Court")
                                )
                                .add(createObjectBuilder()
                                        .add("index", 35)
                                        .add("value", "Defendant is about to be disqualified")
                                )
                )
                .build();
    }

    @Override
    protected JsonArray getExpectedPrompts() {
        return  createArrayBuilder()
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "a528bbfd-54b8-45d6-bad0-26a3a95eb274")
                        .add("value", "2018-01-01")
                )
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "bf63b0b7-9d4b-45af-a16d-4aa520e6be35")
                        .add("value", "11:20")
                )
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "f5699b34-f32f-466e-b7d8-40b4173df154")
                        .add("value", "Coventry Magistrates' Court")
                )
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "dbbb47c9-2202-4913-9a0d-db0a048bfd5f")
                        .add("value", "Defendant is about to be disqualified")
                )
                .build();
    }

}
