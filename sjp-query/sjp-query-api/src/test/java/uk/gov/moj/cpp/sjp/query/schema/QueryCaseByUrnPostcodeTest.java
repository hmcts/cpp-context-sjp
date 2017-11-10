package uk.gov.moj.cpp.sjp.query.schema;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

public class QueryCaseByUrnPostcodeTest {

    private static SchemaAssert schemaAssert;

    @BeforeClass
    public static void loadSchema() throws IOException {
        schemaAssert = SchemaAssert.loadSchema("/json/schema/sjp.query.case-by-urn-postcode.json");
    }

    @Test
    public void shouldValidate() throws URISyntaxException, IOException {
        schemaAssert.assertEachFileInFolderIsValidAgainstSchema("/uk/gov/moj/cpp/sjp/query/case-by-urn-postcode");
    }
}