package uk.gov.moj.cpp.sjp.domain.aggregate.state;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;

import uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus;
import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.domain.decision.Adjourn;
import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.SetAside;
import uk.gov.moj.cpp.sjp.domain.testutils.builders.DismissBuilder;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.Sets;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

public class CaseAggregateStateStateTest {

    private static final UUID DEFENDANT_ID = randomUUID();

    private CaseAggregateState state;

    @Before
    public void setUp() {
        state = new CaseAggregateState();
    }

    @Test
    public void shouldMarkCaseAsCompleted() {
        state.markCaseCompleted();

        assertThat(state.isCaseCompleted(), is(true));
    }

    @Test
    public void shouldMarkCaseReferredToCourtForHearing() {
        state.markCaseReferredForCourtHearing();

        assertThat(state.isCaseReferredForCourtHearing(), is(true));
    }

    @Test
    public void shouldCorreclyCheckIfDefendantHasOffences() {
        state.getOffenceIdsByDefendantId().put(DEFENDANT_ID, emptySet());

        assertThat(state.hasDefendant(DEFENDANT_ID), is(true));
        assertThat(state.hasDefendant(randomUUID()), is(false));
    }

    @Test
    public void shouldReturnInterpreterLanguageForDefendant() {
        state.getDefendantsInterpreterLanguages().put(DEFENDANT_ID, "foo");

        assertThat(state.getDefendantInterpreterLanguage(DEFENDANT_ID), is("foo"));
        assertThat(state.getDefendantInterpreterLanguage(randomUUID()), nullValue());
    }

    @Test
    public void shouldProperlyCheckIfDefendantSpeakWelsh() {
        state.getDefendantsSpeakWelsh().put(DEFENDANT_ID, true);

        assertThat(state.getDefendantsSpeakWelsh().get(DEFENDANT_ID), is(true));
        assertThat(state.getDefendantsSpeakWelsh().get(randomUUID()), nullValue());
    }

    @Test
    public void shouldAddOffenceWithPlea() {
        UUID offenceId = randomUUID();
        state.updateOffenceWithPlea(offenceId);

        assertThat(state.getOffenceIdsWithPleas(), contains(offenceId));
    }

    @Test
    public void shouldRemoveOffenceIdWithPleas() {
        UUID offenceId = randomUUID();

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
        UUID offenceUid = randomUUID();
        state.getOffenceIdsByDefendantId().put(DEFENDANT_ID, Sets.newHashSet(offenceUid));

        assertThat(state.getDefendantForOffence(offenceUid).get(), is(DEFENDANT_ID));
    }


    @Test
    public void shouldGetEmptyOptionalIfNoDefendantForOffenceId() {
        assertThat(state.getDefendantForOffence(randomUUID()), is(Optional.empty()));
    }

    @Test
    public void shouldReturnFalseIfAssigneeNull() {
        assertFalse(state.isAssignee(randomUUID()));
    }

    @Test
    public void shouldReturnFalseIfAssigneeDifferent() {
        state.setAssigneeId(randomUUID());

        assertFalse(state.isAssignee(randomUUID()));
    }

    @Test
    public void shouldReturnTrueIfAssigneeCorrect() {
        UUID assigneeId = randomUUID();
        state.setAssigneeId(assigneeId);

        assertTrue(state.isAssignee(assigneeId));
    }

    @Test
    public void shouldFindOffence() {
        UUID offenceId = randomUUID();

        state.getOffenceIdsByDefendantId().put(DEFENDANT_ID, Sets.newHashSet(offenceId));

        assertTrue(state.offenceExists(offenceId));
    }

    @Test
    public void shouldCheckIfCaseIdEqual() {
        UUID caseId = randomUUID();

        state.setCaseId(caseId);

        assertTrue(state.isCaseIdEqualTo(caseId));
    }

