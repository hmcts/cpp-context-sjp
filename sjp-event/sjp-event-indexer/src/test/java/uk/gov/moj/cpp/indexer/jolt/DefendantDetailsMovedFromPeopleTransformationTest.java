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
    private static final String TITLE = "title";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";

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

        verifyParties(inputJson, transformedJson);
    }

    private void verifyParties(final JsonObject inputDefendant, final JsonObject transformedJson) {

        final JsonObject defendant = (JsonObject) transformedJson.getJsonArray("parties").get(0);
        final JsonObject aliases = defendant.getJsonArray("aliases").getJsonObject(0);

        assertThat(transformedJson.getString("caseId"), is(inputDefendant.getString("caseId")));
        assertThat(defendant.getString("partyId"), is(inputDefendant.getString("defendantId")));
        assertThat(defendant.getString(TITLE), is(inputDefendant.getString(TITLE)));
        assertThat(defendant.getString(FIRST_NAME), is(inputDefendant.getString(FIRST_NAME)));
        assertThat(defendant.getString(LAST_NAME), is(inputDefendant.getString(LAST_NAME)));
        assertThat(defendant.getString("dateOfBirth"), is(inputDefendant.getString("dateOfBirth")));
        assertThat(defendant.getString("gender"), is(inputDefendant.getString("gender")));
        assertThat(defendant.getString("_party_type"), is("defendant"));

        assertAliases(aliases, defendant);

        assertAddressDetails(inputDefendant.getJsonObject("address"), defendant.getString("addressLines")
                , defendant.getString("postCode"));
    }

    private void assertAliases(final JsonObject aliases, final JsonObject defendant) {
        assertThat(aliases.getString(TITLE), is(defendant.getString(TITLE)));
        assertThat(aliases.getString(FIRST_NAME), is(defendant.getString(FIRST_NAME)));
        assertThat(aliases.getString(LAST_NAME), is(defendant.getString(LAST_NAME)));
    }
}
