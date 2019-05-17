package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.json.schemas.domains.sjp.ListingDetails.listingDetails;
import static uk.gov.justice.json.schemas.domains.sjp.Note.note;
import static uk.gov.justice.json.schemas.domains.sjp.User.user;

import uk.gov.justice.json.schemas.domains.sjp.ListingDetails;
import uk.gov.justice.json.schemas.domains.sjp.NoteType;
import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.moj.cpp.sjp.event.CaseDocumentUploadRejected;
import uk.gov.moj.cpp.sjp.event.CaseDocumentUploaded;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

public class UploadCaseDocumentTest extends CaseAggregateBaseTest {

    final UUID caseId = randomUUID();
    final UUID decisionId = randomUUID();
    final UUID sessionId = randomUUID();
    final UUID documentReference = randomUUID();
    final String documentType = "PLEA";

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
        final User legalAdviser = user().build();
        final ListingDetails listingDetails = listingDetails()
                .withReferralReasonId(randomUUID())
                .withHearingTypeId(randomUUID())
                .withListingNotes(note()
                        .withId(randomUUID())
                        .withText("Note")
                        .withType(NoteType.LISTING)
                        .withAddedAt(ZonedDateTime.now())
                        .build())
                .build();

        caseAggregate.referCaseForCourtHearing(caseId, decisionId, sessionId, legalAdviser, listingDetails);
    }
}
