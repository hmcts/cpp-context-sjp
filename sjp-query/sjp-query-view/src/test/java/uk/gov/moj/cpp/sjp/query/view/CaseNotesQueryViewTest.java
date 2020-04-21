package uk.gov.moj.cpp.sjp.query.view;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.json.schemas.domains.sjp.NoteType;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseNote;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseNoteRepository;
import uk.gov.moj.cpp.sjp.query.view.service.UserAndGroupsService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseNotesQueryViewTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private CaseNoteRepository caseNotesRepository;

    @Mock
    private UserAndGroupsService userAndGroupsService;

    @InjectMocks
    private CaseNotesQueryView caseNotesQueryView;

    final UUID caseId = randomUUID();

    final JsonEnvelope queryEnvelope = envelope()
            .with(metadataWithRandomUUID("sjp.query.case-notes"))
            .withPayloadOf(caseId.toString(), "caseId")
            .build();

    @Test
    public void shouldHandleNotesQuery() {
        assertThat(CaseNotesQueryView.class, isHandlerClass(Component.QUERY_VIEW)
                .with(method("getCaseNotes").thatHandles("sjp.query.case-notes")));
    }

    @Test
    public void shouldReturnAllCaseNotes() {
        final UUID decisionId = randomUUID();

        final CaseNote caseNote1 = createCaseNote(caseId, decisionId);
        final CaseNote caseNote2 = createCaseNote(caseId, null);
        final List<CaseNote> allCaseNotes = asList(caseNote1, caseNote2);

        when(caseNotesRepository.findByCaseIdOrderByAddedAtDesc(caseId)).thenReturn(allCaseNotes);
        when(userAndGroupsService.isUserProsecutor(queryEnvelope)).thenReturn(false);

        final JsonEnvelope caseNotes = caseNotesQueryView.getCaseNotes(queryEnvelope);

        assertThat(caseNotes, jsonEnvelope(metadata().withName("sjp.query.case-notes"),
                payload().isJson(allOf(
                        withJsonPath("$.caseId", is(caseId.toString())),
                        withJsonPath("$.notes.length()", is(allCaseNotes.size())),
                        withJsonPath("$.notes[0]", getCaseNoteMatcher(caseNote1)),
                        withJsonPath("$.notes[1]", getCaseNoteMatcher(caseNote2))
                ))
        ));
    }

    @Test
    public void shouldReturnCaseManagementNotesForProsecutors() {
        final UUID decisionId = randomUUID();

        final CaseNote caseNote1 = createCaseNote(caseId, decisionId);
        final CaseNote caseNote2 = createCaseNote(caseId, null);
        final List<CaseNote> returnedCaseNotes = asList(caseNote1, caseNote2);

        when(caseNotesRepository.findByCaseIdAndNoteTypeOrderByAddedAtDesc(caseId, NoteType.CASE_MANAGEMENT))
                .thenReturn(returnedCaseNotes);
        when(userAndGroupsService.isUserProsecutor(queryEnvelope)).thenReturn(true);

        final JsonEnvelope caseNotes = caseNotesQueryView.getCaseNotes(queryEnvelope);

        verify(caseNotesRepository).findByCaseIdAndNoteTypeOrderByAddedAtDesc(caseId, NoteType.CASE_MANAGEMENT);
        assertThat(caseNotes, jsonEnvelope(metadata().withName("sjp.query.case-notes"),
                payload().isJson(allOf(
                        withJsonPath("$.caseId", is(caseId.toString())),
                        withJsonPath("$.notes.length()", is(returnedCaseNotes.size())),
                        withJsonPath("$.notes[0]", getCaseNoteMatcher(caseNote1)),
                        withJsonPath("$.notes[1]", getCaseNoteMatcher(caseNote2))
                ))
        ));
    }

    @Test
    public void shouldReturnEmptyListOfNotesIfNotesAreNotPresent() {
        final List<CaseNote> allCaseNotes = emptyList();

        when(caseNotesRepository.findByCaseIdOrderByAddedAtDesc(caseId)).thenReturn(allCaseNotes);

        final JsonEnvelope caseNotes = caseNotesQueryView.getCaseNotes(queryEnvelope);

        assertThat(caseNotes, jsonEnvelope(metadata().withName("sjp.query.case-notes"),
                payload().isJson(allOf(
                        withJsonPath("$.caseId", is(caseId.toString())),
                        withJsonPath("$.notes.length()", is(0))
                ))
        ));
    }

    private Matcher<? super ReadContext> getCaseNoteMatcher(final CaseNote caseNote) {
        final Optional<UUID> decisionId = caseNote.getDecisionId();
        final Matcher<? super ReadContext> decisionIdMatcher = decisionId.isPresent() ?
                withJsonPath("decisionId", is(decisionId.get().toString())) : withoutJsonPath("decisionId");

        return isJson(allOf(
                withJsonPath("noteId", is(caseNote.getNoteId().toString())),
                withJsonPath("noteText", is(caseNote.getNoteText())),
                withJsonPath("noteType", is(caseNote.getNoteType().name())),
                withJsonPath("authorFirstName", is(caseNote.getAuthorFirstName())),
                withJsonPath("authorLastName", is(caseNote.getAuthorLastName())),
                withJsonPath("addedAt", is(caseNote.getAddedAt().toString())),
                decisionIdMatcher
        ));
    }

    private CaseNote createCaseNote(final UUID caseId, final UUID decisionId) {
        final CaseNote caseNote = new CaseNote();
        caseNote.setCaseId(caseId);
        caseNote.setNoteId(randomUUID());
        caseNote.setNoteText("note");
        caseNote.setNoteType(NoteType.CASE);
        caseNote.setAuthorFirstName("John");
        caseNote.setAuthorLastName("Wall");
        caseNote.setAuthorUserId(randomUUID());
        caseNote.setAddedAt(now().minusDays(1));
        caseNote.setDecisionId(decisionId);
        return caseNote;
    }
}
