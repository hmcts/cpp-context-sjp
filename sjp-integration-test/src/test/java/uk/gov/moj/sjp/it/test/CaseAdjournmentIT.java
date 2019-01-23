package uk.gov.moj.sjp.it.test;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.assertCaseUnassigned;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseNotReady;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.AssignmentHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper;
import uk.gov.moj.sjp.it.producer.CaseAdjournmentProducer;
import uk.gov.moj.sjp.it.stub.AssignmentStub;
import uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub;
import uk.gov.moj.sjp.it.stub.SchedulingStub;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;

public class CaseAdjournmentIT extends BaseIntegrationTest {

    private UUID caseId;
    private UUID sessionId;
    private UUID userId;
    private LocalDate ADJOURNMENT_DATE = LocalDate.now().plusDays(6);

    private static final String LONDON_LJA_NATIONAL_COURT_CODE = "2572";
    private static final String LONDON_COURT_HOUSE_OU_CODE = "B01OK";

    @Before
    public void setUp() {
        caseId = randomUUID();
        sessionId = randomUUID();
        userId = randomUUID();

        AssignmentStub.stubAddAssignmentCommand();
        AssignmentStub.stubRemoveAssignmentCommand();
        SchedulingStub.stubStartSjpSessionCommand();
        ReferenceDataServiceStub.stubCourtByCourtHouseOUCodeQuery(LONDON_COURT_HOUSE_OU_CODE, LONDON_LJA_NATIONAL_COURT_CODE);

        final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId);

        final EventListener eventListener = new EventListener();
        eventListener
                .subscribe(CaseMarkedReadyForDecision.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder));

        startSession(sessionId, userId,LONDON_COURT_HOUSE_OU_CODE, SessionType.MAGISTRATE);

        Optional<JsonEnvelope> caseAssignmentEvent = AssignmentHelper.requestCaseAssignment(sessionId, userId);
        assertThat(caseAssignmentEvent.isPresent(), is(true));
    }

    @Test
    public void shouldRecordCaseAdjournmentAndChangeCaseStatusToNotReady() {
        caseAdjournedRecordedPrivateEventCreated();
        pollUntilCaseNotReady(caseId);
        assertCaseUnassigned(caseId);
    }

    @Test
    public void shouldPutCaseInReadyStateWhenWithdrawalReceivedAfterCaseAdjournment() {
        caseAdjournedRecordedPrivateEventCreated();

        try(OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(caseId)) {
            offencesWithdrawalRequestHelper.requestWithdrawalForAllOffences(USER_ID);
            pollUntilCaseReady(caseId);
            assertCaseUnassigned(caseId);
        }
    }

    private void caseAdjournedRecordedPrivateEventCreated() {
        final CaseAdjournmentProducer caseAdjournmentProducer = new CaseAdjournmentProducer(caseId, sessionId, ADJOURNMENT_DATE);

        final EventListener eventListener = new EventListener();
        final Optional<JsonEnvelope> caseAdjournmentRecordedEvent = eventListener
                .subscribe("sjp.events.case-adjourned-for-later-sjp-hearing-recorded")
                .run(caseAdjournmentProducer::adjournCase)
                .popEvent("sjp.events.case-adjourned-for-later-sjp-hearing-recorded");

        assertTrue(caseAdjournmentRecordedEvent.isPresent());

        final JsonObject event = caseAdjournmentRecordedEvent.get().payloadAsJsonObject();
        assertThat(event.getString("adjournedTo"), is(ADJOURNMENT_DATE.toString()));
        assertThat(event.getString("caseId"), is(caseId.toString()));
        assertThat(event.getString("sessionId"), is(sessionId.toString()));
    }

}
