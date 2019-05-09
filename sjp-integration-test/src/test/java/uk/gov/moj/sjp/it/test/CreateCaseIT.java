package uk.gov.moj.sjp.it.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.sjp.domain.DomainConstants.NUMBER_DAYS_WAITING_FOR_PLEA;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.helper.CaseProsecutingAuthorityHelper.getProsecutingAuthority;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;

import java.util.Optional;
import java.util.UUID;

import com.jayway.restassured.path.json.JsonPath;
import org.junit.Test;

/**
 * Integration test to create a case and verify the case can be read using ID and URN
 */
public class CreateCaseIT extends BaseIntegrationTest {

    @Test
    public void shouldAssociateEnterpriseIdWithCase() {
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ProsecutingAuthority prosecutingAuthority = TFL;

        final CreateCase.CreateCasePayloadBuilder createCase = CreateCase.CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withProsecutingAuthority(prosecutingAuthority)
                .withDefendantId(defendantId);
        final CreateCase.DefendantBuilder defendant = createCase.getDefendantBuilder();
        final CreateCase.OffenceBuilder offence = createCase.getOffenceBuilder();

        final Optional<JsonEnvelope> caseReceivedEvent = new EventListener()
                .subscribe(CaseReceived.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCase))
                .popEvent(CaseReceived.EVENT_NAME);

        assertTrue(caseReceivedEvent.isPresent());
        assertThat(caseReceivedEvent.get().payloadAsJsonObject().getString("expectedDateReady"), is(createCase.getPostingDate().plusDays(NUMBER_DAYS_WAITING_FOR_PLEA).toString()));

        final JsonPath jsonResponse = CasePoller.pollUntilCaseByIdIsOk(caseId);
        assertThat(jsonResponse.get("id"), equalTo(caseId.toString()));
        assertThat(jsonResponse.get("urn"), equalTo(createCase.getUrn()));
        assertThat(jsonResponse.get("enterpriseId"), equalTo(createCase.getEnterpriseId()));
        assertThat(jsonResponse.get("status"), equalTo(CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION.name()));
        assertThat(jsonResponse.get("defendant.id"), equalTo(defendant.getId().toString()));
        assertThat(jsonResponse.get("defendant.personalDetails.title"), equalTo(defendant.getTitle()));
        assertThat(jsonResponse.get("defendant.personalDetails.firstName"), equalTo(defendant.getFirstName()));
        assertThat(jsonResponse.get("defendant.personalDetails.lastName"), equalTo(defendant.getLastName()));
        assertThat(jsonResponse.get("defendant.personalDetails.dateOfBirth"), equalTo(defendant.getDateOfBirth().toString()));
        assertThat(jsonResponse.get("defendant.personalDetails.gender"), equalTo(defendant.getGender().toString()));
        assertThat(jsonResponse.get("defendant.numPreviousConvictions"), equalTo(defendant.getNumPreviousConvictions()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address1"), equalTo(defendant.getAddressBuilder().getAddress1()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address2"), equalTo(defendant.getAddressBuilder().getAddress2()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address3"), equalTo(defendant.getAddressBuilder().getAddress3()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address4"), equalTo(defendant.getAddressBuilder().getAddress4()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address5"), equalTo(defendant.getAddressBuilder().getAddress5()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.postcode"), equalTo(defendant.getAddressBuilder().getPostcode()));
        assertThat(jsonResponse.get("defendant.offences[0].offenceSequenceNumber"), equalTo(1)); //supporting only one - 1st
        assertThat(jsonResponse.get("defendant.offences[0].wording"), equalTo(offence.getOffenceWording()));
        assertThat(jsonResponse.get("defendant.offences[0].wordingWelsh"), equalTo(offence.getOffenceWordingWelsh()));
        assertThat(jsonResponse.get("defendant.offences[0].chargeDate"), equalTo(offence.getChargeDate().toString()));
        assertThat(jsonResponse.get("defendant.offences[0].startDate"), equalTo(offence.getOffenceCommittedDate().toString()));
        assertThat(jsonResponse.get("defendant.personalDetails.contactDetails.home"), equalTo(defendant.getContactDetailsBuilder().getHome()));
        assertThat(jsonResponse.get("defendant.personalDetails.contactDetails.mobile"), equalTo(defendant.getContactDetailsBuilder().getMobile()));
        assertThat(jsonResponse.get("defendant.personalDetails.contactDetails.email"), equalTo(defendant.getContactDetailsBuilder().getEmail()));
        assertThat(jsonResponse.get("defendant.personalDetails.nationalInsuranceNumber"), equalTo(defendant.getNationalInsuranceNumber()));

        assertThat(getProsecutingAuthority(caseId), is(prosecutingAuthority.name()));

    }
}
