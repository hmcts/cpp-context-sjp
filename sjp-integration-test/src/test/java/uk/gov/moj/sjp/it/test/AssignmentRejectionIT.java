package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected.RejectReason.SESSION_DOES_NOT_EXIST;
import static uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected.RejectReason.SESSION_ENDED;
import static uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected.RejectReason.SESSION_NOT_OWNED_BY_USER;
import static uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected.RejectReason.STALE_CANDIDATES;
import static uk.gov.moj.sjp.it.Constants.COMMAND_HANDLE_ACTIVE_MQ_QUEUE;
import static uk.gov.moj.sjp.it.Constants.EVENT_CASE_ASSIGNMENT_REJECTED;
import static uk.gov.moj.sjp.it.Constants.EVENT_CASE_MARKED_READY_FOR_DECISION;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_CASE_ASSIGNMENT_REJECTED;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubAssignmentReplicationCommands;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubEndSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubStartSjpSessionCommand;
import static uk.gov.moj.sjp.it.util.QueueUtil.sendToQueue;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.event.processor.AssignmentProcessor;
import uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected;
import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionStarted;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.AssignmentHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.SessionHelper;

import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class AssignmentRejectionIT extends BaseIntegrationTest {

    private static final String LONDON_LJA_NATIONAL_COURT_CODE = "2572";
    private static final String LONDON_COURT_HOUSE_OU_CODE = "B01OK";

    private UUID sessionId, userId;

    @Before
    public void init() {
        sessionId = randomUUID();
        userId = randomUUID();

        stubAssignmentReplicationCommands();

        stubStartSjpSessionCommand();
        stubEndSjpSessionCommand();

        stubCourtByCourtHouseOUCodeQuery(LONDON_COURT_HOUSE_OU_CODE, LONDON_LJA_NATIONAL_COURT_CODE);
    }

    @Test
    public void shouldRejectCaseAssignmentWhenSessionNotStarted() {
        requestAssignmentAndVerifyRejectionReason(sessionId, userId, SESSION_DOES_NOT_EXIST);
    }

    @Test
    public void shouldRejectCaseAssignmentWhenSessionEnded() {
        startSession(sessionId, userId);
        endSession(sessionId, userId);

        requestAssignmentAndVerifyRejectionReason(sessionId, userId, SESSION_ENDED);
    }

    @Test
    public void shouldRejectCaseAssignmentWhenSessionIsNotOwnedByUser() {
        final UUID assignmentRequesterId = randomUUID();

        startSession(sessionId, userId);

        requestAssignmentAndVerifyRejectionReason(sessionId, assignmentRequesterId, SESSION_NOT_OWNED_BY_USER);
    }

    @Test
    public void shouldRejectAssignmentWhenAllCandidatesAreStale() {
        final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        stubRegionByPostcode("1080", "TestRegion");

        final JsonEnvelope assignCaseFromCandidatesListCommand = envelopeFrom(metadataWithRandomUUID("sjp.command.assign-case-from-candidates-list"), createObjectBuilder()
                .add("sessionId", sessionId.toString())
                .add("assignmentCandidates", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("caseId", createCasePayloadBuilder.getId().toString())
                                .add("caseStreamVersion", 10))
                ).build());

        new EventListener()
                .subscribe(EVENT_CASE_MARKED_READY_FOR_DECISION)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder))
                .popEvent(EVENT_CASE_MARKED_READY_FOR_DECISION);

        startSession(sessionId, userId);

        final EventListener caseAssignmentRejectedListener = new EventListener()
                .subscribe(EVENT_CASE_ASSIGNMENT_REJECTED, PUBLIC_EVENT_CASE_ASSIGNMENT_REJECTED)
                .run(() -> sendToQueue(COMMAND_HANDLE_ACTIVE_MQ_QUEUE, assignCaseFromCandidatesListCommand));

        final Optional<JsonEnvelope> caseAssignmentRejectedEvent = caseAssignmentRejectedListener.popEvent(EVENT_CASE_ASSIGNMENT_REJECTED);
        final Optional<JsonEnvelope> caseAssignmentRejectedPublicEvent = caseAssignmentRejectedListener.popEvent(PUBLIC_EVENT_CASE_ASSIGNMENT_REJECTED);

        assertThat(caseAssignmentRejectedEvent.isPresent(), is(true));
        assertThat(caseAssignmentRejectedPublicEvent.isPresent(), is(true));
        assertThat(caseAssignmentRejectedEvent.get().payloadAsJsonObject().getString("reason"), is(STALE_CANDIDATES.name()));
        assertThat(caseAssignmentRejectedPublicEvent.get().payloadAsJsonObject().getString("reason"), is(STALE_CANDIDATES.name()));
    }

    private void requestAssignmentAndVerifyRejectionReason(final UUID sessionId, final UUID userId, final CaseAssignmentRejected.RejectReason rejectionReason) {
        final Optional<JsonEnvelope> caseAssignmentRejectedPublicEvent = AssignmentHelper.requestCaseAssignmentAndWaitForEvent(sessionId, userId, AssignmentProcessor.PUBLIC_SJP_CASE_ASSIGNMENT_REJECTED);

        assertThat(caseAssignmentRejectedPublicEvent.isPresent(), is(true));
        assertThat(caseAssignmentRejectedPublicEvent.get(),
                jsonEnvelope(
                        metadata().withName(AssignmentProcessor.PUBLIC_SJP_CASE_ASSIGNMENT_REJECTED),
                        payload().isJson(withJsonPath("$.reason", equalTo(rejectionReason.name())))
                ));
    }

    private static void startSession(final UUID sessionId, final UUID userId) {
        SessionHelper.startSession(sessionId, userId, LONDON_COURT_HOUSE_OU_CODE, SessionType.DELEGATED_POWERS);
    }

    private static void endSession(final UUID sessionId, final UUID userId) {
        new EventListener()
                .subscribe(DelegatedPowersSessionStarted.EVENT_NAME)
                .run(() -> SessionHelper.endSession(sessionId, userId));
    }

}