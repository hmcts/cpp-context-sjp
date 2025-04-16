package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PIA;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.WITHDRAWAL_REQUESTED;
import static uk.gov.moj.cpp.sjp.domain.Priority.HIGH;
import static uk.gov.moj.cpp.sjp.domain.Priority.LOW;
import static uk.gov.moj.cpp.sjp.domain.Priority.MEDIUM;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.sjp.it.Constants.EVENT_OFFENCES_WITHDRAWAL_STATUS_SET;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.command.AddDatesToAvoid.addDatesToAvoid;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.SetPleasHelper.requestSetPleasAndConfirm;
import static uk.gov.moj.sjp.it.stub.IdMapperStub.stubForIdMapperSuccess;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubWithdrawalReasons;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;

import uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseHelper;
import uk.gov.moj.sjp.it.helper.CaseSearchResultHelper;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper;
import uk.gov.moj.sjp.it.helper.ReadyCaseHelper;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;

public class ReadyCaseIT extends BaseIntegrationTest {

    private final UUID caseId = randomUUID();
    private final UUID userId = randomUUID();

    private final UUID offence1Id = randomUUID();
    private final UUID offence2Id = randomUUID();
    private final UUID offence3Id = randomUUID();
    private final UUID defendantId = randomUUID();

    private final UUID withdrawalRequestReasonId = randomUUID();

    private final ReadyCaseHelper readyCaseHelper = new ReadyCaseHelper();

    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    private static final String NATIONAL_COURT_CODE = "1080";

    @Test
    public void shouldChangeCaseReadinessWhenCaseAfterNoticeEndDate() throws Exception {
        stubForUserDetails(userId, "ALL");
        stubForIdMapperSuccess(Response.Status.OK);
        stubWithdrawalReasons();

        try (OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(userId, EVENT_OFFENCES_WITHDRAWAL_STATUS_SET)) {
            // create a case which is more than 28 days old
            final LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);
            createCaseAndWaitForTheCaseToBeReady(caseId, defendantId, offence1Id, offence2Id, offence3Id, postingDate);

            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());

