package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder.aDefaultSjpCase;
import static uk.gov.moj.cpp.sjp.domain.testutils.OffenceBuilder.createDefaultOffences;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.CASE_ID;

import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.Offence;
import uk.gov.moj.cpp.sjp.domain.Priority;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.plea.SetPleas;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.domain.testutils.DefendantBuilder;
import uk.gov.moj.cpp.sjp.domain.util.DefaultTestData;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidRequired;
import uk.gov.moj.cpp.sjp.event.TrialRequestCancelled;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Test;

public class CaseReadinessTest {

    @Test
    public void shouldChangeStatusAfterPleadingGuiltyOnNotGuiltyOffences() {
        final UUID firstOffenceId = randomUUID();
        final UUID secondOffenceId = randomUUID();

        final List<Offence> offences = createDefaultOffences(firstOffenceId, secondOffenceId);
        final CaseBuilder caseBuilder = aDefaultSjpCase().withDefendant(
                new DefendantBuilder().withOffences(offences).build()
        );

        final CaseAggregate aCase = new CaseAggregate();

        aCase.receiveCase(caseBuilder.build(), ZonedDateTime.now());
        aCase.setPleas(CASE_ID, prepareSetPleas(offences, PleaType.GUILTY), randomUUID(), ZonedDateTime.now());
        aCase.setPleas(CASE_ID, prepareSetPleas(offences, PleaType.NOT_GUILTY), randomUUID(), ZonedDateTime.now());
        aCase.addDatesToAvoid("not today", "ALL");

        final Stream<Object> eventsAfterGuiltYPlea = aCase.setPleas(CASE_ID, prepareSetPleas(offences, PleaType.GUILTY), randomUUID(), ZonedDateTime.now());
        final List<Object> listOfEventsAfterGuiltyPlea = eventsAfterGuiltYPlea
                .collect(toList());

        assertThat(listOfEventsAfterGuiltyPlea, matchCaseReadyForDecision(new CaseMarkedReadyForDecision(
                CASE_ID, CaseReadinessReason.PLEADED_GUILTY, ZonedDateTime.now(), SessionType.MAGISTRATE, Priority.MEDIUM
        )));
        assertThat(listOfEventsAfterGuiltyPlea, matchTrialRequestedCancelled());
    }

    @Test
    public void shouldntRaiseSecondDatesToAvoidRequiredWhenDefendantsPleasNotGuiltyTwice() {
        final UUID firstOffenceId = randomUUID();

        final List<Offence> offences = createDefaultOffences(firstOffenceId);
        final CaseBuilder caseBuilder = aDefaultSjpCase()
                .withDefendant(
                        new DefendantBuilder().withOffences(offences).build()
                );

        final CaseAggregate aCase = new CaseAggregate();
        aCase.receiveCase(caseBuilder.build(), ZonedDateTime.now());

        aCase.setPleas(CASE_ID, prepareSetPleas(offences, PleaType.GUILTY), randomUUID(), ZonedDateTime.now());
        final Stream<Object> eventsAfterPleadingNotGuilty = aCase.setPleas(CASE_ID, prepareSetPleas(offences, PleaType.NOT_GUILTY), randomUUID(), ZonedDateTime.now());
        final List<Object> eventsAfterPleadingGuiltyFirstTime = eventsAfterPleadingNotGuilty.collect(toList());
        assertThat(eventsAfterPleadingGuiltyFirstTime, matchDatesToAvoidRequired());

        aCase.expireDatesToAvoidTimer();

        aCase.setPleas(CASE_ID, prepareSetPleas(offences, PleaType.GUILTY), randomUUID(), ZonedDateTime.now());
        final Stream<Object> eventsAfterNotGuilty = aCase.setPleas(CASE_ID, prepareSetPleas(offences, PleaType.NOT_GUILTY), randomUUID(), ZonedDateTime.now());

        final List<Object> listOfEventsAfterPleadingNotGuiltySecondTime = eventsAfterNotGuilty.collect(toList());

        assertThat(listOfEventsAfterPleadingNotGuiltySecondTime, matchCaseReadyForDecision(new CaseMarkedReadyForDecision(
                CASE_ID, CaseReadinessReason.PLEADED_NOT_GUILTY, ZonedDateTime.now(), SessionType.DELEGATED_POWERS, Priority.MEDIUM
        )));
        assertThat(listOfEventsAfterPleadingNotGuiltySecondTime, not(matchDatesToAvoidRequired()));
    }

    private Matcher<Iterable<? super DatesToAvoidRequired>> matchDatesToAvoidRequired() {
        return hasItem(new TypeSafeDiagnosingMatcher<DatesToAvoidRequired>() {
            @Override
            public void describeTo(final Description description) {
                description.appendText("dates to avoid required");
            }

            @Override
            protected boolean matchesSafely(final DatesToAvoidRequired item, final Description mismatchDescription) {
                return true;
            }
        });
    }

    private Matcher<Iterable<? super TrialRequestCancelled>> matchTrialRequestedCancelled() {
        return hasItem(new TypeSafeDiagnosingMatcher<TrialRequestCancelled>() {
            @Override
            public void describeTo(final Description description) {
                description.appendText("trial request cancel");
            }

            @Override
            protected boolean matchesSafely(final TrialRequestCancelled item, final Description mismatchDescription) {
                return true;
            }
        });
    }

    private Matcher<Iterable<? super CaseMarkedReadyForDecision>> matchCaseReadyForDecision(final CaseMarkedReadyForDecision expectedCaseMarkedReadyForDecision) {
        return hasItem(new TypeSafeDiagnosingMatcher<CaseMarkedReadyForDecision>() {
            @Override
            public void describeTo(final Description description) {
                description.appendText("pleaded guilty and session type is magistrate");
            }

            @Override
            protected boolean matchesSafely(final CaseMarkedReadyForDecision item, final Description mismatchDescription) {
                if (item.getReason() == expectedCaseMarkedReadyForDecision.getReason() && item.getSessionType() == expectedCaseMarkedReadyForDecision.getSessionType()) {
                    return true;
                }

                mismatchDescription.appendText("no event found");
                return false;
            }
        });
    }

    private SetPleas prepareSetPleas(final List<Offence> offences, final PleaType pleaType) {
        return new SetPleas(null, preparePleas(offences, pleaType));
    }

    private List<Plea> preparePleas(final List<Offence> offences, final PleaType pleaType) {
        return offences.stream()
                .map(offence -> new Plea(DefaultTestData.DEFENDANT_ID, offence.getId(), pleaType))
                .collect(toList());
    }
}
