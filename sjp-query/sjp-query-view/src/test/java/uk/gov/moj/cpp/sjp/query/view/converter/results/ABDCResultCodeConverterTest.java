package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class ABDCResultCodeConverterTest extends ResultCodeConverterTest {

    @Test
    public void shouldConvertDeductFromBenefitsResult() {
        super.testResultCode();
    }

    @Override
    protected JsonObject givenResult() {
        return createObjectBuilder()
                .add("code", "ABDC")
                .add("terminalEntries",
                        createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("index", 1)
                                        .add("value", "22")
                                )
                                .add(createObjectBuilder()
                                        .add("index", 2)
                                        .add("value", "Deducing from benefits as most feasible option")
                                )
                )
                .build();
    }

    @Override
    protected JsonArray getExpectedPrompts() {
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "8273d5ba-680e-11e8-adc0-fa7ae01bbebc")
                        .add("value", "Deducing from benefits as most feasible option")
                )
                .build();
    }

    @Override
    protected String getProsecutingAuthority() {
        return "TFL";
    }
}
