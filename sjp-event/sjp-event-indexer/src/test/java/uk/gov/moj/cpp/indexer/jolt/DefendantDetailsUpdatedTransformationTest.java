package uk.gov.moj.cpp.indexer.jolt;

import static com.jayway.jsonpath.JsonPath.parse;
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

        final JsonObject inputPayload = inputDoc.json();
        final JsonObject defendant = result.getJsonArray("parties").getJsonObject(0);
        final JsonObject aliases = defendant.getJsonArray("aliases").getJsonObject(0);

        assertThat(result.getString("caseId"), is(inputPayload.getString("caseId")));
        assertThat(defendant.getString("_party_type"), is("DEFENDANT"));
        assertThat(defendant.getString("partyId"), is(inputPayload.getString("defendantId")));
        assertThat(defendant.getString("title"), is(inputPayload.getString("title")));
        assertThat(defendant.getString("firstName"), is(inputPayload.getString("firstName")));
        assertThat(defendant.getString("lastName"), is(inputPayload.getString("lastName")));
        assertThat(defendant.getString("dateOfBirth"), is(inputPayload.getString("dateOfBirth")));
        assertThat(defendant.getString("gender"), is(inputPayload.getString("gender")));

        assertAliases(aliases, defendant);

        assertAddressDetails(inputDoc.read("address"), defendant.getString("addressLines"), defendant.getString("postCode"));
    }

    private void assertAliases(final JsonObject aliases, final JsonObject defendant) {
        assertThat(aliases.getString("title"), is(defendant.getString("title")));
        assertThat(aliases.getString("firstName"), is(defendant.getString("firstName")));
        assertThat(aliases.getString("lastName"), is(defendant.getString("lastName")));
    }
}
