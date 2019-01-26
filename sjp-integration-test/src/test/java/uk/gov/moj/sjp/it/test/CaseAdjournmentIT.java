package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseNotReady;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper;
import uk.gov.moj.sjp.it.producer.CaseAdjournmentProducer;
import uk.gov.moj.sjp.it.stub.AssignmentStub;
import uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub;
import uk.gov.moj.sjp.it.stub.SchedulingStub;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class CaseAdjournmentIT extends BaseIntegrationTest {

    private UUID caseId;
    private UUID sessionId;
    private UUID userId;
    private LocalDate adjournmentDate = now().plusDays(7);
    private SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();

    private static final String LONDON_LJA_NATIONAL_COURT_CODE = "2572";
    private static final String LONDON_COURT_HOUSE_OU_CODE = "B01OK";

    @Before
    public void setUp() throws SQLException {
        caseId = randomUUID();
        sessionId = randomUUID();
        userId = randomUUID();

        databaseCleaner.cleanAll();

        AssignmentStub.stubAddAssignmentCommand();
        AssignmentStub.stubRemoveAssignmentCommand();
        SchedulingStub.stubStartSjpSessionCommand();
        ReferenceDataServiceStub.stubCourtByCourtHouseOUCodeQuery(LONDON_COURT_HOUSE_OU_CODE, LONDON_LJA_NATIONAL_COURT_CODE);

        final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId);

        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
        pollUntilCaseReady(caseId);

        startSession(sessionId, userId, LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);
        requestCaseAssignment(sessionId, userId);

        pollUntilCaseByIdIsOk(caseId, caseAssigned(true));
    }

    @Test
    public void shouldRecordCaseAdjournmentAndChangeCaseStatusToNotReady() {
        caseAdjournedRecordedPrivateEventCreated();
        pollUntilCaseNotReady(caseId);
        pollUntilCaseByIdIsOk(caseId, allOf(caseAssigned(false), caseAdjourned(adjournmentDate)));
    }

    @Test
    public void shouldPutCaseInReadyStateWhenWithdrawalReceivedAfterCaseAdjournment() {
        caseAdjournedRecordedPrivateEventCreated();

        try (OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(caseId)) {
            offencesWithdrawalRequestHelper.requestWithdrawalForAllOffences(USER_ID);
            pollUntilCaseReady(caseId);
            pollUntilCaseByIdIsOk(caseId, allOf(caseAssigned(false), caseAdjourned(adjournmentDate)));
        }
    }

    private void caseAdjournedRecordedPrivateEventCreated() {
        final CaseAdjournmentProducer caseAdjournmentProducer = new CaseAdjournmentProducer(caseId, sessionId, adjournmentDate);

        final EventListener eventListener = new EventListener();
        final Optional<JsonEnvelope> caseAdjournmentRecordedEvent = eventListener
                .subscribe("sjp.events.case-adjourned-to-later-sjp-hearing-recorded")
                .run(caseAdjournmentProducer::adjournCase)
                .popEvent("sjp.events.case-adjourned-to-later-sjp-hearing-recorded");

        assertTrue(caseAdjournmentRecordedEvent.isPresent());

        final JsonObject event = caseAdjournmentRecordedEvent.get().payloadAsJsonObject();
        assertThat(event.getString("adjournedTo"), is(adjournmentDate.toString()));
        assertThat(event.getString("caseId"), is(caseId.toString()));
        assertThat(event.getString("sessionId"), is(sessionId.toString()));
    }

    private Matcher caseAssigned(final boolean assigned) {
        return withJsonPath("$.assigned", is(assigned));
    }

    private Matcher caseAdjourned(final LocalDate adjournedTo) {
        return withJsonPath("$.adjournedTo", is(adjournedTo.toString()));
    }

}
