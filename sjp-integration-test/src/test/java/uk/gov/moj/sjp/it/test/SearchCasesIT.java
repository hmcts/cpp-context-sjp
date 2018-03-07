package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.*;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.util.DefaultRequests.searchCases;

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
        final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = withDefaults();
        createCaseForPayloadBuilder(createCasePayloadBuilder);
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

        PersonInfoVerifier personInfoVerifier = PersonInfoVerifier.personInfoVerifierForDefendantUpdatedPayload(caseId, updatedDefendantPayload);
        personInfoVerifier.verifyPersonInfo(true);
    }

    @Test
    public void findsDefendantByHistoricalLastName() {

        // Given a case is created, which defendant record's name will be updated
        final CreateCase.CreateCasePayloadBuilder historicalsCaseToBeUpdated = CreateCase.CreateCasePayloadBuilder.withDefaults();
        historicalsCaseToBeUpdated
                .getDefendantBuilder()
                .withLastName("deHistorical");
        createCaseForPayloadBuilder(historicalsCaseToBeUpdated);

        // and second case is created with the same defendants last name
        final CreateCase.CreateCasePayloadBuilder historicalsCaseWithoutUpdates = CreateCase.CreateCasePayloadBuilder.withDefaults();
        historicalsCaseWithoutUpdates
                .getDefendantBuilder()
                .withLastName("deHistorical");
        createCaseForPayloadBuilder(historicalsCaseWithoutUpdates);

        // when last name is updated for the first case
        UpdateDefendantDetails.DefendantDetailsPayloadBuilder updatedDefendantPayload = UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults()
                .withLastName("von Neumann");

        final UUID caseId = historicalsCaseToBeUpdated.getId();
        UpdateDefendantDetails.updateDefendantDetailsForCaseAndPayload(caseId, UUID.fromString(CasePoller.pollUntilCaseByIdIsOk(caseId).getString("defendant.id")), updatedDefendantPayload);

        // then the first case (and second) will be found and system will mark the name as outdated
        poll(searchCases("deHistorical"))
                .until(
                        status().is(OK),
                        payload().isJson(
                                allOf(
                                        withJsonPath("foundCasesWithOutdatedDefendantsName", is(true)),
                                        withJsonPath(
                                                "$.results[*]", hasItem(isJson(allOf(
                                                        withJsonPath("urn", is(historicalsCaseToBeUpdated.getUrn())),
                                                        withJsonPath("defendant.lastName", is("von Neumann")),
                                                        withJsonPath("defendant.outdated", is(true))

                                                )))
                                        ),
                                        withJsonPath(
                                                "$.results[*]", hasItem(isJson(allOf(
                                                        withJsonPath("urn", is(historicalsCaseWithoutUpdates.getUrn())),
                                                        withJsonPath("defendant.lastName", is("deHistorical")),
                                                        withJsonPath("defendant.outdated", is(false))

                                                )))
                                        )
                                )
                        )
                );
    }

    @Test
    public void verifyAssignmentCreationAndDeletionIsReflected() {
        //given case is created
        final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = withDefaults();
        createCaseForPayloadBuilder(createCasePayloadBuilder);

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
