package uk.gov.moj.cpp.sjp.event.listener.schema;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

public class SjpCaseCreatedSchemaTest {

    private SchemaAssert schemaAssert;

    public SjpCaseCreatedSchemaTest() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/json/schema/structure.events.sjp-case-created.json")) {
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            Schema schema = SchemaLoader.load(rawSchema);
            schemaAssert = new SchemaAssert(schema);
        }
    }

    @Test
    public void shouldValidate() throws URISyntaxException, IOException {
        schemaAssert.assertValid("/uk/gov/moj/cpp/structure/event/sjp-case-created");
    }
}