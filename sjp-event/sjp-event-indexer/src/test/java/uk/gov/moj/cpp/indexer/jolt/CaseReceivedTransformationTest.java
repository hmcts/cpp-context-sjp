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
import javax.json.JsonString;

import com.jayway.jsonpath.DocumentContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CaseReceivedTransformationTest {

    private final JoltTransformer joltTransformer = new JoltTransformer();

    @BeforeEach
    public void setUp() {
        initializeJolt(joltTransformer);
    }

    @Test
    public void shouldTransformCaseReceivedEvent() throws IOException {

        final JsonObject specJson = readJsonViaPath("src/transformer/sjp.events.case-received-spec.json");
        assertNotNull(specJson);

        final JsonObject inputJson = readJson("/sjp.events.case-received.json");
        final DocumentContext inputCourtApplication = parse(inputJson);
        final JsonObject transformedJson = joltTransformer.transformWithJolt(specJson.toString(), inputJson);

        verifyCase(inputCourtApplication, transformedJson, false);
    }

    private void verifyCase(final DocumentContext inputCase, final JsonObject outputCase, final boolean isCompany) {

        assertThat(outputCase.getString("caseId"), is(((JsonString) inputCase.read("$.caseId")).getString()));
        assertThat(outputCase.getString("caseReference"), is(((JsonString) inputCase.read("$.urn")).getString()));
        assertThat(outputCase.getString("prosecutingAuthority"), is(((JsonString) inputCase.read("$.prosecutingAuthority")).getString()));
        assertThat(outputCase.getString("sjpNoticeServed"), is(((JsonString) inputCase.read("$.postingDate")).getString()));
        assertThat(outputCase.getString("caseStatus"), is("NO_PLEA_RECEIVED"));
        assertThat(outputCase.getString("_case_type"), is("PROSECUTION"));
        assertThat(outputCase.getBoolean("_is_sjp"), is(true));
        assertThat(outputCase.getBoolean("_is_magistrates"), is(false));
        assertThat(outputCase.getBoolean("_is_crown"), is(false));
        assertThat(outputCase.getBoolean("_is_charging"), is(false));

        final JsonObject inputDefendant = inputCase.read("$.defendant");
        final JsonObject outputParties = (JsonObject) outputCase.getJsonArray("parties").get(0);

        if (isCompany) {
            verifyPartiesWithCompany(inputDefendant, outputParties);
        } else {
            verifyParties(inputDefendant, outputParties);
        }

    }

    private void verifyParties(final JsonObject inputDefendant, final JsonObject outputParties) {
        final JsonObject aliases = outputParties.getJsonArray("aliases").getJsonObject(0);

        assertThat(outputParties.getString("partyId"), is(inputDefendant.getString("id")));
        assertThat(outputParties.getString(TITLE), is(inputDefendant.getString(TITLE)));
        assertThat(outputParties.getString(FIRST_NAME), is(inputDefendant.getString(FIRST_NAME)));
        assertThat(outputParties.getString(LAST_NAME), is(inputDefendant.getString(LAST_NAME)));
        assertThat(outputParties.getString(DOB), is(inputDefendant.getString(DOB)));
        assertThat(outputParties.getString(GENDER), is(inputDefendant.getString(GENDER)));
        assertThat(outputParties.getString("arrestSummonsNumber"), is(inputDefendant.getString("asn")));
        assertThat(outputParties.getString("_party_type"), is("DEFENDANT"));

        assertAliases(aliases, inputDefendant);

        assertAddressDetails(inputDefendant.getJsonObject("address"), outputParties.getString("addressLines")
                , outputParties.getString("postCode"));
    }

    private void assertAliases(final JsonObject aliases, final JsonObject defendant) {
        assertThat(aliases.getString(TITLE), is(defendant.getString(TITLE)));
        assertThat(aliases.getString(FIRST_NAME), is(defendant.getString(FIRST_NAME)));
        assertThat(aliases.getString(LAST_NAME), is(defendant.getString(LAST_NAME)));
    }

    @Test
    public void shouldTransformCaseReceivedEventWithCompany() throws IOException {

        final JsonObject specJson = readJsonViaPath("src/transformer/sjp.events.case-received-spec.json");
        assertNotNull(specJson);

        final JsonObject inputJson = readJson("/sjp.events.case-received-with-company.json");
        final DocumentContext inputCourtApplication = parse(inputJson);
        final JsonObject transformedJson = joltTransformer.transformWithJolt(specJson.toString(), inputJson);

        verifyCase(inputCourtApplication, transformedJson, true);
    }

    private void verifyPartiesWithCompany(final JsonObject inputDefendant, final JsonObject outputParties) {
        final JsonObject aliases = outputParties.getJsonArray("aliases").getJsonObject(0);

        assertThat(outputParties.getString("partyId"), is(inputDefendant.getString("id")));
        assertThat(outputParties.getString(ORGANISATION_NAME), is(inputDefendant.getString(LEGAL_ENTITY_NAME)));
        assertThat(outputParties.getString("_party_type"), is("DEFENDANT"));

        assertAliasesWithCompany(aliases, inputDefendant);

        assertAddressDetails(inputDefendant.getJsonObject("address"), outputParties.getString("addressLines")
                , outputParties.getString("postCode"));
    }

    private void assertAliasesWithCompany(final JsonObject aliases, final JsonObject defendant) {
        assertEquals(defendant.getString(LEGAL_ENTITY_NAME), aliases.getString(ORGANISATION_NAME));
    }
}
