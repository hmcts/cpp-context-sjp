package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.helper.CaseProsecutingAuthorityHelper.getProsecutingAuthority;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.sjp.it.commandclient.CreateCaseClient;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.verifier.CaseReceivedMQVerifier;

import java.util.UUID;

import com.jayway.restassured.path.json.JsonPath;
import org.junit.Test;
import org.mortbay.log.Log;

/**
 * Integration test to create a case and verify the case can be read using ID and URN
 */
public class CreateCaseIT extends BaseIntegrationTest {

    @Test
    public void shouldAssociateEnterpriseIdWithCase() {
        final UUID caseId = UUID.randomUUID();
        final ProsecutingAuthority prosecutingAuthority = TFL;

        CreateCaseClient createCase = CreateCaseClient.builder()
                .id(caseId)
                .prosecutingAuthority(prosecutingAuthority)
                .build();

        try (final CaseReceivedMQVerifier caseReceivedMQVerifier = new CaseReceivedMQVerifier()) {
            createCase.caseReceivedHandler = envelope -> Log.info("Case is created");
            createCase.getExecutor().executeSync();

            final JsonEnvelope caseReceivePrivateEvent = caseReceivedMQVerifier.verifyInPrivateActiveMQ(caseId, createCase.urn);

            assertThat(caseReceivePrivateEvent, jsonEnvelope(
                    metadata().withName(CaseReceived.EVENT_NAME),
                    payloadIsJson(
                            withJsonPath("defendant.offences[0].offenceDate", equalTo(createCase.defendant.offences[0].offenceCommittedDate))
                    )));
        }

        final JsonPath jsonResponse = CasePoller.pollUntilCaseByIdIsOk(caseId);

        assertThat(jsonResponse.get("id"), equalTo(caseId.toString()));
        assertThat(jsonResponse.get("urn"), equalTo(createCase.urn));
        assertThat(jsonResponse.get("enterpriseId"), equalTo(createCase.enterpriseId));
        assertThat(jsonResponse.get("defendant.personalDetails.title"), equalTo(createCase.defendant.title));
        assertThat(jsonResponse.get("defendant.personalDetails.firstName"), equalTo(createCase.defendant.firstName));
        assertThat(jsonResponse.get("defendant.personalDetails.lastName"), equalTo(createCase.defendant.lastName));
        assertThat(jsonResponse.get("defendant.personalDetails.dateOfBirth"), equalTo(createCase.defendant.dateOfBirth));
        assertThat(jsonResponse.get("defendant.personalDetails.gender"), equalTo(createCase.defendant.gender));
        assertThat(jsonResponse.get("defendant.numPreviousConvictions"), equalTo(createCase.defendant.numPreviousConvictions));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address1"), equalTo(createCase.defendant.address.address1));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address2"), equalTo(createCase.defendant.address.address2));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address3"), equalTo(createCase.defendant.address.address3));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address4"), equalTo(createCase.defendant.address.address4));
        assertThat(jsonResponse.get("defendant.personalDetails.address.postcode"), equalTo(createCase.defendant.address.postcode));
        assertThat(jsonResponse.get("defendant.offences[0].offenceSequenceNumber"), equalTo(1)); //supporting only one - 1st
        assertThat(jsonResponse.get("defendant.offences[0].wording"), equalTo(createCase.defendant.offences[0].offenceWording));
        assertThat(jsonResponse.get("defendant.offences[0].wordingWelsh"), equalTo(createCase.defendant.offences[0].offenceWordingWelsh));
        assertThat(jsonResponse.get("defendant.offences[0].chargeDate"), equalTo(createCase.defendant.offences[0].chargeDate));
        assertThat(jsonResponse.get("defendant.offences[0].startDate"), equalTo(createCase.defendant.offences[0].offenceCommittedDate));
        assertThat(getProsecutingAuthority(caseId), is(prosecutingAuthority.name()));
    }
}
