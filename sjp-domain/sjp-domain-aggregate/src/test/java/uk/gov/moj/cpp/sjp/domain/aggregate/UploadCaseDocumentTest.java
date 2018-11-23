package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.lang.String.format;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import uk.gov.moj.cpp.sjp.event.CaseDocumentUploadRejected;
import uk.gov.moj.cpp.sjp.event.CaseDocumentUploaded;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

public class UploadCaseDocumentTest extends CaseAggregateBaseTest {

    @Test
    public void raisesCaseStartedEvent_whenCaseIsNotStarted() {
        final UUID caseId = UUID.randomUUID();
        final UUID documentReference = UUID.randomUUID();
        final String documentType = "PLEA";

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
        final UUID caseId = UUID.randomUUID();
        final UUID documentReference = UUID.randomUUID();
        final String documentType = "PLEA";

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
        final UUID documentReference = UUID.randomUUID();
        final String documentType = "PLEA";

        givenCaseIsReferredToCourt();

        final List<Object> events = whenUploadCaseDocumentInvoked(documentReference, documentType, this.caseId);

        thenCaseDocumentUploadRejectEventRaised(documentReference, events);

    }

    protected void thenCaseDocumentUploadRejectEventRaised(final UUID documentReference, final List<Object> events) {
        assertThat(events, hasSize(1));
        assertThat(events.get(0), instanceOf(CaseDocumentUploadRejected.class));

        final CaseDocumentUploadRejected caseDocumentUploadRejected = (CaseDocumentUploadRejected) events.get(0);
        assertThat(caseDocumentUploadRejected.getDocumentId(), equalTo(documentReference));
        final String description = format("Case Document %s Upload rejected as case %s is referred to court for hearing", documentReference, caseId);
        assertThat(caseDocumentUploadRejected.getDescription(), equalTo(description));
    }

    protected List<Object> whenUploadCaseDocumentInvoked(final UUID documentReference, final String documentType, final UUID caseId) {
        final Stream<Object> eventsStream = caseAggregate.uploadCaseDocument(caseId, documentReference, documentType);
        return eventsStream.collect(Collectors.toList());
    }

    protected void givenCaseIsReferredToCourt() {
        final UUID sessionId = randomUUID();
        final UUID referralReasonId = randomUUID();
        final UUID hearingTypeId = randomUUID();

        caseAggregate.referCaseForCourtHearing(caseId, sessionId, referralReasonId, hearingTypeId,
                nextInt(0, 999), randomAlphanumeric(100), now(UTC));
    }
}
