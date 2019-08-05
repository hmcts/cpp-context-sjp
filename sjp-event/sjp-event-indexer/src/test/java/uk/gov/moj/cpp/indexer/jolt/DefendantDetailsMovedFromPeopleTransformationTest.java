package uk.gov.moj.cpp.indexer.jolt;

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

import org.junit.Before;
import org.junit.Test;

public class DefendantDetailsMovedFromPeopleTransformationTest {
    private final JoltTransformer joltTransformer = new JoltTransformer();

    @Before
    public void setUp() {
        initializeJolt(joltTransformer);
    }

    @Test
    public void shouldTransformCaseReceivedEvent() throws IOException {

        final JsonObject specJson = readJsonViaPath("src/transformer/sjp.events.defendant-details-moved-from-people-spec.json");
        assertNotNull(specJson);

        final JsonObject inputJson = readJson("/sjp.events.defendant-details-moved-from-people.json");
        final JsonObject transformedJson = joltTransformer.transformWithJolt(specJson.toString(), inputJson);

        final JsonObject outputParties = (JsonObject) transformedJson.getJsonArray("parties").get(0);

        verifyParties(inputJson, outputParties, transformedJson);
    }

    private void verifyParties(final JsonObject inputDefendant, final JsonObject outputParties, final JsonObject transformedJson) {
        assertThat(inputDefendant.getString("caseId"), is(transformedJson.getString("caseId")));
        assertThat(inputDefendant.getString("defendantId"), is(outputParties.getString("partyId")));
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
