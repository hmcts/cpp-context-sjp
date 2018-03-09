package uk.gov.moj.sjp.it.test;

import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.command.UpdateDefendantDetails;
import uk.gov.moj.sjp.it.helper.CaseSearchResultHelper;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.verifier.PersonInfoVerifier;

import java.util.UUID;

import org.junit.Test;

public class SearchCasesIT extends BaseIntegrationTest {

    @Test
    public void verifyInitialSearchDetailsAndUpdateToDefendantDetails() {
        final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
        final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(createCasePayloadBuilder.getId(),
                createCasePayloadBuilder.getUrn(),
                createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());

        caseSearchResultHelper.verifyPersonInfoByUrn();
        caseSearchResultHelper.verifyPersonInfoByLastNameAndDateOfBirth(caseSearchResultHelper.getLastName(), caseSearchResultHelper.getDateOfBirth());

        UpdateDefendantDetails.DefendantDetailsPayloadBuilder updatedDefendantPayload = UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults();

        final UUID caseId = createCasePayloadBuilder.getId();
        UpdateDefendantDetails.updateDefendantDetailsForCaseAndPayload(caseId, UUID.fromString(CasePoller.pollUntilCaseByIdIsOk(caseId).getString("defendant.id")), updatedDefendantPayload);
        caseSearchResultHelper.verifyPersonInfoByLastNameAndDateOfBirth(updatedDefendantPayload.getLastName(), updatedDefendantPayload.getDateOfBirth());
        caseSearchResultHelper.verifyPersonNotFound(createCasePayloadBuilder.getUrn(), caseSearchResultHelper.getLastName());

        PersonInfoVerifier personInfoVerifier = PersonInfoVerifier.personInfoVerifierForDefendantUpdatedPayload(caseId, updatedDefendantPayload);
        personInfoVerifier.verifyPersonInfo(true);
    }
    
    @Test
    public void verifyAssignmentCreationAndDeletionIsReflected() {
        //given case is created
        final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);

        final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(createCasePayloadBuilder.getId(),
                createCasePayloadBuilder.getUrn(),
                createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());
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
