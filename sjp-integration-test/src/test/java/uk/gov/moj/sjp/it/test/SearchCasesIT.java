package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.withDefaults;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubAddAssignmentCommand;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubRemoveAssignmentCommand;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryOffenceById;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubResultDefinitions;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubStartSjpSessionCommand;
import static uk.gov.moj.sjp.it.util.DefaultRequests.searchCases;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaults;

import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.command.UpdateDefendantDetails;
import uk.gov.moj.sjp.it.helper.CaseSearchResultHelper;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;
import uk.gov.moj.sjp.it.verifier.PersonInfoVerifier;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class SearchCasesIT extends BaseIntegrationTest {

    private SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();

    @Before
    public void setUp() {
        stubQueryOffenceById(randomUUID());
        stubResultDefinitions();
    }

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

        final UpdateDefendantDetails.DefendantDetailsPayloadBuilder updatedDefendantPayload = UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults();

        final UUID caseId = createCasePayloadBuilder.getId();
        UpdateDefendantDetails.updateDefendantDetailsForCaseAndPayload(caseId, UUID.fromString(CasePoller.pollUntilCaseByIdIsOk(caseId).getString("defendant.id")), updatedDefendantPayload);
        caseSearchResultHelper.verifyPersonInfoByLastNameAndDateOfBirth(updatedDefendantPayload.getLastName(), updatedDefendantPayload.getDateOfBirth());

        final PersonInfoVerifier personInfoVerifier = PersonInfoVerifier.personInfoVerifierForDefendantUpdatedPayload(caseId, updatedDefendantPayload);
        personInfoVerifier.verifyPersonInfo(true);
    }

    @Test
    public void findsDefendantByHistoricalLastName() {

        // Given a case is created, which defendant record's name will be updated
        final CreateCase.CreateCasePayloadBuilder historicalCaseToBeUpdated = CreateCase.CreateCasePayloadBuilder.withDefaults();
        historicalCaseToBeUpdated
                .getDefendantBuilder()
                .withLastName("deHistorical");
        createCaseForPayloadBuilder(historicalCaseToBeUpdated);

        // and second case is created with the same defendants last name
        final CreateCase.CreateCasePayloadBuilder historicalCaseWithoutUpdates = CreateCase.CreateCasePayloadBuilder.withDefaults();
        historicalCaseWithoutUpdates
                .getDefendantBuilder()
                .withLastName("deHistorical");
        createCaseForPayloadBuilder(historicalCaseWithoutUpdates);

        // when last name is updated for the first case
        UpdateDefendantDetails.DefendantDetailsPayloadBuilder updatedDefendantPayload = UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults()
                .withLastName("von Neumann");

        final UUID caseId = historicalCaseToBeUpdated.getId();
        UpdateDefendantDetails.updateDefendantDetailsForCaseAndPayload(caseId, UUID.fromString(CasePoller.pollUntilCaseByIdIsOk(caseId).getString("defendant.id")), updatedDefendantPayload);

        // then the first case (and second) will be found and system will mark the name as outdated
        pollWithDefaults(searchCases("deHistorical", USER_ID))
                .until(
                        status().is(OK),
                        payload().isJson(
                                allOf(
                                        withJsonPath("foundCasesWithOutdatedDefendantsName", is(true)),
                                        withJsonPath(
                                                "$.results[*]", hasItem(isJson(allOf(
                                                        withJsonPath("urn", is(historicalCaseToBeUpdated.getUrn())),
                                                        withJsonPath("defendant.lastName", is("von Neumann")),
                                                        withJsonPath("defendant.outdated", is(true))

                                                )))
                                        ),
                                        withJsonPath(
                                                "$.results[*]", hasItem(isJson(allOf(
                                                        withJsonPath("urn", is(historicalCaseWithoutUpdates.getUrn())),
                                                        withJsonPath("defendant.lastName", is("deHistorical")),
                                                        withJsonPath("defendant.outdated", is(false))

                                                )))
                                        )
                                )
                        )
                );
    }

    @Test
    public void verifyCaseAssignmentIsReflected() throws Exception {
        databaseCleaner.cleanAll();
        stubStartSjpSessionCommand();
        stubAddAssignmentCommand();
        stubRemoveAssignmentCommand();

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
        caseSearchResultHelper.startSessionAndAssignCase();
        // then
        caseSearchResultHelper.verifyAssignment(true);

        // when
        //TODO change to end session when it is ready (ATCM-2957)
        caseSearchResultHelper.completeCase(createCasePayloadBuilder.getDefendantBuilder().getId(), createCasePayloadBuilder.getOffenceBuilder().getId());
        // then
        caseSearchResultHelper.verifyAssignment(false);
    }

}
