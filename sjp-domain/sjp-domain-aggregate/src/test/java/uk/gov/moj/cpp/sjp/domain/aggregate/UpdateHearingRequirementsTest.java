package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.event.DefendantNotFound;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link CaseAggregate#updateHearingRequirements}
 */
public class UpdateHearingRequirementsTest extends CaseAggregateBaseTest {

    private static final String INITIAL_INTERPRETER_LANGUAGE = "French";
    private static final Boolean INITIAL_SPEAK_WELSH = true;

    private UUID userId;

    @BeforeEach
    public void init() {
        userId = randomUUID();
    }

    private List<Object> updatedRequirements(final String interpreterLanguage, final Boolean speakWelsh) {
        return caseAggregate.updateHearingRequirements(userId, defendantId, interpreterLanguage, speakWelsh, PleaMethod.ONLINE, clock.now())
                .collect(toList());
    }

    @Test
    public void shouldRejectCaseWhenDefendantNotFound() {
        final UUID nonExistingDefendantId = randomUUID();
        final List<Object> events = caseAggregate.updateHearingRequirements(userId, nonExistingDefendantId, INITIAL_INTERPRETER_LANGUAGE, INITIAL_SPEAK_WELSH, PleaMethod.ONLINE, clock.now())
                .collect(toList());

        assertThat(events, contains(instanceOf(DefendantNotFound.class)));

        final DefendantNotFound defendantNotFound = (DefendantNotFound) events.get(0);
        assertThat(defendantNotFound.getDefendantId(), equalTo(nonExistingDefendantId));
        assertThat(defendantNotFound.getDescription(), equalTo("Update hearing requirements"));
    }

    @Test
    public void shouldUpdateRequirements() {
        List<Object> events;
        events = updatedRequirements(INITIAL_INTERPRETER_LANGUAGE, INITIAL_SPEAK_WELSH);

        assertThat(events, contains(
                instanceOf(InterpreterUpdatedForDefendant.class),
                instanceOf(HearingLanguagePreferenceUpdatedForDefendant.class)));

        final InterpreterUpdatedForDefendant interpreterUpdatedForDefendant = (InterpreterUpdatedForDefendant) events.get(0);
        assertThat(interpreterUpdatedForDefendant.getCaseId(), equalTo(caseId));
        assertThat(interpreterUpdatedForDefendant.getDefendantId(), equalTo(defendantId));
        assertThat(interpreterUpdatedForDefendant.getInterpreter().isNeeded(), is(true));
        assertThat(interpreterUpdatedForDefendant.getInterpreter().getLanguage(), equalTo(INITIAL_INTERPRETER_LANGUAGE));

        final HearingLanguagePreferenceUpdatedForDefendant HearingLanguagePreferenceUpdatedForDefendant = (HearingLanguagePreferenceUpdatedForDefendant) events.get(1);
        assertThat(HearingLanguagePreferenceUpdatedForDefendant.getCaseId(), equalTo(caseId));
        assertThat(HearingLanguagePreferenceUpdatedForDefendant.getDefendantId(), equalTo(defendantId));
        assertThat(HearingLanguagePreferenceUpdatedForDefendant.getSpeakWelsh(), equalTo(INITIAL_SPEAK_WELSH));
    }

    @Test
    public void shouldUpdateJustInterpreterLanguage() {
        updatedRequirements(INITIAL_INTERPRETER_LANGUAGE, INITIAL_SPEAK_WELSH);
        final String INTERPRETER_LANGUAGE_UPDATED = "Spanish";

        final List<Object> events = updatedRequirements(INTERPRETER_LANGUAGE_UPDATED, INITIAL_SPEAK_WELSH);

        assertThat(events, contains(instanceOf(InterpreterUpdatedForDefendant.class)));
        final InterpreterUpdatedForDefendant interpreterUpdatedForDefendantSpanish = (InterpreterUpdatedForDefendant) events.get(0);
        assertThat(interpreterUpdatedForDefendantSpanish.getCaseId(), equalTo(caseId));
        assertThat(interpreterUpdatedForDefendantSpanish.getDefendantId(), equalTo(defendantId));
        assertThat(interpreterUpdatedForDefendantSpanish.getInterpreter().isNeeded(), is(true));
        assertThat(interpreterUpdatedForDefendantSpanish.getInterpreter().getLanguage(), equalTo(INTERPRETER_LANGUAGE_UPDATED));
    }

