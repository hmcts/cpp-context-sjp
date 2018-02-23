package uk.gov.moj.cpp.sjp.domain.aggregate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

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

}
