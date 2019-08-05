package uk.gov.moj.cpp.indexer.jolt;

import static com.jayway.jsonpath.JsonPath.parse;
import static java.lang.String.format;
import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.indexer.jolt.helper.AddressVerificationHelper.assertAddressDetails;
import static uk.gov.moj.cpp.indexer.jolt.helper.JoltInstanceHelper.initializeJolt;
import static uk.gov.moj.cpp.indexer.jolt.helper.JsonHelper.readJson;
import static uk.gov.moj.cpp.indexer.jolt.helper.JsonHelper.readJsonViaPath;

import uk.gov.justice.json.jolt.JoltTransformer;

import java.io.IOException;

import javax.json.JsonObject;
import javax.json.JsonString;

import com.jayway.jsonpath.DocumentContext;
import org.junit.Before;
import org.junit.Test;

public class DefendantDetailsUpdatedTransformationTest {

    private final JoltTransformer joltTransformer = new JoltTransformer();

    @Before
    public void setUp() {
        initializeJolt(joltTransformer);
    }

    @Test
    public void shouldTransformCaseReceivedEvent() throws IOException {

        final JsonObject specJson = readJsonViaPath("src/transformer/sjp.events.defendant-details-updated-spec.json");
        assertNotNull(specJson);

        final JsonObject inputJson = readJson("/sjp.events.defendant-details-updated.json");
        final DocumentContext eventDocContext = parse(inputJson);
        final JsonObject transformedJson = joltTransformer.transformWithJolt(specJson.toString(), inputJson);

        verifyTransformedJson(eventDocContext, transformedJson);
    }

    private void verifyTransformedJson(final DocumentContext inputDoc, final JsonObject result) {
        final JsonObject defendant = result.getJsonArray("parties").getJsonObject(0);

        assertThat(getStringFromJson(result, "caseId"), is(getStringFromDoc(inputDoc, "caseId")));
        assertThat(getStringFromJson(defendant,  "_party_type"), is("defendant"));
        assertThat(getStringFromJson(defendant,  "partyId"), is(getStringFromDoc(inputDoc, "defendantId")));
        assertThat(getStringFromJson(defendant,  "title"), is(getStringFromDoc(inputDoc, "title")));
        assertThat(getStringFromJson(defendant,  "firstName"), is(getStringFromDoc(inputDoc, "firstName")));
        assertThat(getStringFromJson(defendant,  "lastName"), is(getStringFromDoc(inputDoc, "lastName")));
        assertThat(getStringFromJson(defendant,  "dateOfBirth"), is(getStringFromDoc(inputDoc, "dateOfBirth")));
        assertThat(getStringFromJson(defendant,  "gender"), is(getStringFromDoc(inputDoc, "gender")));

        assertAddressDetails(inputDoc.read("address"), getStringFromJson(defendant, "addressLines"), getStringFromJson(defendant, "postCode"));
    }

    private String getStringFromDoc(final DocumentContext eventDocContext, final String field) {
        return ((JsonString) eventDocContext.read(format("$.%s", field))).getString();
    }

    private String getStringFromJson(final JsonObject transformedJson, final String child) {
        return transformedJson.getString(child);
    }
}
