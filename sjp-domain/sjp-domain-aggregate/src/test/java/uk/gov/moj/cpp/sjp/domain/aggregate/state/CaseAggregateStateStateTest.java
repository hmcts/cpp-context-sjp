package uk.gov.moj.cpp.sjp.domain.aggregate.state;

import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import uk.gov.moj.cpp.sjp.domain.Interpreter;

import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

public class CaseAggregateStateStateTest {

    private static final UUID DEFENDANT_ID = UUID.randomUUID();

    private CaseAggregateState state;

    @Before
    public void setUp() {
        state = new CaseAggregateState();
    }

    @Test
    public void shouldCorreclyCheckIfDefendantHasOffences() {
        state.getOffenceIdsByDefendantId().put(DEFENDANT_ID, emptySet());

        assertThat(state.hasDefendant(DEFENDANT_ID), is(true));
        assertThat(state.hasDefendant(UUID.randomUUID()), is(false));
    }

    @Test
    public void shouldReturnInterpreterLanguageForDefendant() {
        state.getDefendantsInterpreterLanguages().put(DEFENDANT_ID, "foo");

        assertThat(state.getDefendantInterpreterLanguage(DEFENDANT_ID), is("foo"));
        assertThat(state.getDefendantInterpreterLanguage(UUID.randomUUID()), nullValue());
    }

    @Test
    public void shouldProperlyCheckIfDefendantSpeakWelsh() {
        state.getDefendantsSpeakWelsh().put(DEFENDANT_ID, true);

        assertThat(state.getDefendantsSpeakWelsh().get(DEFENDANT_ID), is(true));
        assertThat(state.getDefendantsSpeakWelsh().get(UUID.randomUUID()), nullValue());
    }

    @Test
    public void shouldAddOffenceWithPlea() {
        UUID offenceId = UUID.randomUUID();
        state.addOffenceIdWithPleas(offenceId);

        assertThat(state.getOffenceIdsWithPleas(), contains(offenceId));
    }

    @Test
    public void shouldRemoveOffenceIdWithPleas() {
        UUID offenceId = UUID.randomUUID();

        state.getOffenceIdsWithPleas().add(offenceId);
        state.removePleaFromOffence(offenceId);

        assertThat(state.getOffenceIdsWithPleas(), Matchers.iterableWithSize(0));
    }

    @Test
    public void shouldUpdateDefendantInterpreterLanguage() {
        String interpreterLanguage = "welsh";
        final Interpreter welshInterpreter = Interpreter.of(interpreterLanguage);

        state.updateDefendantInterpreterLanguage(DEFENDANT_ID, welshInterpreter);
        assertThat(state.getDefendantsInterpreterLanguages().get(DEFENDANT_ID), is(interpreterLanguage));
    }

    @Test
    public void shouldUpdateDefendantSpeakWelshPreference() {
        state.updateDefendantSpeakWelsh(DEFENDANT_ID, true);
        assertThat(state.getDefendantsSpeakWelsh().get(DEFENDANT_ID), is(true));
    }

    @Test
    public void shouldRemoveDefendantSpeakWelshPreference() {
        state.getDefendantsSpeakWelsh().put(DEFENDANT_ID, true);

        state.removeDefendantSpeakWelshPreference(DEFENDANT_ID);
        assertThat(state.getDefendantsSpeakWelsh().get(DEFENDANT_ID), nullValue());
    }

    @Test
    public void shouldRemoveInterpreterForDefendant() {
        state.getDefendantsInterpreterLanguages().put(DEFENDANT_ID, "welsh");

        state.removeInterpreterForDefendant(DEFENDANT_ID);
        assertThat(state.getDefendantsInterpreterLanguages().get(DEFENDANT_ID), isEmptyOrNullString());
    }

    @Test
    public void shouldUpdateEmploymentStatusForDefendant() {
        String employmentStatus = "unemployed";
        state.updateEmploymentStatusForDefendant(DEFENDANT_ID, employmentStatus);

        assertThat(state.getEmploymentStatusByDefendantId().get(DEFENDANT_ID), is(employmentStatus));
    }

    @Test
    public void shouldRemoveEmploymentStatusForDefendant() {
        String employmentStatus = "unemployed";
        state.getEmploymentStatusByDefendantId().put(DEFENDANT_ID, employmentStatus);

        state.removeEmploymentStatusForDefendant(DEFENDANT_ID);

        assertThat(state.getEmploymentStatusByDefendantId().get(DEFENDANT_ID), isEmptyOrNullString());
    }

    @Test
    public void shouldGetEmptyOptionalWhenDefendantEmploymentStatusNotPresent() {
        assertThat(state.getDefendantEmploymentStatus(DEFENDANT_ID), is(Optional.empty()));
    }


    @Test
    public void shouldGetDefendantEmploymentStatusWhenPresent() {
        String employmentStatus = "employmentStatus";

        state.getEmploymentStatusByDefendantId().put(DEFENDANT_ID, employmentStatus);

        assertThat(state.getDefendantEmploymentStatus(DEFENDANT_ID).get(), is(employmentStatus));
    }

    @Test
    public void shouldGetDefendantForOffenceId() {
        UUID offenceUid = UUID.randomUUID();
        state.getOffenceIdsByDefendantId().put(DEFENDANT_ID, Sets.newHashSet(offenceUid));

        assertThat(state.getDefendantForOffence(offenceUid).get(), is(DEFENDANT_ID));
    }


    @Test
    public void shouldGetEmptyOptionalIfNoDefendantForOffenceId() {
        assertThat(state.getDefendantForOffence(UUID.randomUUID()), is(Optional.empty()));
    }

    @Test
    public void shouldReturnFalseIfAssigneeNull() {
        assertFalse(state.isAssignee(UUID.randomUUID()));
    }

    @Test
    public void shouldReturnFalseIfAssigneeDifferent() {
        state.setAssigneeId(UUID.randomUUID());

        assertFalse(state.isAssignee(UUID.randomUUID()));
    }

    @Test
    public void shouldReturnTrueIfAssigneeCorrect() {
        UUID assigneeId = UUID.randomUUID();
        state.setAssigneeId(assigneeId);

        assertTrue(state.isAssignee(assigneeId));
    }

    @Test
    public void shouldFindOffence() {
        UUID offenceId = UUID.randomUUID();

        state.getOffenceIdsByDefendantId().put(DEFENDANT_ID, Sets.newHashSet(offenceId));

        assertTrue(state.offenceExists(offenceId));
    }

    @Test
    public void shouldCheckIfCaseIdEqual() {
        UUID caseId = UUID.randomUUID();

        state.setCaseId(caseId);

        assertTrue(state.isCaseIdEqualTo(caseId));
    }
}
