package uk.gov.moj.sjp.it.test;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_SJP_CASE_UPDATE_REJECTED;
import static uk.gov.moj.sjp.it.EventSelector.SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED;
import static uk.gov.moj.sjp.it.EventSelector.SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED;
import static uk.gov.moj.sjp.it.EventSelector.SJP_EVENTS_CASE_UPDATE_REJECTED;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithDecision;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithNoDecision;

import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.sjp.it.helper.CaseSearchResultHelper;
import uk.gov.moj.sjp.it.helper.CaseSjpHelper;
import uk.gov.moj.sjp.it.helper.CaseUpdateRejectedHelper;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestCancelHelper;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AllOffencesWithdrawalRequestedIT extends BaseIntegrationTest {

    private CaseSjpHelper caseSjpHelper;
    private UUID userId, otherUserId;

    @Before
    public void setUp() {
        userId = randomUUID();
        otherUserId = randomUUID();
        caseSjpHelper = new CaseSjpHelper();
        caseSjpHelper.createCase();
        caseSjpHelper.verifyCaseCreatedUsingId();
        stubGetCaseDecisionsWithNoDecision(caseSjpHelper.getCaseId());
    }

    @After
    public void tearDown() {
        caseSjpHelper.close();
    }

    @Test
    public void shouldWithdrawThenCancelWithdrawAllOffencesWhenCaseNotAssigned() {
        stubGetEmptyAssignmentsByDomainObjectId(caseSjpHelper.getCaseId());
        try (
                final OffencesWithdrawalRequestCancelHelper offencesWithdrawalRequestCancelHelper = new OffencesWithdrawalRequestCancelHelper(caseSjpHelper,
                        SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED, PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED);
                final OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(caseSjpHelper,
                        SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED, PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED);
                final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(caseSjpHelper)) {

            caseSearchResultHelper.verifyPersonInfoByUrn();

            //check successful standard withdrawal request
            offencesWithdrawalRequestHelper.requestWithdrawalForAllOffences(userId);
            offencesWithdrawalRequestHelper.verifyAllOffencesWithdrawalInPrivateActiveMQ();
            offencesWithdrawalRequestHelper.verifyAllOffencesWithdrawalRequestedInPublicActiveMQ();

            caseSearchResultHelper.verifyWithdrawalRequestedDate();

            //check successful cancel withdrawal request
            offencesWithdrawalRequestCancelHelper.cancelRequestWithdrawalForAllOffences(userId);
            offencesWithdrawalRequestCancelHelper.verifyAllOffencesWithdrawalCancelledInPrivateActiveMQ();
            offencesWithdrawalRequestCancelHelper.verifyAllOffencesWithdrawalRequestCancelledInPublicActiveMQ();

            caseSearchResultHelper.verifyNoWithdrawalRequestedDate();
        }
    }

    @Test
    public void shouldWithdrawThenCancelWithdrawAllOffencesWhenCaseAssignedToCaller() {
        stubGetAssignmentsByDomainObjectId(caseSjpHelper.getCaseId(), userId);
        try (
                final OffencesWithdrawalRequestCancelHelper offencesWithdrawalRequestCancelHelper = new OffencesWithdrawalRequestCancelHelper(caseSjpHelper,
                        SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED, PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED);
                final OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(caseSjpHelper,
                        SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED, PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED);
                final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(caseSjpHelper)) {

            caseSearchResultHelper.verifyPersonInfoByUrn();

            //check successful standard withdrawal request
            offencesWithdrawalRequestHelper.requestWithdrawalForAllOffences(userId);
            offencesWithdrawalRequestHelper.verifyAllOffencesWithdrawalInPrivateActiveMQ();
            offencesWithdrawalRequestHelper.verifyAllOffencesWithdrawalRequestedInPublicActiveMQ();

            caseSearchResultHelper.verifyWithdrawalRequestedDate();

            //check successful cancel withdrawal request
            offencesWithdrawalRequestCancelHelper.cancelRequestWithdrawalForAllOffences(userId);
            offencesWithdrawalRequestCancelHelper.verifyAllOffencesWithdrawalCancelledInPrivateActiveMQ();
            offencesWithdrawalRequestCancelHelper.verifyAllOffencesWithdrawalRequestCancelledInPublicActiveMQ();

            caseSearchResultHelper.verifyNoWithdrawalRequestedDate();
        }
    }

    @Test
    public void shouldRejectWithdrawalWhenCaseAssignedToSomebodyElse() {
        stubGetAssignmentsByDomainObjectId(caseSjpHelper.getCaseId(), otherUserId);
        try (final OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(caseSjpHelper,
                SJP_EVENTS_CASE_UPDATE_REJECTED, PUBLIC_SJP_CASE_UPDATE_REJECTED)) {
            offencesWithdrawalRequestHelper.requestWithdrawalForAllOffences(userId);
            offencesWithdrawalRequestHelper.verifyCaseUpdateRejectedPrivateInActiveMQ(CaseUpdateRejected.RejectReason.CASE_ASSIGNED.name());
            offencesWithdrawalRequestHelper.verifyCaseUpdateRejectedPublicInActiveMQ(CaseUpdateRejected.RejectReason.CASE_ASSIGNED.name());
        }
    }

    @Test
    public void shouldRejectWithdrawalWhenCaseResulted() {
        stubGetEmptyAssignmentsByDomainObjectId(caseSjpHelper.getCaseId());
        stubGetCaseDecisionsWithDecision(caseSjpHelper.getCaseId());
        try (final OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(caseSjpHelper,
                SJP_EVENTS_CASE_UPDATE_REJECTED, PUBLIC_SJP_CASE_UPDATE_REJECTED)) {
            offencesWithdrawalRequestHelper.requestWithdrawalForAllOffences(userId);
            offencesWithdrawalRequestHelper.verifyCaseUpdateRejectedPrivateInActiveMQ(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name());
            offencesWithdrawalRequestHelper.verifyCaseUpdateRejectedPublicInActiveMQ(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name());
        }
    }

    @Test
    public void shouldRejectCancelWithdrawalWhenCaseAssignedToSomebodyElse() {
        stubGetAssignmentsByDomainObjectId(caseSjpHelper.getCaseId(), otherUserId);
        try (
                final OffencesWithdrawalRequestCancelHelper offencesWithdrawalRequestCancelHelper = new OffencesWithdrawalRequestCancelHelper(caseSjpHelper,
                        SJP_EVENTS_CASE_UPDATE_REJECTED, PUBLIC_SJP_CASE_UPDATE_REJECTED);
                final OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(caseSjpHelper,
                        SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED, PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED);
                final CaseUpdateRejectedHelper caseUpdateRejectedHelper = new CaseUpdateRejectedHelper(caseSjpHelper,
                        SJP_EVENTS_CASE_UPDATE_REJECTED, PUBLIC_SJP_CASE_UPDATE_REJECTED)
        ) {
            offencesWithdrawalRequestHelper.requestWithdrawalForAllOffences(userId);
            offencesWithdrawalRequestCancelHelper.cancelRequestWithdrawalForAllOffences(userId);
            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPrivateInActiveMQ(CaseUpdateRejected.RejectReason.CASE_ASSIGNED.name());
            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPublicInActiveMQ(CaseUpdateRejected.RejectReason.CASE_ASSIGNED.name());
        }
    }

    @Test
    public void shouldRejectCancelWithdrawalWhenCaseResulted() {
        stubGetAssignmentsByDomainObjectId(caseSjpHelper.getCaseId(), userId);
        stubGetCaseDecisionsWithDecision(caseSjpHelper.getCaseId());
        try (
                final OffencesWithdrawalRequestCancelHelper offencesWithdrawalRequestCancelHelper = new OffencesWithdrawalRequestCancelHelper(caseSjpHelper,
                        SJP_EVENTS_CASE_UPDATE_REJECTED, PUBLIC_SJP_CASE_UPDATE_REJECTED);
                final OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(caseSjpHelper,
                        SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED, PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED);
                final CaseUpdateRejectedHelper caseUpdateRejectedHelper = new CaseUpdateRejectedHelper(caseSjpHelper,
                        SJP_EVENTS_CASE_UPDATE_REJECTED, PUBLIC_SJP_CASE_UPDATE_REJECTED)
        ) {
            offencesWithdrawalRequestHelper.requestWithdrawalForAllOffences(userId);
            offencesWithdrawalRequestCancelHelper.cancelRequestWithdrawalForAllOffences(userId);
            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPrivateInActiveMQ(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name());
            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPublicInActiveMQ(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name());
        }
    }
}
