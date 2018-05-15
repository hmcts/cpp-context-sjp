package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.sjp.it.command.AssociateEnterpriseId;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.verifier.CaseReceivedMQVerifier;

import com.jayway.restassured.path.json.JsonPath;
import org.junit.Test;

/**
 * Integration test to create a case and verify the case can be read using ID and URN
 */
public class CreateCaseIT extends BaseIntegrationTest {


    @Test
    public void shouldAssociateEnterpriseIdWithCase() {
        final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        

        try (CaseReceivedMQVerifier caseReceivedMQVerifier = new CaseReceivedMQVerifier()) {
            CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
            caseReceivedMQVerifier.verifyInPrivateActiveMQ(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getUrn());
        }

        final JsonPath jsonResponse = CasePoller.pollUntilCaseByIdIsOk(createCasePayloadBuilder.getId());

        assertThat(jsonResponse.get("id"), equalTo(createCasePayloadBuilder.getId().toString()));
        assertThat(jsonResponse.get("urn"), equalTo(createCasePayloadBuilder.getUrn()));
        assertThat(jsonResponse.get("defendant.personalDetails.title"), equalTo(createCasePayloadBuilder.getDefendantBuilder().getTitle()));
        assertThat(jsonResponse.get("defendant.personalDetails.firstName"), equalTo(createCasePayloadBuilder.getDefendantBuilder().getFirstName()));
        assertThat(jsonResponse.get("defendant.personalDetails.lastName"), equalTo(createCasePayloadBuilder.getDefendantBuilder().getLastName()));
        assertThat(jsonResponse.get("defendant.personalDetails.dateOfBirth"), equalTo(LocalDates.to(createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth())));
        assertThat(jsonResponse.get("defendant.personalDetails.gender"), equalTo(createCasePayloadBuilder.getDefendantBuilder().getGender()));
        assertThat(jsonResponse.get("defendant.numPreviousConvictions"), equalTo(createCasePayloadBuilder.getDefendantBuilder().getNumPreviousConvictions()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address1"), equalTo(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getAddress1()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address2"), equalTo(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getAddress2()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address3"), equalTo(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getAddress3()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address4"), equalTo(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getAddress4()));
        assertThat(jsonResponse.get("defendant.personalDetails.address.postcode"), equalTo(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode()));
        assertThat(jsonResponse.get("defendant.offences[0].offenceSequenceNumber"), equalTo(1)); //supporting only one - 1st 
        assertThat(jsonResponse.get("defendant.offences[0].wording"), equalTo(createCasePayloadBuilder.getOffenceBuilder().getOffenceWording()));
        assertThat(jsonResponse.get("defendant.offences[0].chargeDate"), equalTo(LocalDates.to(createCasePayloadBuilder.getOffenceBuilder().getChargeDate())));

        final String enterpriseId = "2K2SLYFC743H";
        AssociateEnterpriseId associateCommand = new AssociateEnterpriseId(enterpriseId, createCasePayloadBuilder.getId());
        associateCommand.associateEnterpriseIdWIthCase();

        CasePoller.pollUntilCaseByIdIsOk(createCasePayloadBuilder.getId(),
                allOf(
                        withJsonPath("$.urn", is(createCasePayloadBuilder.getUrn())),
                        withJsonPath("$.enterpriseId", is(enterpriseId))
                )
        );
    }
}
