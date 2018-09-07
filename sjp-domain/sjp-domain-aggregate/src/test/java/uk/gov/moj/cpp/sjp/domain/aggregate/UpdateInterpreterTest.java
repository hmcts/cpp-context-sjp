package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;

import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.event.DefendantNotFound;
import uk.gov.moj.cpp.sjp.event.InterpreterCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;

import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

/**
 * Test for {@link CaseAggregate#updateInterpreter}
 */
public class UpdateInterpreterTest extends CaseAggregateBaseTest {

    private static final String INITIAL_LANGUAGE = "French";

    private UUID userId;

    @Before
    public void init() {
        userId = randomUUID();
    }

    private List<Object> updateInterpreterLanguage(final String interpreterLanguage) {
        return caseAggregate.updateInterpreter(userId, defendantId, interpreterLanguage)
                .collect(toList());
    }

    @Test
    public void shouldRejectCaseWhenDefendantNotFound() {
        final UUID nonExistingDefendantId = randomUUID();
        final List<Object> events = caseAggregate.updateInterpreter(userId, nonExistingDefendantId, INITIAL_LANGUAGE)
                .collect(toList());

        assertThat(events, contains(instanceOf(DefendantNotFound.class)));

        final DefendantNotFound defendantNotFound = (DefendantNotFound) events.get(0);
        assertThat(defendantNotFound.getDefendantId(), equalTo(nonExistingDefendantId));
        assertThat(defendantNotFound.getDescription(), equalTo("Update interpreter"));
    }

    @Test
    public void shouldUpdateInterpreter() {
        final List<Object> events = updateInterpreterLanguage(INITIAL_LANGUAGE);

        assertThat(events, contains(
                instanceOf(InterpreterUpdatedForDefendant.class)));

        final InterpreterUpdatedForDefendant interpreterUpdatedForDefendant = (InterpreterUpdatedForDefendant) events.get(0);
        assertThat(interpreterUpdatedForDefendant.getCaseId(), equalTo(caseId));
        assertThat(interpreterUpdatedForDefendant.getDefendantId(), equalTo(defendantId));
        assertThat(interpreterUpdatedForDefendant.getInterpreter(), equalTo(Interpreter.of(INITIAL_LANGUAGE)));
    }

    @Test
    public void shouldUpdateJustInterpreterLanguage() {
        final String INTERPRETER_LANGUAGE_UPDATED = "Spanish";

        final List<Object> events = updateInterpreterLanguage(INTERPRETER_LANGUAGE_UPDATED);

        assertThat(events, contains(instanceOf(InterpreterUpdatedForDefendant.class)));
        final InterpreterUpdatedForDefendant interpreterUpdatedForDefendantSpanish = (InterpreterUpdatedForDefendant) events.get(0);
        assertThat(interpreterUpdatedForDefendantSpanish.getCaseId(), equalTo(caseId));
        assertThat(interpreterUpdatedForDefendantSpanish.getDefendantId(), equalTo(defendantId));
        assertThat(interpreterUpdatedForDefendantSpanish.getInterpreter(), equalTo(Interpreter.of(INTERPRETER_LANGUAGE_UPDATED)));
    }

    @Test
    public void shouldNotCreateInterpreterUpdatedForDefendantEventIfInterpreterLanguageAlreadyExist() {
        assertThat(updateInterpreterLanguage(INITIAL_LANGUAGE), not(empty()));
        assertThat(updateInterpreterLanguage(INITIAL_LANGUAGE), empty());
    }

    @Test
    public void shouldCreateInterpreterCancelledForDefendantEvent() {
        updateInterpreterLanguage(INITIAL_LANGUAGE);

        final List<Object> events = updateInterpreterLanguage(null);

        assertThat(events, contains(
                instanceOf(InterpreterCancelledForDefendant.class)));

        final InterpreterCancelledForDefendant interpreterCancelledForDefendant = (InterpreterCancelledForDefendant) events.get(0);
        assertThat(interpreterCancelledForDefendant.getCaseId(), equalTo(caseId));
        assertThat(interpreterCancelledForDefendant.getDefendantId(), equalTo(defendantId));
    }

    @Test
    public void shouldNotCreateCancelledEventsIfInterpreterIsNotSpecified() {
        assertThat(updateInterpreterLanguage(null), empty());
    }

}
