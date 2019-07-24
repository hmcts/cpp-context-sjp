package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static javax.json.JsonValue.NULL;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.json.schemas.domains.sjp.AddCaseNote.addCaseNote;
import static uk.gov.justice.json.schemas.domains.sjp.Note.note;
import static uk.gov.justice.json.schemas.domains.sjp.NoteType.DECISION;
import static uk.gov.justice.json.schemas.domains.sjp.User.user;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.event.CaseNoteAdded.caseNoteAdded;

import uk.gov.justice.json.schemas.domains.sjp.AddCaseNote;
import uk.gov.justice.json.schemas.domains.sjp.Note;
import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.event.CaseNoteAdded;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseNoteHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private CaseAggregate caseAggregate;

    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> argumentCaptor;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(CaseNoteAdded.class);

    @InjectMocks
    private CaseNoteHandler caseNoteHandler;

    @Test
    public void shouldHandleCaseNoteCommands() {
        assertThat(CaseNoteHandler.class, isHandlerClass(COMMAND_HANDLER)
                .with(method("addCaseNote").thatHandles("sjp.command.add-case-note")));
    }

    @Test
    public void shouldHandleAddCaseNote() throws EventStreamException {
        final UUID caseId = randomUUID();
        final UUID decisionId = randomUUID();

        final Note note = note()
                .withId(randomUUID())
                .withText("note")
                .withType(DECISION)
                .withAddedAt(now())
                .build();

        final User author = user()
                .withUserId(randomUUID())
                .withFirstName("John")
                .withLastName("Smith")
                .build();

        final AddCaseNote caseNote = addCaseNote()
                .withCaseId(caseId)
                .withNote(note)
                .withAuthor(author)
                .withDecisionId(decisionId)
                .build();

        final CaseNoteAdded caseNoteAdded = caseNoteAdded()
                .withCaseId(caseId)
                .withNote(note)
                .withAuthor(author)
                .withDecisionId(decisionId)
                .build();

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.addCaseNote(caseId, caseNote.getNote(), caseNote.getAuthor(), decisionId)).thenReturn(Stream.of(caseNoteAdded));

        final Envelope<AddCaseNote> addCaseNoteCommand = envelopeFrom(metadataWithRandomUUID("sjp.command.add-case-note"), caseNote);

        caseNoteHandler.addCaseNote(addCaseNoteCommand);

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), is(streamContaining(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelopeFrom(addCaseNoteCommand.metadata(), NULL))
                                .withName("sjp.events.case-note-added"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.note", isJson(allOf(
                                        withJsonPath("id", is(note.getId().toString())),
                                        withJsonPath("text", is(note.getText())),
                                        withJsonPath("type", is(note.getType().name()))
                                ))),
                                withJsonPath("$.author", isJson(allOf(
                                        withJsonPath("userId", is(author.getUserId().toString())),
                                        withJsonPath("firstName", is(author.getFirstName())),
                                        withJsonPath("lastName", is(author.getLastName()))
                                ))),
                                withJsonPath("decisionId", is(decisionId.toString()))
                        ))))));
    }
}
