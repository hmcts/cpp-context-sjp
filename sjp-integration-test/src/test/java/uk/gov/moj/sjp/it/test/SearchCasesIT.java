package uk.gov.moj.sjp.it.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.ContactDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseSearchResultHelper;
import uk.gov.moj.sjp.it.helper.DefendantDetailsHelper;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.util.FileUtil;
import uk.gov.moj.sjp.it.verifier.PersonInfoVerifier;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;

import com.jayway.restassured.path.json.JsonPath;
import org.junit.Before;
import org.junit.Test;

public class SearchCasesIT extends BaseIntegrationTest {

    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;
    private CaseSearchResultHelper caseSearchResultHelper;
    private DefendantDetailsHelper defendantDetailsHelper;

    @Before
    public void createSjpCaseAndVerifyInQueue() {
        defendantDetailsHelper = new DefendantDetailsHelper();
        this.createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        CreateCase.createCaseForPayloadBuilder(this.createCasePayloadBuilder);

        caseSearchResultHelper = new CaseSearchResultHelper(createCasePayloadBuilder);
    }

    private PersonalDetails generateExpectedPersonDetails(JsonObject payload) {
        final String title =  payload.getString("title");
        final String firstName =  payload.getString("firstName");
        final String lastName = payload.getString("lastName");
        final String gender = payload.getString("gender");
        final String nationalInsuranceNumber = payload.getString("nationalInsuranceNumber");
        final String dateOfBirth = payload.getString("dateOfBirth");
        final String email = payload.getString("email");

        final JsonObject contactNumberPayload = payload.getJsonObject("contactNumber");
        final String homeNumber = contactNumberPayload.getString("home");
        final String mobileNumber = contactNumberPayload.getString("mobile");

        final JsonObject address = payload.getJsonObject("address");
        final String address1 = address.getString("address1");
        final String address2 = address.getString("address2");
        final String address3 = address.getString("address3");
        final String address4 = address.getString("address4");
        final String postcode = address.getString("postcode");

        return new PersonalDetails(title, firstName, lastName, LocalDate.parse(dateOfBirth), gender, nationalInsuranceNumber,
                new Address(address1, address2, address3, address4, postcode),
                new ContactDetails(email, homeNumber, mobileNumber)
        );
    }

    @Test
    public void verifyInitialSearchDetailsAndUpdateToDefendantDetails() throws IOException {
        caseSearchResultHelper.verifyPersonInfoByUrn();
        caseSearchResultHelper.verifyPersonInfoByLastNameAndDateOfBirth(caseSearchResultHelper.getLastName(), caseSearchResultHelper.getDateOfBirth());

        JsonObject updatedDefendantPayload = FileUtil.givenPayload("/payload/sjp.update-defendant-details.json");

        final UUID caseId = createCasePayloadBuilder.getId();
        defendantDetailsHelper.updateDefendantDetails(caseId, CasePoller.pollUntilCaseByIdIsOk(caseId).getString("defendant.id"), updatedDefendantPayload);
        caseSearchResultHelper.verifyPersonInfoByLastNameAndDateOfBirth("SMITH", LocalDates.from("1980-07-16"));
        caseSearchResultHelper.verifyPersonNotFound(createCasePayloadBuilder.getUrn(), caseSearchResultHelper.getLastName());

        PersonInfoVerifier personInfoVerifier = new PersonInfoVerifier(caseId);
        personInfoVerifier.verifyPersonInfo(generateExpectedPersonDetails(updatedDefendantPayload), true);

        final JsonPath updatedCase = CasePoller.pollUntilCaseByIdIsOk(createCasePayloadBuilder.getId());
        final String firstName = updatedCase.getString("defendant.personalDetails.firstName");
        final String lastName = updatedCase.getString("defendant.personalDetails.lastName");

        assertThat(firstName, is("David"));
        assertThat(lastName, is("SMITH"));
    }
    
    @Test
    public void verifyAssignmentCreationAndDeletionIsReflected() {
        //given case is created
        // then
        caseSearchResultHelper.verifyAssignment(false);

        // when
        caseSearchResultHelper.assignmentCreated();
        // then
        caseSearchResultHelper.verifyAssignment(true);

        // when
        caseSearchResultHelper.assignmentDeleted();
        // then
        caseSearchResultHelper.verifyAssignment(false);
    }
}
