package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.event.CaseDocumentUploadRejected;
import uk.gov.moj.cpp.sjp.event.CaseDocumentUploaded;
import uk.gov.moj.cpp.sjp.event.CaseStarted;

import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UploadCaseDocumentHandlerTest {

    private static final String CASE_ID_PROPERTY = "caseId";
    private static final String CASE_DOCUMENT_REFERENCE_PROPERTY = "caseDocument";
    private static final String CASE_DOCUMENT_TYPE_PROPERTY = "caseDocumentType";

    @Mock
    private Stream<Object> events;

    @Mock
    private EventStream eventStream;

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private UploadCaseDocumentHandler uploadCaseDocumentHandler;

    @Spy
    private CaseAggregate caseAggregate;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CaseDocumentUploaded.class,
            CaseStarted.class,
            CaseDocumentUploadRejected.class
    );

    private static final UUID CASE_ID = randomUUID();
    private static final UUID DOCUMENT_REFERENCE = randomUUID();
    private static final String DOCUMENT_TYPE = "PLEA";


    @BeforeEach
    public void setUp() {
        when(aggregateService.get(any(), any())).thenReturn(caseAggregate);
        caseAggregate.getState().setManagedByAtcm(true);
    }

    @Test
    public void shouldProcessAssociateEnterpriseIdCommandAndGenerateAppendExpectedEvent() throws EventStreamException {

        final JsonEnvelope command = createCaseDocumentUploadCommand(CASE_ID, DOCUMENT_REFERENCE, DOCUMENT_TYPE);
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);

        uploadCaseDocumentHandler.handle(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.case-document-uploaded"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(CASE_ID.toString())),
                                        withJsonPath("$.documentReference", equalTo(DOCUMENT_REFERENCE.toString())),
                                        withJsonPath("$.documentType", equalTo(DOCUMENT_TYPE))

                                )))
                )));
    }

    @Test
    public void shouldRejectUploadOfCaseDocumentWhenCaseIsInReferredToCourtState() throws EventStreamException {
        final CaseAggregate caseAggregate1 = mock(CaseAggregate.class);
        when(aggregateService.get(any(), any())).thenReturn(caseAggregate1);

        final JsonEnvelope command = createCaseDocumentUploadCommand(CASE_ID, DOCUMENT_REFERENCE, DOCUMENT_TYPE);
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);

        final CaseDocumentUploadRejected caseDocumentUploadRejected = new CaseDocumentUploadRejected(DOCUMENT_REFERENCE, "");
        when(caseAggregate1.uploadCaseDocument(CASE_ID, DOCUMENT_REFERENCE, DOCUMENT_TYPE)).thenReturn(Stream.of(caseDocumentUploadRejected));

        uploadCaseDocumentHandler.handle(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.case-document-upload-rejected"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.documentId", equalTo(DOCUMENT_REFERENCE.toString())),
                                        withJsonPath("$.description", equalTo(""))

                                )))
                )));
    }


    private JsonEnvelope createCaseDocumentUploadCommand(final UUID caseId, final UUID caseDocumentReference, final String documentType) {
        final JsonObjectBuilder payload = createObjectBuilder()
                .add(CASE_ID_PROPERTY, caseId.toString())
                .add(CASE_DOCUMENT_REFERENCE_PROPERTY, caseDocumentReference.toString())
                .add(CASE_DOCUMENT_TYPE_PROPERTY, documentType);

        return envelopeFrom(
                metadataOf(randomUUID(), "sjp.command.upload-case-document"),
                payload.build());
    }
}