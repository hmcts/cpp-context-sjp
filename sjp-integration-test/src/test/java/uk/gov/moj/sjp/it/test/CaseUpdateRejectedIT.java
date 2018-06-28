package uk.gov.moj.sjp.it.test;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.jgroups.util.Util.assertTrue;
import static org.junit.Assert.assertThat;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.cpp.sjp.event.processor.AssignmentProcessor;
import uk.gov.moj.sjp.it.Constants;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.AssignmentHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper;
import uk.gov.moj.sjp.it.helper.SessionHelper;
import uk.gov.moj.sjp.it.producer.CompleteCaseProducer;
import uk.gov.moj.sjp.it.stub.AssignmentStub;
import uk.gov.moj.sjp.it.stub.ReferenceDataStub;
import uk.gov.moj.sjp.it.stub.SchedulingStub;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;

public class CaseUpdateRejectedIT extends BaseIntegrationTest {

    private static final String LONDON_LJA_NATIONAL_COURT_CODE = "2572";
    private static final String LONDON_COURT_HOUSE_OU_CODE = "B01OK";

    private UUID caseId;

    @Before
    public void init() throws SQLException {

        new SjpDatabaseCleaner().cleanAll();

        ReferenceDataStub.stubCourtByCourtHouseOUCodeQuery(LONDON_COURT_HOUSE_OU_CODE, LONDON_LJA_NATIONAL_COURT_CODE);
        //TODO remove after ATCM-3219
        SchedulingStub.stubStartSjpSessionCommand();
        AssignmentStub.stubAddAssignmentCommand();

        caseId = randomUUID();

        final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase
                .CreateCasePayloadBuilder.withDefaults().withId(caseId);

        new EventListener()
                .subscribe(CaseMarkedReadyForDecision.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder));
    }

    @Test
    public void shouldRejectWhenCaseAssigned() {
        final UUID sessionId = randomUUID();
        final UUID caseId = assignCase(sessionId);
        assertCaseUpdateRejected(caseId, CaseUpdateRejected.RejectReason.CASE_ASSIGNED);
    }

    @Test
    public void shouldRejectWhenCaseCompleted() {
        completeCase(caseId);
        assertCaseUpdateRejected(caseId, CaseUpdateRejected.RejectReason.CASE_COMPLETED);
    }

    private void requestWithdrawalForAllOffences(final UUID caseId) {
        try (final OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper =
                     new OffencesWithdrawalRequestHelper(caseId)) {
            offencesWithdrawalRequestHelper.requestWithdrawalForAllOffences(caseId);
        }
    }

    private void assertCaseUpdateRejected(final UUID caseId, final CaseUpdateRejected.RejectReason rejectReason) {

        final Optional<JsonEnvelope> jsonEnvelope = new EventListener()
                .subscribe(Constants.PUBLIC_SJP_CASE_UPDATE_REJECTED)
                .run(() -> requestWithdrawalForAllOffences(caseId))
                .popEvent(Constants.PUBLIC_SJP_CASE_UPDATE_REJECTED);

        assertTrue(jsonEnvelope.isPresent());
        final JsonObject payload = jsonEnvelope.get().payloadAsJsonObject();
        assertThat(payload.getString("caseId"), equalTo(caseId.toString()));
        assertThat(payload.getString("reason"), equalTo(rejectReason.name()));

    }

    private UUID assignCase(final UUID sessionId) {

        final UUID userId = randomUUID();

        SessionHelper.startMagistrateSession(sessionId, userId, LONDON_COURT_HOUSE_OU_CODE, "Reggie Gates");

        final Optional<JsonEnvelope> jsonEnvelope = AssignmentHelper.requestCaseAssignmentAndWaitForEvent(sessionId, userId, AssignmentProcessor.PUBLIC_SJP_CASE_ASSIGNED);

        return jsonEnvelope.map(envelope ->
                UUID.fromString(envelope.payloadAsJsonObject().getString("caseId")))
                .orElseThrow(IllegalStateException::new);
    }

    private void completeCase(final UUID caseId) {
        final CompleteCaseProducer completeCaseProducer = new CompleteCaseProducer(caseId);
        completeCaseProducer.completeCase();
    }

}
