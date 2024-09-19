package uk.gov.moj.cpp.indexer.jolt;

import static com.jayway.jsonpath.JsonPath.parse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.indexer.jolt.helper.AddressVerificationHelper.assertAddressDetails;
import static uk.gov.moj.cpp.indexer.jolt.helper.Constants.DOB;
import static uk.gov.moj.cpp.indexer.jolt.helper.Constants.FIRST_NAME;
import static uk.gov.moj.cpp.indexer.jolt.helper.Constants.GENDER;
import static uk.gov.moj.cpp.indexer.jolt.helper.Constants.LAST_NAME;
import static uk.gov.moj.cpp.indexer.jolt.helper.Constants.LEGAL_ENTITY_NAME;
import static uk.gov.moj.cpp.indexer.jolt.helper.Constants.ORGANISATION_NAME;
import static uk.gov.moj.cpp.indexer.jolt.helper.Constants.TITLE;
import static uk.gov.moj.cpp.indexer.jolt.helper.JoltInstanceHelper.initializeJolt;
import static uk.gov.moj.cpp.indexer.jolt.helper.JsonHelper.readJson;
import static uk.gov.moj.cpp.indexer.jolt.helper.JsonHelper.readJsonViaPath;

import uk.gov.justice.json.jolt.JoltTransformer;

import java.io.IOException;

import javax.json.JsonObject;

import com.jayway.jsonpath.DocumentContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefendantDetailsUpdatedTransformationTest {

    private final JoltTransformer joltTransformer = new JoltTransformer();


    @BeforeEach
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
        assertThat(defendant.getString(TITLE), is(inputPayload.getString(TITLE)));
        assertThat(defendant.getString(FIRST_NAME), is(inputPayload.getString(FIRST_NAME)));
        assertThat(defendant.getString(LAST_NAME), is(inputPayload.getString(LAST_NAME)));
        assertThat(defendant.getString(DOB), is(inputPayload.getString(DOB)));
        assertThat(defendant.getString(GENDER), is(inputPayload.getString(GENDER)));

        assertAliases(aliases, defendant);

        assertAddressDetails(inputDoc.read("address"), defendant.getString("addressLines"), defendant.getString("postCode"));
    }

    private void assertAliases(final JsonObject aliases, final JsonObject defendant) {
        assertThat(aliases.getString("title"), is(defendant.getString("title")));
        assertThat(aliases.getString("firstName"), is(defendant.getString("firstName")));
        assertThat(aliases.getString("lastName"), is(defendant.getString("lastName")));
    }

    @Test
    public void shouldTransformCaseReceivedEventForCompany() throws IOException {

        final JsonObject specJson = readJsonViaPath("src/transformer/sjp.events.defendant-details-updated-spec.json");
        assertNotNull(specJson);

        final JsonObject inputJson = readJson("/sjp.events.defendant-details-updated-with-company.json");
        final DocumentContext eventDocContext = parse(inputJson);
        final JsonObject transformedJson = joltTransformer.transformWithJolt(specJson.toString(), inputJson);

        verifyTransformedJsonForCompany(eventDocContext, transformedJson);
    }

    private void verifyTransformedJsonForCompany(final DocumentContext inputDoc, final JsonObject result) {

        final JsonObject inputPayload = inputDoc.json();
        final JsonObject defendant = result.getJsonArray("parties").getJsonObject(0);
        final JsonObject aliases = defendant.getJsonArray("aliases").getJsonObject(0);

        assertThat(result.getString("caseId"), is(inputPayload.getString("caseId")));
        assertThat(defendant.getString("_party_type"), is("DEFENDANT"));
        assertThat(defendant.getString("partyId"), is(inputPayload.getString("defendantId")));
        assertEquals(defendant.getString(ORGANISATION_NAME), inputPayload.getString(LEGAL_ENTITY_NAME));

        assertAliasesForCompany(aliases, defendant);

        assertAddressDetails(inputDoc.read("address"), defendant.getString("addressLines"), defendant.getString("postCode"));
    }

    private void assertAliasesForCompany(final JsonObject aliases, final JsonObject defendant) {
        assertEquals(defendant.getString(ORGANISATION_NAME), aliases.getString(ORGANISATION_NAME));
    }
}