    @Test
    public void shouldResolveToTrueWhenWithdrawalIsRequestedOnAllCases() {
        UUID offenceId1 = randomUUID();
        UUID offenceId2 = randomUUID();

        final UUID withdrawalRequestReasonId1 = randomUUID();
        final UUID withdrawalRequestReasonId2 = randomUUID();

        state.getOffenceIdsByDefendantId().put(DEFENDANT_ID, Sets.newHashSet(offenceId1, offenceId2));

        state.addWithdrawnOffences(new WithdrawalRequestsStatus(offenceId1, withdrawalRequestReasonId1));
        state.addWithdrawnOffences(new WithdrawalRequestsStatus(offenceId2, withdrawalRequestReasonId2));

        assertTrue(state.withdrawalRequestedOnAllOffences());
    }


    @Test
    public void shouldResolveToFalseWhenWithdrawalIsNotRequestedOnAllCases() {
        // given
        UUID offenceId1 = randomUUID();
        UUID offenceId2 = randomUUID();

        final UUID withdrawalRequestReasonId1 = randomUUID();
        final UUID withdrawalRequestReasonId2 = randomUUID();

        state.getOffenceIdsByDefendantId().put(DEFENDANT_ID, Sets.newHashSet(offenceId1, offenceId2));
        state.addWithdrawnOffences(new WithdrawalRequestsStatus(offenceId1, withdrawalRequestReasonId1));
        state.addWithdrawnOffences(new WithdrawalRequestsStatus(offenceId2, withdrawalRequestReasonId2));

        // when
        state.cancelWithdrawnOffence(offenceId1);

        // then
        assertFalse(state.withdrawalRequestedOnAllOffences());
    }

    @Test
    public void shouldClearOffenceConvictionDatesWhenTheDecisionIsSetAside() {
        // given
        ZonedDateTime zonedDateTime = ZonedDateTime.now();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();

        final LocalDate adjournedTo = LocalDate.now().plusDays(10);
        List<OffenceDecision> offenceDecisions = newArrayList(
                new Adjourn(randomUUID(), asList(
                        createOffenceDecisionInformation(offenceId1, FOUND_GUILTY),
                        createOffenceDecisionInformation(offenceId2, FOUND_GUILTY)),
                        "adjourn reason ", adjournedTo));

        state.updateOffenceConvictionDates(zonedDateTime, offenceDecisions);

        assertTrue(state.offenceHasPreviousConviction(offenceId1));
        zonedDateTime = ZonedDateTime.now();
        offenceDecisions = newArrayList(
                new SetAside(randomUUID(), asList(
                        createOffenceDecisionInformation(offenceId1, null),
                        createOffenceDecisionInformation(offenceId2, null))));
        // when
        state.updateOffenceConvictionDates(zonedDateTime, offenceDecisions);

        // then
        assertFalse(state.offenceHasPreviousConviction(offenceId1));
    }

    @Test
    public void shouldMarkCasesAsHavingPreviousPressRestrictions() {
        final Dismiss dismiss1 = DismissBuilder.withDefaults().pressRestriction("Ryan").build();
        final Dismiss dismiss2 = DismissBuilder.withDefaults().pressRestrictionRevoked().build();
        final Dismiss dismiss3 = DismissBuilder.withDefaults().build();
        state.markOffenceAsPressRestrictable(dismiss1.getId());
        state.markOffenceAsPressRestrictable(dismiss2.getId());
        assumeThat(state.hasPreviousPressRestriction(dismiss1.getId()), is(false));
        assumeThat(state.hasPreviousPressRestriction(dismiss2.getId()), is(false));
        assumeThat(state.hasPreviousPressRestriction(dismiss3.getId()), is(false));

        state.updateOffenceDecisions(asList(dismiss1, dismiss2, dismiss3), randomUUID());

        assertThat(state.hasPreviousPressRestriction(dismiss1.getId()), is(true));
        assertThat(state.hasPreviousPressRestriction(dismiss2.getId()), is(true));
        assertThat(state.hasPreviousPressRestriction(dismiss3.getId()), is(false));
    }

    @Test
    public void shouldMarkCasesAsNotHavingPreviousPressRestrictions() {
        final DismissBuilder dismiss = DismissBuilder.withDefaults();
        state.markOffenceAsPressRestrictable(dismiss.getId());

        state.updateOffenceDecisions(singletonList(dismiss.build()), randomUUID());

        assertThat(state.hasPreviousPressRestriction(dismiss.getId()), is(false));
    }
}
