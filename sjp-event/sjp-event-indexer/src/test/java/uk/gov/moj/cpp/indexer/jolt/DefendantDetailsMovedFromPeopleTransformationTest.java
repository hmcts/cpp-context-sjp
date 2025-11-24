package uk.gov.moj.cpp.indexer.jolt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.CoreMatchers.is;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefendantDetailsMovedFromPeopleTransformationTest {

    private final JoltTransformer joltTransformer = new JoltTransformer();

    @BeforeEach
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
        assertEquals(defendant.getString(ORGANISATION_NAME), inputDefendant.getString(LEGAL_ENTITY_NAME));
        assertThat(defendant.getString(DOB), is(inputDefendant.getString(DOB)));
        assertThat(defendant.getString(GENDER), is(inputDefendant.getString("gender")));
        assertThat(defendant.getString("_party_type"), is("DEFENDANT"));

        assertAliases(aliases, defendant);

        assertAddressDetails(inputDefendant.getJsonObject("address"), defendant.getString("addressLines")
                , defendant.getString("postCode"));
    }

    private void assertAliases(final JsonObject aliases, final JsonObject defendant) {
        assertEquals(aliases.getString(TITLE), defendant.getString(TITLE));
        assertEquals(aliases.getString(FIRST_NAME), defendant.getString(FIRST_NAME));
        assertEquals(aliases.getString(LAST_NAME), defendant.getString(LAST_NAME));
        assertEquals(aliases.getString(ORGANISATION_NAME), defendant.getString(ORGANISATION_NAME));
    }
}
