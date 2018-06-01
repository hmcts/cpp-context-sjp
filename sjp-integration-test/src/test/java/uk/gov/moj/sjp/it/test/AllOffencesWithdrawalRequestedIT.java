package uk.gov.moj.sjp.it.test;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED;
import static uk.gov.moj.sjp.it.Constants.SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED;
import static uk.gov.moj.sjp.it.Constants.SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;

import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseSearchResultHelper;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestCancelHelper;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class AllOffencesWithdrawalRequestedIT extends BaseIntegrationTest {

    private UUID userId;
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    @Before
    public void setUp() {
        userId = randomUUID();
        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        createCaseForPayloadBuilder(createCasePayloadBuilder);
    }

    @Test
    public void shouldWithdrawThenCancelWithdrawAllOffencesWhenCaseNotAssigned() {
        try (
                final OffencesWithdrawalRequestCancelHelper offencesWithdrawalRequestCancelHelper = new OffencesWithdrawalRequestCancelHelper(createCasePayloadBuilder.getId(),
                        SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED, PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED);
                final OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(createCasePayloadBuilder.getId(),
                        SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED, PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED)) {
            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(createCasePayloadBuilder.getId(),
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());
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
        try (
                final OffencesWithdrawalRequestCancelHelper offencesWithdrawalRequestCancelHelper = new OffencesWithdrawalRequestCancelHelper(createCasePayloadBuilder.getId(),
                        SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED, PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUEST_CANCELLED);
                final OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(createCasePayloadBuilder.getId(),
                        SJP_EVENTS_ALL_OFFENCES_WITHDRAWAL_REQUESTED, PUBLIC_SJP_ALL_OFFENCES_WITHDRAWAL_REQUESTED)
                ) {
            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(createCasePayloadBuilder.getId(),
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());
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

}
