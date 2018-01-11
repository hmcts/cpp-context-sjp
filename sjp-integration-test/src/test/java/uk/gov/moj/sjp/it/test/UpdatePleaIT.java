package uk.gov.moj.sjp.it.test;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.sjp.it.EventSelector.EVENT_SELECTOR_PLEA_CANCELLED;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_STRUCTURE_CASE_UPDATE_REJECTED;
import static uk.gov.moj.sjp.it.EventSelector.STRUCTURE_EVENTS_CASE_UPDATE_REJECTED;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithDecision;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithNoDecision;

import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.sjp.it.helper.CancelPleaHelper;
import uk.gov.moj.sjp.it.helper.CaseSearchResultHelper;
import uk.gov.moj.sjp.it.helper.CaseSjpHelper;
import uk.gov.moj.sjp.it.helper.CaseUpdateRejectedHelper;
import uk.gov.moj.sjp.it.helper.UpdatePleaHelper;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UpdatePleaIT extends BaseIntegrationTest {

    private CaseSjpHelper caseSjpHelper;

    static final String PLEA_GUILTY = "GUILTY";
    static final String PLEA_GUILTY_REQUEST_HEARING = "PLEA_GUILTY_REQUEST_HEARING";
    static final String PLEA_NOT_GUILTY = "NOT_GUILTY";

    @Before
    public void setUp() {
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
    public void shouldAddUpdateAndCancelPlea() {
        stubGetEmptyAssignmentsByDomainObjectId(caseSjpHelper.getCaseId());
        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper(caseSjpHelper);
             final CancelPleaHelper cancelPleaHelper = new CancelPleaHelper(caseSjpHelper,
                     EVENT_SELECTOR_PLEA_CANCELLED, PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED);
             final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(caseSjpHelper)) {

            caseSearchResultHelper.verifyPersonInfoByUrn();

            String plea = PLEA_GUILTY;
            final String pleaMethod = "POSTAL";

            updatePleaHelper.updatePlea(getPleaPayload(plea));
            updatePleaHelper.verifyInPublicTopic(plea, null);

            updatePleaHelper.verifyPleaUpdated(plea, pleaMethod);

            caseSearchResultHelper.verifyPleaReceivedDate();

            plea = PLEA_NOT_GUILTY;
            updatePleaHelper.updatePlea(getPleaPayload(plea));
            updatePleaHelper.verifyInPublicTopic(plea, null);
            updatePleaHelper.verifyPleaUpdated(plea, pleaMethod);

            cancelPleaHelper.cancelPlea(Response.Status.ACCEPTED);
            cancelPleaHelper.verifyInPublicTopic();
            cancelPleaHelper.verifyPleaCancelled();

            caseSearchResultHelper.verifyNoPleaReceivedDate();
        }
    }

    @Test
    public void shouldRejectAddPleaWhenCaseAssigned() {
        stubGetAssignmentsByDomainObjectId(caseSjpHelper.getCaseId(), randomUUID());
        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper(caseSjpHelper);
             final CaseUpdateRejectedHelper caseUpdateRejectedHelper = new CaseUpdateRejectedHelper(caseSjpHelper,
                     STRUCTURE_EVENTS_CASE_UPDATE_REJECTED, PUBLIC_STRUCTURE_CASE_UPDATE_REJECTED)) {

            updatePleaHelper.updatePlea(getPleaPayload(PLEA_GUILTY));
            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPrivateInActiveMQ(CaseUpdateRejected.RejectReason.CASE_ASSIGNED.name());
            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPublicInActiveMQ(CaseUpdateRejected.RejectReason.CASE_ASSIGNED.name());
        }
    }

    @Test
    public void shouldRejectAddPleaWhenCaseCompleted() {
        stubGetEmptyAssignmentsByDomainObjectId(caseSjpHelper.getCaseId());
        stubGetCaseDecisionsWithDecision(caseSjpHelper.getCaseId());

        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper(caseSjpHelper);
             final CaseUpdateRejectedHelper caseUpdateRejectedHelper = new CaseUpdateRejectedHelper(caseSjpHelper,
                     STRUCTURE_EVENTS_CASE_UPDATE_REJECTED, PUBLIC_STRUCTURE_CASE_UPDATE_REJECTED)) {

            updatePleaHelper.updatePlea(getPleaPayload(PLEA_GUILTY));
            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPrivateInActiveMQ(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name());
            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPublicInActiveMQ(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name());
        }
    }

    /*
     * Do twice to check serialization works correctly
     */
    @Test
    public void shouldRejectAddPleaWhenCaseCompletedTwice() {
        stubGetEmptyAssignmentsByDomainObjectId(caseSjpHelper.getCaseId());
        stubGetCaseDecisionsWithDecision(caseSjpHelper.getCaseId());

        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper(caseSjpHelper,
                STRUCTURE_EVENTS_CASE_UPDATE_REJECTED, PUBLIC_STRUCTURE_CASE_UPDATE_REJECTED);
             final CaseUpdateRejectedHelper caseUpdateRejectedHelper = new CaseUpdateRejectedHelper(caseSjpHelper,
                     STRUCTURE_EVENTS_CASE_UPDATE_REJECTED, PUBLIC_STRUCTURE_CASE_UPDATE_REJECTED)) {

            updatePleaHelper.updatePlea(getPleaPayload(PLEA_GUILTY));
            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPrivateInActiveMQ(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name());
            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPublicInActiveMQ(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name());
        }

        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper(caseSjpHelper,
                STRUCTURE_EVENTS_CASE_UPDATE_REJECTED, PUBLIC_STRUCTURE_CASE_UPDATE_REJECTED);
             final CaseUpdateRejectedHelper caseUpdateRejectedHelper = new CaseUpdateRejectedHelper(caseSjpHelper,
                     STRUCTURE_EVENTS_CASE_UPDATE_REJECTED, PUBLIC_STRUCTURE_CASE_UPDATE_REJECTED)) {

            updatePleaHelper.updatePlea(getPleaPayload(PLEA_GUILTY));
            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPrivateInActiveMQ(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name());
            caseUpdateRejectedHelper.verifyCaseUpdateRejectedPublicInActiveMQ(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name());
        }
    }

    private JsonObject getPleaPayload(final String plea) {
        final JsonObjectBuilder builder = createObjectBuilder().add("plea", plea);
        if (PLEA_NOT_GUILTY.equals(plea) || PLEA_GUILTY_REQUEST_HEARING.equals(plea)) {
            builder.add("interpreterRequired", false);
        }
        return builder.build();
    }
}
