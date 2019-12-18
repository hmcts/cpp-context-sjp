package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtInterpreter;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.decision.Decision;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.CaseDocumentUploadRejected;
import uk.gov.moj.cpp.sjp.event.CaseDocumentUploaded;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UploadCaseDocumentTest extends CaseAggregateBaseTest {

    final UUID caseId = randomUUID();
    final UUID decisionId = randomUUID();
    final UUID sessionId = randomUUID();
    final UUID documentReference = randomUUID();
    final UUID userId = randomUUID();
    final String documentType = "PLEA";
    final ZonedDateTime savedAt = ZonedDateTime.now();
    final UUID REFERRAL_REASON_ID = randomUUID();

    @Test
    public void raisesCaseStartedEvent_whenCaseIsNotStarted() {

        final Stream<Object> eventsStream = caseAggregate.uploadCaseDocument(caseId, documentReference, documentType);
        final List<Object> events = eventsStream.collect(Collectors.toList());

        assertThat(events, hasSize(1));

        assertThat(events.get(0), instanceOf(CaseDocumentUploaded.class));
        CaseDocumentUploaded caseDocumentUploaded = (CaseDocumentUploaded) events.get(0);
        assertThat(caseDocumentUploaded.getCaseId(), equalTo(caseId));
        assertThat(caseDocumentUploaded.getDocumentReference(), equalTo(documentReference));
        assertThat(caseDocumentUploaded.getDocumentType(), equalTo(documentType));
    }

    @Test
    public void raisesOnlyCaseDocumentsUploaded_whenCaseIsCreated() {

        final Stream<Object> eventsStream = caseAggregate.uploadCaseDocument(caseId, documentReference, documentType);
        final List<Object> events = eventsStream.collect(Collectors.toList());

        assertThat(events, hasSize(1));

        assertThat(events.get(0), instanceOf(CaseDocumentUploaded.class));
        CaseDocumentUploaded caseDocumentUploaded = (CaseDocumentUploaded) events.get(0);
        assertThat(caseDocumentUploaded.getCaseId(), equalTo(caseId));
        assertThat(caseDocumentUploaded.getDocumentReference(), equalTo(documentReference));
        assertThat(caseDocumentUploaded.getDocumentType(), equalTo(documentType));
    }

    @Test
    public void raisesOnlyCaseDocumentsUploadRejectedEventWhenCaseIsReferredToCourt() {

        givenCaseIsReferredToCourt();

        final List<Object> events = whenUploadCaseDocumentInvoked(documentReference, documentType, this.caseId);

        thenCaseDocumentUploadRejectEventRaised(documentReference, events);
    }

    private void thenCaseDocumentUploadRejectEventRaised(final UUID documentReference, final List<Object> events) {
        assertThat(events, hasSize(1));
        assertThat(events.get(0), instanceOf(CaseDocumentUploadRejected.class));

        final CaseDocumentUploadRejected caseDocumentUploadRejected = (CaseDocumentUploadRejected) events.get(0);
        assertThat(caseDocumentUploadRejected.getDocumentId(), equalTo(documentReference));
        final String description = format("Case Document %s Upload rejected as case %s is referred to court for hearing", documentReference, caseId);
        assertThat(caseDocumentUploadRejected.getDescription(), equalTo(description));
    }

    private List<Object> whenUploadCaseDocumentInvoked(final UUID documentReference, final String documentType, final UUID caseId) {
        final Stream<Object> eventsStream = caseAggregate.uploadCaseDocument(caseId, documentReference, documentType);
        return eventsStream.collect(Collectors.toList());
    }

    private void givenCaseIsReferredToCourt() {
        final User savedBy = new User("John", "Smith", userId);
        final DefendantCourtOptions courtOptions =
                new DefendantCourtOptions(
                        new DefendantCourtInterpreter("EN", true),
                        false);

        final List<OffenceDecisionInformation> offenceDecisionInformation = new ArrayList<>();
        offenceDecisionInformation.add(OffenceDecisionInformation.createOffenceDecisionInformation(offenceId, VerdictType.NO_VERDICT));
        final List<OffenceDecision> offenceDecisions = new ArrayList<>();
        final OffenceDecision offenceDecision =
                new ReferForCourtHearing(
                        randomUUID(),
                        offenceDecisionInformation,
                        REFERRAL_REASON_ID,
                        "Note",
                        30,
                        courtOptions);
        offenceDecisions.add(offenceDecision);

        final Decision decision = new Decision(decisionId, sessionId, caseId, "duplicate conviction", savedAt, savedBy, offenceDecisions, null);

        caseAggregate.assignCase(savedBy.getUserId(), ZonedDateTime.now(), CaseAssignmentType.MAGISTRATE_DECISION);
        when(session.getSessionType()).thenReturn(SessionType.MAGISTRATE);
        when(session.getLocalJusticeAreaNationalCourtCode()).thenReturn("1080");
        caseAggregate.saveDecision(decision, session);
    }
}
