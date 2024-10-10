package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import uk.gov.justice.json.schemas.domains.sjp.Note;
import uk.gov.justice.json.schemas.domains.sjp.NoteType;
import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.json.schemas.domains.sjp.events.CaseNoteAdded;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseNoteRejected;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseNoteHandlerTest {

    private CaseNoteHandler caseNoteHandler = CaseNoteHandler.INSTANCE;
    @Mock
    private CaseAggregateState caseAggregateState;

    @Test
    public void addCaseNoteWithSocCheck() {
        final Note note = Note.note().withId(UUID.randomUUID())
                        .withText("Added by code")
                                .withType(NoteType.SOC_CHECK)
                                        .withAddedAt(ZonedDateTime.now()).build();
        when(caseAggregateState.isCaseReferredForCourtHearing()).thenReturn(true);
        final UUID caseId = UUID.randomUUID();
        when(caseAggregateState.isCaseIdEqualTo(caseId)).thenReturn(true);
        final Stream<Object> objectStream = caseNoteHandler.addCaseNote(caseId, note, User.user().build(), UUID.randomUUID(), caseAggregateState);
        final List<Object> objectList = objectStream.collect(Collectors.toList());
        assertThat(objectList.size(), is(1));
        final CaseNoteAdded caseNoteAdded = (CaseNoteAdded) objectList.get(0);
        assertThat(caseNoteAdded.getNote().getId(), is(note.getId()));
    }

    @Test
    public void addCaseNoteWithNoSocCheck() {
        final Note note = Note.note().withId(UUID.randomUUID())
                        .withText("Added by code")
                                .withType(NoteType.CASE)
                                        .withAddedAt(ZonedDateTime.now()).build();
        when(caseAggregateState.isCaseReferredForCourtHearing()).thenReturn(true);
        final UUID caseId = UUID.randomUUID();
        when(caseAggregateState.isCaseIdEqualTo(caseId)).thenReturn(true);
        final Stream<Object> objectStream = caseNoteHandler.addCaseNote(caseId, note, User.user().build(), UUID.randomUUID(), caseAggregateState);
        final List<Object> objectList = objectStream.collect(Collectors.toList());
        assertThat(objectList.size(), is(1));
        final CaseNoteRejected caseNoteRejected = (CaseNoteRejected) objectList.get(0);
        assertThat(caseNoteRejected.getCaseId(), is(caseId));
    }
}
