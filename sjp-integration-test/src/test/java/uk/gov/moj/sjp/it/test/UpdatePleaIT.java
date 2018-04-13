package uk.gov.moj.sjp.it.test;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.sjp.it.EventSelector.EVENT_SELECTOR_PLEA_CANCELLED;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_SJP_CASE_UPDATE_REJECTED;
import static uk.gov.moj.sjp.it.EventSelector.SJP_EVENTS_CASE_UPDATE_REJECTED;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithDecision;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithNoDecision;

import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CancelPleaHelper;
import uk.gov.moj.sjp.it.helper.CaseSearchResultHelper;
import uk.gov.moj.sjp.it.helper.CaseUpdateRejectedHelper;
import uk.gov.moj.sjp.it.helper.UpdatePleaHelper;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.Before;
import org.junit.Test;

public class UpdatePleaIT extends BaseIntegrationTest {

    static final String PLEA_GUILTY = "GUILTY";
    static final String PLEA_GUILTY_REQUEST_HEARING = "PLEA_GUILTY_REQUEST_HEARING";
    static final String PLEA_NOT_GUILTY = "NOT_GUILTY";
    private UpdatePleaHelper updatePleaHelper;
    private UpdatePleaHelper updateRejectedPleaHelper;

    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    @Before
    public void setUp() {
        this.updatePleaHelper = new UpdatePleaHelper();
        this.updateRejectedPleaHelper = new UpdatePleaHelper(PUBLIC_SJP_CASE_UPDATE_REJECTED);
        this.createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        CreateCase.createCaseForPayloadBuilder(this.createCasePayloadBuilder);
        stubGetCaseDecisionsWithNoDecision(createCasePayloadBuilder.getId());
    }

    @Test
    public void shouldAddUpdateAndCancelPlea() {
        stubGetEmptyAssignmentsByDomainObjectId(createCasePayloadBuilder.getId());
        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper();
             final CancelPleaHelper cancelPleaHelper = new CancelPleaHelper(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId(),
                     EVENT_SELECTOR_PLEA_CANCELLED, PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED)
        ) {
            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(createCasePayloadBuilder.getId(),
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());

            caseSearchResultHelper.verifyPersonInfoByUrn();

            String plea = PLEA_GUILTY;
            final String pleaMethod = "POSTAL";

            updatePleaHelper.updatePlea(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId(), getPleaPayload(plea));
            updatePleaHelper.verifyInPublicTopic(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId(), plea, null);

            updatePleaHelper.verifyPleaUpdated(createCasePayloadBuilder.getId(), plea, pleaMethod);

            caseSearchResultHelper.verifyPleaReceivedDate();

            plea = PLEA_NOT_GUILTY;
            updatePleaHelper.updatePlea(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId(), getPleaPayload(plea));
            updatePleaHelper.verifyInPublicTopic(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId(), plea, null);
            updatePleaHelper.verifyPleaUpdated(createCasePayloadBuilder.getId(), plea, pleaMethod);

            cancelPleaHelper.cancelPlea();
            cancelPleaHelper.verifyInPublicTopic();
            cancelPleaHelper.verifyPleaCancelled();

            caseSearchResultHelper.verifyNoPleaReceivedDate();
        }
    }

    @Test
    public void shouldRejectAddPleaWhenCaseAssigned() {
        stubGetAssignmentsByDomainObjectId(createCasePayloadBuilder.getId(), randomUUID());
        final CaseUpdateRejectedHelper caseUpdateRejectedHelper = new CaseUpdateRejectedHelper(createCasePayloadBuilder.getId(),
                SJP_EVENTS_CASE_UPDATE_REJECTED, PUBLIC_SJP_CASE_UPDATE_REJECTED);

        updatePleaHelper.updatePlea(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId(), getPleaPayload(PLEA_GUILTY));
        caseUpdateRejectedHelper.verifyCaseUpdateRejectedPrivateInActiveMQ(CaseUpdateRejected.RejectReason.CASE_ASSIGNED.name());
        caseUpdateRejectedHelper.verifyCaseUpdateRejectedPublicInActiveMQ(CaseUpdateRejected.RejectReason.CASE_ASSIGNED.name());
    }

    @Test
    public void shouldRejectAddPleaWhenCaseCompleted() {
        stubGetEmptyAssignmentsByDomainObjectId(createCasePayloadBuilder.getId());
        stubGetCaseDecisionsWithDecision(createCasePayloadBuilder.getId());
        final CaseUpdateRejectedHelper caseUpdateRejectedHelper = new CaseUpdateRejectedHelper(createCasePayloadBuilder.getId(),
                SJP_EVENTS_CASE_UPDATE_REJECTED, PUBLIC_SJP_CASE_UPDATE_REJECTED);

        updatePleaHelper.updatePlea(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId(), getPleaPayload(PLEA_GUILTY));
        caseUpdateRejectedHelper.verifyCaseUpdateRejectedPrivateInActiveMQ(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name());
        caseUpdateRejectedHelper.verifyCaseUpdateRejectedPublicInActiveMQ(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name());
    }

    /*
     * Do twice to check serialization works correctly
     */
    @Test
    public void shouldRejectAddPleaWhenCaseCompletedTwice() {
        stubGetEmptyAssignmentsByDomainObjectId(createCasePayloadBuilder.getId());
        stubGetCaseDecisionsWithDecision(createCasePayloadBuilder.getId());

        final CaseUpdateRejectedHelper caseUpdateRejectedHelper = new CaseUpdateRejectedHelper(createCasePayloadBuilder.getId(),
                SJP_EVENTS_CASE_UPDATE_REJECTED, PUBLIC_SJP_CASE_UPDATE_REJECTED);

        updatePleaHelper.updatePlea(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId(), getPleaPayload(PLEA_GUILTY));
        caseUpdateRejectedHelper.verifyCaseUpdateRejectedPrivateInActiveMQ(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name());
        caseUpdateRejectedHelper.verifyCaseUpdateRejectedPublicInActiveMQ(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name());

        updatePleaHelper.updatePlea(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId(), getPleaPayload(PLEA_GUILTY));
        caseUpdateRejectedHelper.verifyCaseUpdateRejectedPrivateInActiveMQ(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name());
        caseUpdateRejectedHelper.verifyCaseUpdateRejectedPublicInActiveMQ(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name());

    }

    private JsonObject getPleaPayload(final String plea) {
        final JsonObjectBuilder builder = createObjectBuilder().add("plea", plea);
        if (PLEA_NOT_GUILTY.equals(plea) || PLEA_GUILTY_REQUEST_HEARING.equals(plea)) {
            builder.add("interpreterRequired", false);
        }
        return builder.build();
    }
}
