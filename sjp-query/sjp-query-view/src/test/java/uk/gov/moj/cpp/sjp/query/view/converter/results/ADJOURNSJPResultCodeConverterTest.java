package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ADJOURNSJPResultCodeConverterTest extends ResultCodeConverterTest {

    @Test
    public void shouldConvertAdjournResult() {
        super.testResultCode();
    }

    @Override
    protected JsonObject givenResult() {
        return createObjectBuilder()
                .add("code", "ADJOURNSJP")
                .add("terminalEntries",
                        createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("index", -1)
                                        .add("value", "2018-03-30")
                                )
                )
                .build();
    }

    @Override
    protected JsonArray getExpectedPrompts() {
        return createArrayBuilder()
                .add(createObjectBuilder()
                        .add("promptDefinitionId", "185e6a04-8b44-430d-8073-d8d12f69733a")
                        .add("value", "2018-03-30")
                )
                .build();
    }

}
