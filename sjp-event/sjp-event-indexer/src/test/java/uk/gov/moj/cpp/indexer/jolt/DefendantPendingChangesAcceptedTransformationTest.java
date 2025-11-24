package uk.gov.moj.cpp.indexer.jolt;

import static com.jayway.jsonpath.JsonPath.parse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.moj.cpp.indexer.jolt.helper.AddressVerificationHelper.assertAddressDetails;
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

class DefendantPendingChangesAcceptedTransformationTest {
    private final JoltTransformer joltTransformer = new JoltTransformer();

    @BeforeEach
    public void setUp() {
        initializeJolt(joltTransformer);
    }

    @Test
    void shouldTransformPleaReceivedEvent() throws IOException {
        final JsonObject specJson = readJsonViaPath("src/transformer/sjp.events.defendant-pending-changes-accepted-spec.json");
        final JsonObject inputPleaReceivedJson = readJson("/sjp.events.defendant-pending-changes-accepted.json");

        assertNotNull(specJson);

        final DocumentContext inputPleaReceived = parse(inputPleaReceivedJson);
        final JsonObject transformedPleaReceivedJson = joltTransformer.transformWithJolt(specJson.toString(), inputPleaReceivedJson);

        verifyPlea(inputPleaReceived, transformedPleaReceivedJson);
    }

    @Test
    void shouldTransformPleaReceivedEventForCompany() throws IOException {
        final JsonObject specJson = readJsonViaPath("src/transformer/sjp.events.defendant-pending-changes-accepted-spec.json");
        final JsonObject inputPleaReceivedJson = readJson("/sjp.events.defendant-pending-changes-accepted-with-company.json");

        assertNotNull(specJson);

        final DocumentContext inputPleaReceived = parse(inputPleaReceivedJson);
        final JsonObject transformedPleaReceivedJson = joltTransformer.transformWithJolt(specJson.toString(), inputPleaReceivedJson);

        verifyPleaForCompany(inputPleaReceived, transformedPleaReceivedJson);
    }

    private void verifyPlea(final DocumentContext inputPleaReceived, final JsonObject outputPleaReceived) {
        assertEquals(outputPleaReceived.getString("caseId"), ((JsonString) inputPleaReceived.read("$.caseId")).getString());

        final JsonObject outputParties = (JsonObject) outputPleaReceived.getJsonArray("parties").get(0);

        verifyParties(outputParties, inputPleaReceived);
    }

    private void verifyParties(final JsonObject outputParties, final DocumentContext inputPleaReceived) {
        assertEquals(outputParties.getString("partyId"), getJsonString(inputPleaReceived, "defendantId"));

        assertEquals(outputParties.getString("firstName"), getJsonString(inputPleaReceived, "firstName"));
        assertEquals(outputParties.getString("lastName"), getJsonString(inputPleaReceived, "lastName"));
        assertEquals(outputParties.getString("dateOfBirth"), getJsonString(inputPleaReceived, "dateOfBirth"));
        assertAddressDetails(inputPleaReceived.read("$.address"), outputParties.getString("addressLines")
                , outputParties.getString("postCode"));
    }

    private void verifyPleaForCompany(final DocumentContext inputPleaReceived, final JsonObject outputPleaReceived) {
        assertEquals(outputPleaReceived.getString("caseId"), ((JsonString) inputPleaReceived.read("$.caseId")).getString());

        final JsonObject outputParties = (JsonObject) outputPleaReceived.getJsonArray("parties").get(0);

        verifyPartiesForCompany(outputParties, inputPleaReceived);
    }

    private void verifyPartiesForCompany(final JsonObject outputParties, final DocumentContext inputPleaReceived) {
        assertEquals(outputParties.getString("partyId"), getJsonString(inputPleaReceived, "defendantId"));

        assertEquals(outputParties.getString("organisationName"), getJsonString(inputPleaReceived, "legalEntityName"));

        assertAddressDetails(inputPleaReceived.read("$.address"), outputParties.getString("addressLines")
                , outputParties.getString("postCode"));
    }

    private String getJsonString(final DocumentContext inputPleaReceived, final String field) {
        return ((JsonString) inputPleaReceived.read("$." + field)).getString();
    }
}