    @Test
    public void shouldUpdateJustSpeakWelsh() {
        updatedRequirements(INITIAL_INTERPRETER_LANGUAGE, INITIAL_SPEAK_WELSH);
        final Boolean SPEAK_WELSH_UPDATED = ! INITIAL_SPEAK_WELSH;

        final List<Object> events = updatedRequirements(INITIAL_INTERPRETER_LANGUAGE, SPEAK_WELSH_UPDATED);

        assertThat(events, contains(instanceOf(HearingLanguagePreferenceUpdatedForDefendant.class)));

        final HearingLanguagePreferenceUpdatedForDefendant HearingLanguagePreferenceUpdatedForDefendantNoSpeakWelsh = (HearingLanguagePreferenceUpdatedForDefendant) events.get(0);
        assertThat(HearingLanguagePreferenceUpdatedForDefendantNoSpeakWelsh.getCaseId(), equalTo(caseId));
        assertThat(HearingLanguagePreferenceUpdatedForDefendantNoSpeakWelsh.getDefendantId(), equalTo(defendantId));
        assertThat(HearingLanguagePreferenceUpdatedForDefendantNoSpeakWelsh.getSpeakWelsh(), equalTo(SPEAK_WELSH_UPDATED));
    }

    @Test
    public void shouldNotCreateInterpreterUpdatedForDefendantEventIfInterpreterLanguageAlreadyExist() {
        assertThat(updatedRequirements(INITIAL_INTERPRETER_LANGUAGE, INITIAL_SPEAK_WELSH), not(empty()));
        assertThat(updatedRequirements(INITIAL_INTERPRETER_LANGUAGE, INITIAL_SPEAK_WELSH), empty());
    }

    @Test
    public void shouldCreateInterpreterCancelledForDefendantEvent() {
        updatedRequirements(INITIAL_INTERPRETER_LANGUAGE, INITIAL_SPEAK_WELSH);

        final List<Object> events = updatedRequirements(null, null);

        assertThat(events, contains(
                instanceOf(InterpreterCancelledForDefendant.class),
                instanceOf(HearingLanguagePreferenceCancelledForDefendant.class)));

        final InterpreterCancelledForDefendant interpreterCancelledForDefendant = (InterpreterCancelledForDefendant) events.get(0);
        assertThat(interpreterCancelledForDefendant.getCaseId(), equalTo(caseId));
        assertThat(interpreterCancelledForDefendant.getDefendantId(), equalTo(defendantId));

        final HearingLanguagePreferenceCancelledForDefendant hearingLanguageCancelledForDefendant = (HearingLanguagePreferenceCancelledForDefendant) events.get(1);
        assertThat(hearingLanguageCancelledForDefendant.getCaseId(), equalTo(caseId));
        assertThat(hearingLanguageCancelledForDefendant.getDefendantId(), equalTo(defendantId));
    }

    @Test
    public void shouldNotCreateCancelledEventsIfInterpreterAndSpeakWelshAreNotSpecified() {
        assertThat(updatedRequirements(null, null), empty());
    }

    @Test
    public void shouldConsiderEmptyInterpreterLanguageAsCancelled() {
        updatedRequirements(INITIAL_INTERPRETER_LANGUAGE, INITIAL_SPEAK_WELSH);

        assertThat(updatedRequirements(null, INITIAL_SPEAK_WELSH), contains(instanceOf(InterpreterCancelledForDefendant.class)));
        assertThat(updatedRequirements(null, INITIAL_SPEAK_WELSH), empty());
        assertThat(updatedRequirements("", INITIAL_SPEAK_WELSH), empty());

        updatedRequirements(INITIAL_INTERPRETER_LANGUAGE, INITIAL_SPEAK_WELSH);

        assertThat(updatedRequirements("", INITIAL_SPEAK_WELSH), contains(instanceOf(InterpreterCancelledForDefendant.class)));
        assertThat(updatedRequirements("", INITIAL_SPEAK_WELSH), empty());
        assertThat(updatedRequirements(null, INITIAL_SPEAK_WELSH), empty());
    }

}
