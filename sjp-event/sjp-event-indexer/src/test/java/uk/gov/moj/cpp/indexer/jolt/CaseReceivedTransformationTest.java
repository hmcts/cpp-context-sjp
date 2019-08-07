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
import javax.json.JsonString;

import com.jayway.jsonpath.DocumentContext;
import org.junit.Before;
import org.junit.Test;

public class CaseReceivedTransformationTest {


    private final JoltTransformer joltTransformer = new JoltTransformer();

    @Before
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

        verifyCase(inputCourtApplication, transformedJson);
    }

    private void verifyCase(final DocumentContext inputCase, final JsonObject outputCase) {

        assertThat(((JsonString) inputCase.read("$.caseId")).getString(), is(outputCase.getString("caseId")));
        assertThat(((JsonString) inputCase.read("$.urn")).getString(), is(outputCase.getString("caseReference")));
        assertThat(((JsonString) inputCase.read("$.prosecutingAuthority")).getString(), is(outputCase.getString("prosecutingAuthority")));
        assertThat(((JsonString) inputCase.read("$.postingDate")).getString(), is(outputCase.getString("sjpNoticeServed")));

        assertThat(outputCase.getString("caseStatus"), is("NO_PLEA_RECEIVED"));
        assertThat(outputCase.getString("_case_type"), is("prosecution"));

        assertThat(outputCase.getBoolean("_is_sjp"), is(true));
        assertThat(outputCase.getBoolean("_is_magistrates"), is(false));
        assertThat(outputCase.getBoolean("_is_crown"), is(false));
        assertThat(outputCase.getBoolean("_is_charging"), is(false));


        final JsonObject inputDefendant = inputCase.read("$.defendant");
        final JsonObject outputParties = (JsonObject) outputCase.getJsonArray("parties").get(0);

        verifyParties(inputDefendant, outputParties);
    }

    private void verifyParties(final JsonObject inputDefendant, final JsonObject outputParties) {
        assertThat(inputDefendant.getString("id"), is(outputParties.getString("partyId")));
        assertThat(inputDefendant.getString("firstName"), is(outputParties.getString("firstName")));
        assertThat(inputDefendant.getString("lastName"), is(outputParties.getString("lastName")));
        assertThat(inputDefendant.getString("title"), is(outputParties.getString("title")));
        assertThat(inputDefendant.getString("dateOfBirth"), is(outputParties.getString("dateOfBirth")));
        assertThat(inputDefendant.getString("gender"), is(outputParties.getString("gender")));
        assertThat(outputParties.getString("_party_type"), is("defendant"));

        assertAddressDetails(inputDefendant.getJsonObject("address"), outputParties.getString("addressLines")
                , outputParties.getString("postCode"));

    }
}
