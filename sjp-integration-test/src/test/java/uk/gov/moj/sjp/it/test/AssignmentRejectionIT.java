package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected.RejectReason.SESSION_DOES_NOT_EXIST;
import static uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected.RejectReason.SESSION_ENDED;
import static uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected.RejectReason.SESSION_NOT_OWNED_BY_USER;
import static uk.gov.moj.sjp.it.helper.SessionHelper.endSession;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startDelegatedPowersSessionAndGetSessionStartedEvent;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected;
import uk.gov.moj.sjp.it.helper.AssignmentHelper;
import uk.gov.moj.sjp.it.stub.ReferenceDataStub;

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
        ReferenceDataStub.stubCourtByCourtHouseOUCodeQuery(LONDON_COURT_HOUSE_OU_CODE, LONDON_LJA_NATIONAL_COURT_CODE);
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

    private void requestAssignmentAndVerifyRejectionReason(final UUID sessionId, final UUID userId, final CaseAssignmentRejected.RejectReason rejectionReason) {
        try (final AssignmentHelper assignmentHelper = new AssignmentHelper()) {
            assignmentHelper.requestCaseAssignment(sessionId, userId);

            final JsonEnvelope caseAssignmentRejectedPublicEvent = assignmentHelper.getCaseAssignmentRejectedPublicEvent();
            assertThat(caseAssignmentRejectedPublicEvent,
                    jsonEnvelope(
                            metadata().withName("public.sjp.case-assignment-rejected"),
                            payload().isJson(withJsonPath("$.reason", equalTo(rejectionReason.name())))
                    ));
        }
    }

    private static void startSession(final UUID sessionId, final UUID userId) {
        startDelegatedPowersSessionAndGetSessionStartedEvent(sessionId, userId, LONDON_COURT_HOUSE_OU_CODE);
    }
}