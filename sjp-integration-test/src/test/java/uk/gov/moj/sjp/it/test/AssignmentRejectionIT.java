package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected.RejectReason.SESSION_DOES_NOT_EXIST;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignmentAndWaitForEvent;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubCourtByCourtHouseOUCodeQuery;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.AssignmentProcessor;
import uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AssignmentRejectionIT extends BaseIntegrationTest {

    private static final String LONDON_LJA_NATIONAL_COURT_CODE = "2572";
    private static final String LONDON_COURT_HOUSE_OU_CODE = "B01OK";

    private UUID sessionId, userId;

    @BeforeEach
    public void init() {
        sessionId = randomUUID();
        userId = randomUUID();

        stubCourtByCourtHouseOUCodeQuery(LONDON_COURT_HOUSE_OU_CODE, LONDON_LJA_NATIONAL_COURT_CODE);
    }

    @Test
    public void shouldRejectCaseAssignmentWhenSessionNotStarted() {
        requestAssignmentAndVerifyRejectionReason(sessionId, userId, SESSION_DOES_NOT_EXIST);
    }

    private void requestAssignmentAndVerifyRejectionReason(final UUID sessionId, final UUID userId, final CaseAssignmentRejected.RejectReason rejectionReason) {
        final Optional<JsonEnvelope> caseAssignmentRejectedPublicEvent = requestCaseAssignmentAndWaitForEvent(sessionId, userId, AssignmentProcessor.PUBLIC_SJP_CASE_ASSIGNMENT_REJECTED);

        assertThat(caseAssignmentRejectedPublicEvent.isPresent(), is(true));
        assertThat(caseAssignmentRejectedPublicEvent.get(),
                jsonEnvelope(
                        metadata().withName(AssignmentProcessor.PUBLIC_SJP_CASE_ASSIGNMENT_REJECTED),
                        payload().isJson(withJsonPath("$.reason", equalTo(rejectionReason.name())))
                ));
    }

}