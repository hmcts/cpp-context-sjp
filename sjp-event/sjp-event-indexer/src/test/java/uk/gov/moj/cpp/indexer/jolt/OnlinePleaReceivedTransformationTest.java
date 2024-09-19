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

public class OnlinePleaReceivedTransformationTest {


    private final JoltTransformer joltTransformer = new JoltTransformer();

    @BeforeEach
    public void setUp() {
        initializeJolt(joltTransformer);
    }

    @Test
    public void shouldTransformPleaReceivedEvent() throws IOException {

        final JsonObject specJson = readJsonViaPath("src/transformer/sjp.events.online-plea-received-spec.json");
        final JsonObject inputPleaReceivedJson = readJson("/sjp.events.online-plea-received.json");

        assertNotNull(specJson);

        final DocumentContext inputPleaReceived = parse(inputPleaReceivedJson);
        final JsonObject transformedPleaReceivedJson = joltTransformer.transformWithJolt(specJson.toString(), inputPleaReceivedJson);

        verifyPlea(inputPleaReceived, transformedPleaReceivedJson);
    }

    @Test
    public void shouldTransformPleaReceivedEventForCompany() throws IOException {

        final JsonObject specJson = readJsonViaPath("src/transformer/sjp.events.online-plea-received-spec.json");
        final JsonObject inputPleaReceivedJson = readJson("/sjp.events.online-plea-received-with-company.json");

        assertNotNull(specJson);

        final DocumentContext inputPleaReceived = parse(inputPleaReceivedJson);
        final JsonObject transformedPleaReceivedJson = joltTransformer.transformWithJolt(specJson.toString(), inputPleaReceivedJson);

        verifyPleaForCompany(inputPleaReceived, transformedPleaReceivedJson);
    }

    private void verifyPlea(final DocumentContext inputPleaReceived, final JsonObject outputPleaReceived) {

        assertEquals(outputPleaReceived.getString("caseId"),((JsonString) inputPleaReceived.read("$.caseId")).getString());
        assertEquals(outputPleaReceived.getString("caseReference"),((JsonString) inputPleaReceived.read("$.urn")).getString());

        final JsonObject outputParties = (JsonObject) outputPleaReceived.getJsonArray("parties").get(0);
        final JsonObject inputPersonalDetails = inputPleaReceived.read("$.personalDetails");

        verifyParties(inputPersonalDetails, outputParties, inputPleaReceived);
    }

    private void verifyParties(final JsonObject inputPersonalDetails, final JsonObject outputParties, final DocumentContext inputPleaReceived) {

        assertEquals(outputParties.getString("partyId"),((JsonString) inputPleaReceived.read("$.defendantId")).getString());

        assertEquals(outputParties.getString("firstName"),inputPersonalDetails.getString("firstName"));
        assertEquals(outputParties.getString("lastName"),inputPersonalDetails.getString("lastName"));
        assertEquals(outputParties.getString("dateOfBirth"),inputPersonalDetails.getString("dateOfBirth"));

        assertAddressDetails(inputPersonalDetails.getJsonObject("address"), outputParties.getString("addressLines")
                , outputParties.getString("postCode"));
    }

    private void verifyPleaForCompany(final DocumentContext inputPleaReceived, final JsonObject outputPleaReceived) {

        assertEquals(outputPleaReceived.getString("caseId"),((JsonString) inputPleaReceived.read("$.caseId")).getString());
        assertEquals(outputPleaReceived.getString("caseReference"),((JsonString) inputPleaReceived.read("$.urn")).getString());

        final JsonObject outputParties = (JsonObject) outputPleaReceived.getJsonArray("parties").get(0);
        final JsonObject inputLegalEntityDetails = inputPleaReceived.read("$.legalEntityDefendant");

        verifyPartiesForCompany(inputLegalEntityDetails, outputParties, inputPleaReceived);
    }

    private void verifyPartiesForCompany(final JsonObject inputLegalEntityDetails, final JsonObject outputParties, final DocumentContext inputPleaReceived) {

        assertEquals(outputParties.getString("partyId"),((JsonString) inputPleaReceived.read("$.defendantId")).getString());

        assertEquals(outputParties.getString("organisationName"),inputLegalEntityDetails.getString("legalEntityName"));

        assertAddressDetails(inputLegalEntityDetails.getJsonObject("address"), outputParties.getString("addressLines")
                , outputParties.getString("postCode"));
    }
}
