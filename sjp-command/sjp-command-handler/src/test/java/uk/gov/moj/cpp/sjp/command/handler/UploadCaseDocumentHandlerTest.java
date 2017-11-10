package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataOf;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.event.CaseDocumentUploaded;
import uk.gov.moj.cpp.sjp.event.CaseStarted;

import java.util.UUID;

import javax.json.JsonObjectBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UploadCaseDocumentHandlerTest {

    private static final String CASE_ID_PROPERTY = "caseId";
    private static final String CASE_DOCUMENT_REFERENCE_PROPERTY = "caseDocument";
    private static final String CASE_DOCUMENT_TYPE_PROPERTY = "caseDocumentType";

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
            CaseStarted.class
    );

    @Before
    public void setUp() {
        caseAggregate = new CaseAggregate();
        when(aggregateService.get(any(), any())).thenReturn(caseAggregate);
    }

    @Test
    public void shouldProcessAssociateEnterpriseIdCommandAndGenerateAppendExpectedEvent() throws EventStreamException {
        final UUID caseId = randomUUID();
        final UUID caseDocumentReference = randomUUID();
        final String documentType = "PLEA";

        final JsonEnvelope command = createCaseDocumentUploadCommand(caseId, caseDocumentReference, documentType);

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);

        uploadCaseDocumentHandler.handle(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("structure.events.case-document-uploaded"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                                        withJsonPath("$.documentReference", equalTo(caseDocumentReference.toString())),
                                        withJsonPath("$.documentType", equalTo(documentType))

                                )))
                )));
    }

    private JsonEnvelope createCaseDocumentUploadCommand(final UUID caseId, final UUID caseDocumentReference, final String documentType) {
        final JsonObjectBuilder payload = createObjectBuilder()
                .add(CASE_ID_PROPERTY, caseId.toString())
                .add(CASE_DOCUMENT_REFERENCE_PROPERTY, caseDocumentReference.toString())
                .add(CASE_DOCUMENT_TYPE_PROPERTY, documentType);

        return JsonEnvelopeBuilder.envelopeFrom(
                metadataOf(randomUUID(), "structure.command.upload-case-document"),
                payload.build());
    }

}