package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.moj.cpp.sjp.command.handler.builder.AddCaseDocumentCommandBuilder.anAddCaseDocumentCommand;
import static uk.gov.moj.cpp.sjp.command.handler.builder.AddCaseDocumentCommandBuilder.anMinimumAddCaseDocumentCommand;
import static uk.gov.moj.cpp.sjp.domain.testutils.CaseDocumentBuilder.aCaseDocument;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.CASE_DOCUMENT_ID_STR;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.CASE_DOCUMENT_MATERIAL_ID_STR;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.CASE_DOCUMENT_TYPE_SJPN;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.CASE_ID;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.CASE_ID_STR;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAdded;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAlreadyAdded;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAlreadyExists;
import uk.gov.moj.cpp.sjp.event.CaseStarted;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AddCaseDocumentHandlerTest {

    private CaseAggregate caseAggregate;

    @Mock
    private EventStream eventStream;

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CaseStarted.class,
            CaseDocumentAdded.class,
            CaseDocumentAlreadyExists.class,
            CaseDocumentAlreadyAdded.class
    );

    @InjectMocks
    private AddCaseDocumentHandler addCaseDocumentHandler;

    @Spy
    private Clock clock = new UtcClock();

    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> captor;

    @BeforeEach
    public void setUp() {
        caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any(UUID.class))).thenReturn(eventStream);
        when(aggregateService.get(any(EventStream.class), any())).thenReturn(caseAggregate);
    }

    @AfterEach
    public void tearDown() {
        verify(eventSource).getStreamById(eq(CASE_ID));
        verify(aggregateService).get(eq(eventStream), eq(CaseAggregate.class));
        verifyNoMoreInteractions(eventStream, eventSource, aggregateService);
    }


    @Test
    public void testAddCaseDocument_triggersCaseDocumentAddedEvent() throws Exception {
        JsonEnvelope addCaseDocumentCommand = anAddCaseDocumentCommand()
                .build();
        caseAggregate.receiveCase(CaseBuilder.aDefaultSjpCase().build(), clock.now());

        addCaseDocumentHandler.addCaseDocument(addCaseDocumentCommand);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(addCaseDocumentCommand)
                                        .withName("sjp.events.case-document-added"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", is(CASE_ID_STR)),
                                        withJsonPath("$.caseDocument.id", is(CASE_DOCUMENT_ID_STR)),
                                        withJsonPath("$.caseDocument.materialId", is(CASE_DOCUMENT_MATERIAL_ID_STR)),
                                        withJsonPath("$.caseDocument.documentType", is(CASE_DOCUMENT_TYPE_SJPN))
                                ))))));
    }

    @Test
    public void testAddCaseDocument_whenCaseDocumentAlreadyExists_ReturnCaseDocumentAlreadyExistsEvent() throws Exception {

        JsonEnvelope addCaseDocumentCommand = anAddCaseDocumentCommand()
                .build();
        caseAggregate.receiveCase(CaseBuilder.aDefaultSjpCase().build(), clock.now());
        caseAggregate.addCaseDocument(CASE_ID, aCaseDocument().build());

        addCaseDocumentHandler.addCaseDocument(addCaseDocumentCommand);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(addCaseDocumentCommand)
                                        .withName("sjp.events.case-document-addition-failed"),
                                payloadIsJson(withJsonPath("$.documentId", is(CASE_DOCUMENT_ID_STR)))))));
    }

    @Test
    public void testAddCaseDocument_whenCaseNotCreated_triggerCaseStartedAndCaseDocumentAddedEvents() throws Exception {
        JsonEnvelope addCaseDocumentCommand = anMinimumAddCaseDocumentCommand().build();

        addCaseDocumentHandler.addCaseDocument(addCaseDocumentCommand);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(addCaseDocumentCommand)
                                        .withName("sjp.events.case-document-added"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", is(CASE_ID_STR)),
                                        withJsonPath("$.caseDocument.id", is(CASE_DOCUMENT_ID_STR)),
                                        withJsonPath("$.caseDocument.materialId", is(CASE_DOCUMENT_MATERIAL_ID_STR)),
                                        withoutJsonPath("$.caseDocument.documentType")
                                ))))));
    }

    @Test
    public void testAddCaseDocument_whenRequiredFieldsOnly_triggersCaseDocumentAddedEvent() throws Exception {
        JsonEnvelope addCaseDocumentCommand = anMinimumAddCaseDocumentCommand().build();
        caseAggregate.receiveCase(CaseBuilder.aDefaultSjpCase().build(), clock.now());

        addCaseDocumentHandler.addCaseDocument(addCaseDocumentCommand);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(addCaseDocumentCommand)
                                        .withName("sjp.events.case-document-added"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", is(CASE_ID_STR)),
                                        withJsonPath("$.caseDocument.id", is(CASE_DOCUMENT_ID_STR)),
                                        withJsonPath("$.caseDocument.materialId", is(CASE_DOCUMENT_MATERIAL_ID_STR)),
                                        withoutJsonPath("$.caseDocument.documentType")
                                ))))));
    }
}
