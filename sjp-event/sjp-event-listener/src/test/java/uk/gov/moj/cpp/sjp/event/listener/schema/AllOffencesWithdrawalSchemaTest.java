package uk.gov.moj.cpp.sjp.event.listener.schema;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;

public class AllOffencesWithdrawalSchemaTest {

    private SchemaAssert schemaAssert;

    @Before
    public void setup() throws IOException {
        schemaAssert = SchemaAssert.loadSchema("/json/schema/sjp.events.all-offences-withdrawal-requested.json");
    }

    @Test
    public void shouldValidate() throws URISyntaxException, IOException {
        schemaAssert.assertValid("/uk/gov/moj/cpp/sjp/event/all-offences-withdrawal");
    }
}