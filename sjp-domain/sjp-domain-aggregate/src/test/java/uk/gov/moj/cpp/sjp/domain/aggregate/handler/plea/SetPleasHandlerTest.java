package uk.gov.moj.cpp.sjp.domain.aggregate.handler.plea;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.json.schemas.domains.sjp.User.user;
import static uk.gov.moj.cpp.sjp.domain.DomainConstants.NUMBER_DAYS_WAITING_FOR_DATES_TO_AVOID;
import static uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds.disabilityNeedsOf;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaMethod.POSTAL;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtInterpreter;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.plea.SetPleas;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidRequired;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.PleadedGuilty;
import uk.gov.moj.cpp.sjp.event.PleadedGuiltyCourtHearingRequested;
import uk.gov.moj.cpp.sjp.event.PleadedNotGuilty;
import uk.gov.moj.cpp.sjp.event.PleasSet;
import uk.gov.moj.cpp.sjp.event.TrialRequested;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

public class SetPleasHandlerTest {

    private final LocalDate expectedDateReady = now().plusDays(NUMBER_DAYS_WAITING_FOR_DATES_TO_AVOID);

    private final SetPleasHandler setPleasHandler = SetPleasHandler.INSTANCE;

    private final UUID caseId = randomUUID();

    private final UUID defendantId = randomUUID();

    private final User user = user().withUserId(randomUUID()).withFirstName("Theresa").withLastName("May").build();

    private final Clock clock = new StoppedClock(new UtcClock().now());

    private CaseAggregateState caseAggregateState;

    @Before
    public void onceBeforeEachTest() {
        caseAggregateState = new CaseAggregateState();
        caseAggregateState.setCaseId(caseId);
    }

    @Test
    public void testSetMultipleNotGuiltyPleasWithoutInterpreter() {
        final ZonedDateTime now = clock.now();
        final SetPleas setPleas = createSetPleas(null, false,
                newArrayList(NOT_GUILTY, NOT_GUILTY, NOT_GUILTY),
                null);

        caseAggregateState.addOffenceIdsForDefendant(defendantId, setPleas.getPleas().stream()
                .map(Plea::getOffenceId)
                .collect(toSet()));

        final List<Object> eventList = this.setPleasHandler
                .setPleas(caseId, setPleas, caseAggregateState, user.getUserId(), now)
                .collect(toList());

        assertThat(eventList, containsInAnyOrder(
                new PleasSet(caseId, setPleas.getDefendantCourtOptions(), setPleas.getPleas()),
                HearingLanguagePreferenceUpdatedForDefendant.createEvent(caseId, defendantId, false),
                new TrialRequested(caseId, now),
                new DatesToAvoidRequired(caseId, expectedDateReady),
                new PleadedNotGuilty(caseId, defendantId, setPleas.getPleas().get(0).getOffenceId(), now, POSTAL),
                new PleadedNotGuilty(caseId, defendantId, setPleas.getPleas().get(1).getOffenceId(), now, POSTAL),
                new PleadedNotGuilty(caseId, defendantId, setPleas.getPleas().get(2).getOffenceId(), now, POSTAL)));
    }

    @Test
    public void testSetTheSamePleasAgain() {
        final ZonedDateTime now = clock.now();
        final String disabilityNeeds = "needs hearing aid";
        final SetPleas setPleas = createSetPleas(null, false,
                newArrayList(NOT_GUILTY, GUILTY, GUILTY_REQUEST_HEARING), disabilityNeeds);

        caseAggregateState.updateDefendantSpeakWelsh(defendantId, false);
        caseAggregateState.addOffenceIdsForDefendant(defendantId, setPleas.getPleas().stream()
                .map(Plea::getOffenceId)
                .collect(toSet()));
        caseAggregateState.setPleas(setPleas.getPleas());
        caseAggregateState.setTrialRequested(true);
        caseAggregateState.setDatesToAvoidExpirationDate(now());
        caseAggregateState.setDatesToAvoidPreviouslyRequested();

        final List<Object> eventList = this.setPleasHandler.setPleas(caseId, setPleas, caseAggregateState, user.getUserId(), now).collect(toList());

        assertThat(eventList, containsInAnyOrder(
                new PleasSet(caseId, setPleas.getDefendantCourtOptions(), setPleas.getPleas())));
    }