            // verify the case status as PIA
            readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, PIA, MAGISTRATE, LOW);
            verifyCaseReadyInViewStore(caseId, PIA);
            caseSearchResultHelper.verifyCaseStatus(CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION);

            // verify the case status as PLEA_RECEIVED_READY_FOR_DECISION when all the pleas are set ti GUILTY
            requestSetPleasAndConfirm(caseId,
                    true,
                    false,
                    true,
                    null,
                    false,
                    null,
                    asList(Triple.of(offence1Id, defendantId, GUILTY),
                            Triple.of(offence2Id, defendantId, GUILTY),
                            Triple.of(offence3Id, defendantId, GUILTY)));
            verifyCaseReadyInViewStore(caseId, PLEADED_GUILTY);
            caseSearchResultHelper.verifyCaseStatus(CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION);
            readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, PLEADED_GUILTY, MAGISTRATE, MEDIUM);


            // verify the case status as WITHDRAWAL_REQUEST_READY_FOR_DECISION when all the offences are withdrawn
            offencesWithdrawalRequestHelper.requestWithdrawalOfOffences(caseId, getRequestWithdrawalPayload());

            caseSearchResultHelper.verifyCaseStatus(CaseStatus.WITHDRAWAL_REQUEST_READY_FOR_DECISION);
            readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, WITHDRAWAL_REQUESTED, DELEGATED_POWERS, HIGH);
            verifyCaseReadyInViewStore(caseId, WITHDRAWAL_REQUESTED);


            // cancel one of the withdrawal
            List<WithdrawalRequestsStatus> withdrawalRequestsStatusList = getRequestWithdrawalPayload();
            withdrawalRequestsStatusList.remove(2);
            offencesWithdrawalRequestHelper.requestWithdrawalOfOffences(caseId, withdrawalRequestsStatusList);

            caseSearchResultHelper.verifyCaseStatus(CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION);
            readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, PLEADED_GUILTY, MAGISTRATE, MEDIUM);
            verifyCaseReadyInViewStore(caseId, PLEADED_GUILTY);

            requestSetPleasAndConfirm(caseId,
                    true,
                    false,
                    true,
                    null,
                    false,
                    null,
                    asList(Triple.of(offence1Id, defendantId, null),
                            Triple.of(offence2Id, defendantId, null),
                            Triple.of(offence3Id, defendantId, null)));
            caseSearchResultHelper.verifyCaseStatus(CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION);
            readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, PIA, MAGISTRATE, LOW);
            verifyCaseReadyInViewStore(caseId, PIA);
        }
    }

    @Test
    public void shouldChangeCaseReadinessWhenCaseBeforeNoticeEndDate() throws Exception {
        stubForUserDetails(userId, "ALL");
        stubForIdMapperSuccess(Response.Status.OK);

        try (OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(userId, EVENT_OFFENCES_WITHDRAWAL_STATUS_SET)) {
            // create a case which is more than 28 days old
            final LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS - 1);
            createCase(caseId, defendantId, offence1Id, offence2Id, offence3Id, postingDate);

            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());

            caseSearchResultHelper.verifyCaseStatus(CaseStatus.NO_PLEA_RECEIVED);

            // verify the case status as PLEA_RECEIVED_READY_FOR_DECISION
            requestSetPleasAndConfirm(caseId,
                    true,
                    false,
                    true,
                    null,
                    false,
                    null,
                    asList(Triple.of(offence1Id, defendantId, GUILTY),
                            Triple.of(offence2Id, defendantId, GUILTY),
                            Triple.of(offence3Id, defendantId, GUILTY)));
            caseSearchResultHelper.verifyCaseStatus(CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION);
            readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, PLEADED_GUILTY, MAGISTRATE, MEDIUM);
            verifyCaseReadyInViewStore(caseId, PLEADED_GUILTY);

            // verify the case status as PLEA_RECEIVED_READY_FOR_DECISION
            requestSetPleasAndConfirm(caseId,
                    true,
                    false,
                    true,
                    null,
                    false,
                    null,
                    asList(Triple.of(offence1Id, defendantId, NOT_GUILTY),
                            Triple.of(offence2Id, defendantId, NOT_GUILTY),
                            Triple.of(offence3Id, defendantId, NOT_GUILTY)));
            readyCaseHelper.verifyCaseUnmarkedReadyForDecisionEventEmitted(caseId, now().plusDays(10));
            CaseHelper.pollUntilCaseNotReady(caseId);

            addDatesToAvoid(caseId, "my-dates-to-avoid");

            // verify the case status as PLEA_RECEIVED_READY_FOR_DECISION
            readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, PLEADED_NOT_GUILTY, DELEGATED_POWERS, MEDIUM);
            verifyCaseReadyInViewStore(caseId, PLEADED_NOT_GUILTY);
            caseSearchResultHelper.verifyCaseStatus(CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION);

            // verify the case status as NO_PLEA_RECEIVED
            requestSetPleasAndConfirm(caseId,
                    true,
                    false,
                    true,
                    null,
                    false,
                    null,
                    asList(Triple.of(offence1Id, defendantId, null),
                            Triple.of(offence2Id, defendantId, null),
                            Triple.of(offence3Id, defendantId, null)));
            readyCaseHelper.verifyCaseUnmarkedReadyForDecisionEventEmitted(caseId, postingDate.plusDays(28));
            caseSearchResultHelper.verifyCaseStatus(CaseStatus.NO_PLEA_RECEIVED);
            CaseHelper.pollUntilCaseNotReady(caseId);

            // verify the case status is WITHDRAWAL_REQUEST_READY_FOR_DECISION
            offencesWithdrawalRequestHelper.requestWithdrawalOfOffences(caseId, getRequestWithdrawalPayload());
            caseSearchResultHelper.verifyCaseStatus(CaseStatus.WITHDRAWAL_REQUEST_READY_FOR_DECISION);
            readyCaseHelper.verifyCaseMarkedReadyForDecisionEventEmitted(caseId, WITHDRAWAL_REQUESTED, DELEGATED_POWERS, HIGH);
            verifyCaseReadyInViewStore(caseId, WITHDRAWAL_REQUESTED);

            // cancel one of the withdrawal
            List<WithdrawalRequestsStatus> withdrawalRequestsStatusList = getRequestWithdrawalPayload();
            withdrawalRequestsStatusList.remove(2);
            offencesWithdrawalRequestHelper.requestWithdrawalOfOffences(caseId, withdrawalRequestsStatusList);

            caseSearchResultHelper.verifyCaseStatus(CaseStatus.NO_PLEA_RECEIVED);
            CaseHelper.pollUntilCaseNotReady(caseId);
        }

    }

    private static void verifyCaseReadyInViewStore(final UUID caseId, final CaseReadinessReason readinessReason) {
        CaseHelper.pollReadyCasesUntilResponseIsJson((withJsonPath("readyCases.*", hasItem(
                isJson(allOf(
                        withJsonPath("caseId", equalTo(caseId.toString())),
                        withJsonPath("reason", equalTo(readinessReason.name())))
                )))));
    }


    private List<WithdrawalRequestsStatus> getRequestWithdrawalPayload() {
        final List<WithdrawalRequestsStatus> withdrawalRequestsStatuses = new ArrayList<>();
        withdrawalRequestsStatuses.add(new WithdrawalRequestsStatus(offence1Id, withdrawalRequestReasonId));
        withdrawalRequestsStatuses.add(new WithdrawalRequestsStatus(offence2Id, withdrawalRequestReasonId));
        withdrawalRequestsStatuses.add(new WithdrawalRequestsStatus(offence3Id, withdrawalRequestReasonId));
        return withdrawalRequestsStatuses;
    }

    private void createCaseAndWaitForTheCaseToBeReady(
            final UUID caseId,
            final UUID defendantId,
            final UUID offence1Id,
            final UUID offence2Id,
            final UUID offence3Id,
            final LocalDate postingDate) {
        createCase(caseId, defendantId, offence1Id, offence2Id, offence3Id, postingDate);
        pollUntilCaseReady(caseId);
    }

    private void createCase(
            final UUID caseId,
            final UUID defendantId,
            final UUID offence1Id,
            final UUID offence2Id,
            final UUID offence3Id,
            final LocalDate postingDate) {
        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withDefendantId(defendantId)
                .withOffenceBuilders(CreateCase.OffenceBuilder.withDefaults().withId(offence1Id),
                        CreateCase.OffenceBuilder.withDefaults().withId(offence2Id),
                        CreateCase.OffenceBuilder.withDefaults().withId(offence3Id))
                .withPostingDate(postingDate);

        final ProsecutingAuthority prosecutingAuthority = createCasePayloadBuilder.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, "TestRegion");

        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
    }

}
