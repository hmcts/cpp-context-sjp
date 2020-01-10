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

public class OnlinePleaReceivedTransformationTest {


    private final JoltTransformer joltTransformer = new JoltTransformer();

    @Before
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

    private void verifyPlea(final DocumentContext inputPleaReceived, final JsonObject outputPleaReceived) {

        assertThat(((JsonString) inputPleaReceived.read("$.caseId")).getString(), is(outputPleaReceived.getString("caseId")));
        assertThat(((JsonString) inputPleaReceived.read("$.urn")).getString(), is(outputPleaReceived.getString("caseReference")));

        final JsonObject outputParties = (JsonObject) outputPleaReceived.getJsonArray("parties").get(0);
        final JsonObject inputPersonalDetails = inputPleaReceived.read("$.personalDetails");

        verifyParties(inputPersonalDetails, outputParties, inputPleaReceived);
    }

    private void verifyParties(final JsonObject inputPersonalDetails, final JsonObject outputParties, final DocumentContext inputPleaReceived) {

        assertThat(((JsonString) inputPleaReceived.read("$.defendantId")).getString(), is(outputParties.getString("partyId")));

        assertThat(inputPersonalDetails.getString("firstName"), is(outputParties.getString("firstName")));
        assertThat(inputPersonalDetails.getString("lastName"), is(outputParties.getString("lastName")));
        assertThat(inputPersonalDetails.getString("dateOfBirth"), is(outputParties.getString("dateOfBirth")));

        assertAddressDetails(inputPersonalDetails.getJsonObject("address"), outputParties.getString("addressLines")
                , outputParties.getString("postCode"));
    }
}
