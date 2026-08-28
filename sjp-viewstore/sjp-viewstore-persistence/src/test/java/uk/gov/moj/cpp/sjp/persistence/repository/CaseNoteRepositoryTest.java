package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import uk.gov.justice.json.schemas.domains.sjp.NoteType;
import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseNote;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class CaseNoteRepositoryTest {

    private static final String PERSISTENCE_UNIT = "sjp-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider provider = new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private CaseNoteRepository caseNoteRepository;

    @BeforeEach
    void setUp() {
        caseNoteRepository = new CaseNoteRepository();
        provider.injectEntityManagerInto(caseNoteRepository);
    }

    @Test
    public void shouldFindOrderdCaseNotesStartingWithMostRecent() {
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();

        final CaseNote firstCaseNot = createCaseNote(case1Id, now().minusHours(2));
        final CaseNote secondCaseNot = createCaseNote(case1Id, now().minusHours(1));
        final CaseNote thirdCaseNot = createCaseNote(case2Id, now().minusHours(1));

        caseNoteRepository.save(firstCaseNot);
        caseNoteRepository.save(secondCaseNot);
        caseNoteRepository.save(thirdCaseNot);

        final List<CaseNote> orderedCaseNotes = caseNoteRepository.findByCaseIdOrderByAddedAtDesc(case1Id);

        assertThat(orderedCaseNotes, contains(secondCaseNot, firstCaseNot));
    }

    private static CaseNote createCaseNote(final UUID caseId, final ZonedDateTime addedAt) {
        final CaseNote caseNote = new CaseNote();
        caseNote.setCaseId(caseId);
        caseNote.setDecisionId(randomUUID());
        caseNote.setNoteId(randomUUID());
        caseNote.setNoteText(randomAlphanumeric(10));
        caseNote.setNoteType(NoteType.ADJOURNMENT);
        caseNote.setAuthorUserId(randomUUID());
        caseNote.setAuthorFirstName("John");
        caseNote.setAuthorLastName("Wall");
        caseNote.setAddedAt(addedAt);

        return caseNote;
    }
}
