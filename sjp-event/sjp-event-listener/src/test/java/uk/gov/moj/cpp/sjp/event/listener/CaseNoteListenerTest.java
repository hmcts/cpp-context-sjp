package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.json.schemas.domains.sjp.Note.note;
import static uk.gov.justice.json.schemas.domains.sjp.NoteAuthor.noteAuthor;
import static uk.gov.justice.json.schemas.domains.sjp.NoteType.DECISION;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.event.CaseNoteAdded;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseNote;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseNoteRepository;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class CaseNoteListenerTest {

    @Mock
    private CaseNoteRepository caseNoteRepository;

    @InjectMocks
    private CaseNoteListener caseNoteListener;

    @Captor
    private ArgumentCaptor<CaseNote> caseNoteCaptor;

    @Test
    public void shouldHandleCaseNoteEvents() {
        assertThat(CaseNoteListener.class, isHandlerClass(EVENT_LISTENER)
                .with(method("handleCaseNoteAdded").thatHandles("sjp.events.case-note-added")));
    }

    @Test
    public void shouldHandleCaseNoteAddedEvent() {
        final CaseNoteAdded caseNoteAdded = CaseNoteAdded.caseNoteAdded()
                .withCaseId(randomUUID())
                .withDecisionId(randomUUID())
                .withNote(note()
                        .withId(randomUUID())
                        .withText("note 1")
                        .withType(DECISION)
                        .withAddedAt(now())
                        .build())
                .withAuthor(noteAuthor()
                        .withUserId(randomUUID())
                        .withFirstName("John")
                        .withLastName("Smith")
                        .build())
                .build();

        final Envelope<CaseNoteAdded> caseNoteAddedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-note-added"), caseNoteAdded);

        caseNoteListener.handleCaseNoteAdded(caseNoteAddedEvent);

        verify(caseNoteRepository).save(caseNoteCaptor.capture());
        final CaseNote caseNote = caseNoteCaptor.getValue();

        assertThat(caseNote.getCaseId(), is(caseNoteAdded.getCaseId()));
        assertThat(caseNote.getDecisionId(), is(Optional.of(caseNoteAdded.getDecisionId())));
        assertThat(caseNote.getNoteId(), is(caseNoteAdded.getNote().getId()));
        assertThat(caseNote.getNoteText(), is(caseNoteAdded.getNote().getText()));
        assertThat(caseNote.getNoteType(), is(caseNoteAdded.getNote().getType()));
        assertThat(caseNote.getAddedAt(), is(caseNoteAdded.getNote().getAddedAt().toLocalDateTime()));
        assertThat(caseNote.getAuthorUserId(), is(caseNoteAdded.getAuthor().getUserId()));
        assertThat(caseNote.getAuthorFirstName(), is(caseNoteAdded.getAuthor().getFirstName()));
        assertThat(caseNote.getAuthorLastName(), is(caseNoteAdded.getAuthor().getLastName()));
    }
}