    @Test
    public void testSetPleasWithInterpreter() {
        final ZonedDateTime now = clock.now();
        final SetPleas setPleas = createSetPleas("ES", false,
                newArrayList(NOT_GUILTY, GUILTY, GUILTY_REQUEST_HEARING), null);

        caseAggregateState.addOffenceIdsForDefendant(defendantId, setPleas.getPleas().stream()
                .map(Plea::getOffenceId)
                .collect(toSet()));

        final List<Object> eventList = this.setPleasHandler.setPleas(caseId, setPleas, caseAggregateState, user.getUserId(), now).collect(toList());

        assertThat(eventList, containsInAnyOrder(
                new PleasSet(caseId, setPleas.getDefendantCourtOptions(), setPleas.getPleas()),
                InterpreterUpdatedForDefendant.createEvent(caseId, defendantId, "ES"),
                HearingLanguagePreferenceUpdatedForDefendant.createEvent(caseId, defendantId, false),
                new TrialRequested(caseId, now),
                new DatesToAvoidRequired(caseId, expectedDateReady),
                new PleadedNotGuilty(caseId, defendantId, setPleas.getPleas().get(0).getOffenceId(), now, POSTAL),
                new PleadedGuilty(caseId, defendantId, setPleas.getPleas().get(1).getOffenceId(), POSTAL, now),
                new PleadedGuiltyCourtHearingRequested(caseId, defendantId, setPleas.getPleas().get(2).getOffenceId(), POSTAL, now)));
    }

    @Test
    public void testSetPleasWithoutCourtOptionsAndDatesToAvoidPreviouslyAdded() {
        final ZonedDateTime now = clock.now();
        final SetPleas setPleas = createSetPleas(null, null,
                newArrayList(NOT_GUILTY, GUILTY, GUILTY_REQUEST_HEARING), null);

        caseAggregateState.addOffenceIdsForDefendant(defendantId, setPleas.getPleas().stream()
                .map(Plea::getOffenceId)
                .collect(toSet()));

        caseAggregateState.updateDefendantInterpreterLanguage(defendantId, Interpreter.of("ES"));
        caseAggregateState.updateDefendantSpeakWelsh(defendantId, true);
        caseAggregateState.setDatesToAvoidExpirationDate(now().plusDays(1));
        caseAggregateState.setDatesToAvoidPreviouslyRequested();

        final List<Object> eventList = this.setPleasHandler.setPleas(caseId, setPleas, caseAggregateState, user.getUserId(), now).collect(toList());

        assertThat(eventList, containsInAnyOrder(
                new PleasSet(caseId, setPleas.getDefendantCourtOptions(), setPleas.getPleas()),
                new InterpreterCancelledForDefendant(caseId, defendantId),
                new HearingLanguagePreferenceCancelledForDefendant(caseId, defendantId),
                new TrialRequested(caseId, now),
                new PleadedNotGuilty(caseId, defendantId, setPleas.getPleas().get(0).getOffenceId(), now, POSTAL),
                new PleadedGuilty(caseId, defendantId, setPleas.getPleas().get(1).getOffenceId(), POSTAL, now),
                new PleadedGuiltyCourtHearingRequested(caseId, defendantId, setPleas.getPleas().get(2).getOffenceId(), POSTAL, now)));
    }

    private SetPleas createSetPleas(final String interpreterLanguage, final Boolean welshHearing, final List<PleaType> pleaTypes, final String disabilityNeeds) {

        DefendantCourtOptions defendantCourtOptions = null;

        if (interpreterLanguage != null || welshHearing != null) {
            defendantCourtOptions = new DefendantCourtOptions(
                    Optional.ofNullable(interpreterLanguage).map(lang -> new DefendantCourtInterpreter(interpreterLanguage, true)).orElse(null),
                    welshHearing,
                    disabilityNeedsOf(disabilityNeeds));
        }

        final List<Plea> pleas = pleaTypes.stream()
                .map(this::createPlea)
                .collect(Collectors.toList());

        return new SetPleas(defendantCourtOptions, pleas);
    }

    private Plea createPlea(final PleaType pleaType) {
        return new Plea(defendantId, randomUUID(), pleaType);
    }
}
