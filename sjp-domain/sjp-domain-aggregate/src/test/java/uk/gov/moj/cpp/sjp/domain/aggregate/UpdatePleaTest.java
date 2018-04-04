package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import uk.gov.moj.cpp.sjp.domain.PleaType;
import uk.gov.moj.cpp.sjp.domain.command.CancelPlea;
import uk.gov.moj.cpp.sjp.domain.command.UpdatePlea;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadOnline;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.testutils.PleaBuilder;
import uk.gov.moj.cpp.sjp.domain.testutils.StoreOnlinePleaBuilder;
import uk.gov.moj.cpp.sjp.event.InterpreterCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.OffenceNotFound;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.event.TrialRequestCancelled;
import uk.gov.moj.cpp.sjp.event.TrialRequested;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class UpdatePleaTest extends CaseAggregateBaseTest {

    private static UUID caseId;
    private UUID defendantId;
    private UUID offenceId;
    private final ZonedDateTime now = clock.now();

    @Before
    public void setUp() {
        super.setUp();
        caseId = aCase.getId();
        defendantId = caseReceivedEvent.getDefendant().getId();
        offenceId = aCase.getDefendant().getOffences().get(0).getId();
    }

    @Test
    public void shouldUpdatePlea() {
        //when
        final UpdatePlea updatePlea = PleaBuilder.defaultUpdatePlea(offenceId);
        Stream<Object> eventStream = caseAggregate.updatePlea(updatePlea, now);

        //then
        List<Object> events = asList(eventStream.toArray());
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
    }

    private void shouldUpdatePlea(final String plea, final String interpreterLanguage, final Class expectedInterpreterEvent,
                                  final boolean trialRequestedEventExpected, final boolean trialRequestCancelledEventExpected) {
        //when
        final UpdatePlea updatePlea = new UpdatePlea(caseId, offenceId, plea, true, interpreterLanguage);
        final Stream<Object> eventStream = caseAggregate.updatePlea(updatePlea, now);

        //then
        final List<Object> events = asList(eventStream.toArray());
        if (trialRequestedEventExpected || trialRequestCancelledEventExpected) {
            assertThat(events.size(), is(expectedInterpreterEvent == null ? 2 : 3));
        }
        else {
            assertThat(events.size(), is(expectedInterpreterEvent == null ? 1 : 2));
        }
        PleaUpdated pleaUpdated = (PleaUpdated)events.get(0);
        assertThat(pleaUpdated.getCaseId(), equalTo(caseId.toString()));
        assertThat(pleaUpdated.getOffenceId(), equalTo(offenceId.toString()));
        assertThat(pleaUpdated.getPlea(), equalTo(plea));
        if (expectedInterpreterEvent == InterpreterUpdatedForDefendant.class) {
            final InterpreterUpdatedForDefendant interpreterUpdated = findInterpreterUpdatedForDefendantEvent(events).get();
            assertThat(interpreterUpdated.getCaseId(), is(caseId));
            assertThat(interpreterUpdated.getDefendantId(), isA(UUID.class));
            assertThat(interpreterUpdated.getInterpreter().getLanguage(), equalTo(interpreterLanguage));
        }
        else if (expectedInterpreterEvent == InterpreterCancelledForDefendant.class) {
            final InterpreterCancelledForDefendant interpreterCancelled = findInterpreterCancelledForDefendantEvent(events).get();
            assertThat(interpreterCancelled.getCaseId(), is(caseId));
            assertThat(interpreterCancelled.getDefendantId(), isA(UUID.class));
        }
    }

    private Optional<InterpreterUpdatedForDefendant> findInterpreterUpdatedForDefendantEvent(final List<Object> events) {
        return events.stream()
                .filter(event -> event instanceof InterpreterUpdatedForDefendant)
                .map(event -> (InterpreterUpdatedForDefendant) event)
                .findFirst();
    }

    private Optional<InterpreterCancelledForDefendant> findInterpreterCancelledForDefendantEvent(final List<Object> events) {
        return events.stream()
                .filter(event -> event instanceof InterpreterCancelledForDefendant)
                .map(event -> (InterpreterCancelledForDefendant) event)
                .findFirst();
    }

    private void shouldCancelPlea(final boolean cancelInterpreter) {
        //when
        final CancelPlea cancelPlea = new CancelPlea(caseId, offenceId);
        final Stream<Object> eventStream = caseAggregate.cancelPlea(cancelPlea, now);

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events.size(), is(cancelInterpreter ? 2 : 1));
        final PleaCancelled pleaCancelled = (PleaCancelled) events.get(0);
        assertThat(pleaCancelled.getCaseId(), equalTo(caseId.toString()));
        assertThat(pleaCancelled.getOffenceId(), equalTo(offenceId.toString()));
        if (cancelInterpreter) {
            final InterpreterCancelledForDefendant interpreterCancelled = (InterpreterCancelledForDefendant)events.get(1);
            assertThat(interpreterCancelled.getCaseId(), is(caseId));
            assertThat(interpreterCancelled.getDefendantId(), isA(UUID.class));
        }
    }

    @Test
    public void shouldAddUpdateAndCancelPleaAndInterpreter() {
        final String interpreterLanguage1 = "Maori";
        final String interpreterLanguage2 = "Welsh";

        // add plea
        shouldUpdatePlea(Plea.Type.GUILTY_REQUEST_HEARING.name(), null, null, false, false);

        // add interpreter
        shouldUpdatePlea(Plea.Type.GUILTY_REQUEST_HEARING.name(), interpreterLanguage1, InterpreterUpdatedForDefendant.class, false, false);

        // update just plea
        shouldUpdatePlea(Plea.Type.NOT_GUILTY.name(), interpreterLanguage1, null, true, false);

        // update just interpreter
        shouldUpdatePlea(Plea.Type.NOT_GUILTY.name(), interpreterLanguage2, InterpreterUpdatedForDefendant.class, false, false);

        // cancel the interpreter
        shouldUpdatePlea(Plea.Type.NOT_GUILTY.name(), null, InterpreterCancelledForDefendant.class, false, false);

        // update plea and interpreter
        shouldUpdatePlea(Plea.Type.GUILTY_REQUEST_HEARING.name(), interpreterLanguage1, InterpreterUpdatedForDefendant.class, false, true);

        // cancel plea (and interpreter)
        shouldCancelPlea(true);

        // add plea
        shouldUpdatePlea(Plea.Type.GUILTY_REQUEST_HEARING.name(), null, null, false, false);

        // cancel plea
        shouldCancelPlea(false);
    }

    @Test
    public void shouldUpdatePleaWhenWithdrawalOffencesRequested() {
        //given
        caseAggregate.requestWithdrawalAllOffences(caseId.toString());

        //when
        final UpdatePlea updatePlea = PleaBuilder.defaultUpdatePlea(offenceId);
        Stream<Object> eventStream = caseAggregate.updatePlea(updatePlea, now);

        //then
        List<Object> events = asList(eventStream.toArray());
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
    }

    @Test
    public void shouldUpdatePleaWhenWithdrawalOffencesRequestCancelled() {
        //given
        caseAggregate.requestWithdrawalAllOffences(caseId.toString());
        caseAggregate.cancelRequestWithdrawalAllOffences(caseId.toString());

        //when
        final UpdatePlea updatePlea = PleaBuilder.defaultUpdatePlea(offenceId);
        Stream<Object> eventStream = caseAggregate.updatePlea(updatePlea, now);

        //then
        List<Object> events = asList(eventStream.toArray());
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
    }

    @Test
    public void shouldUpdatePleaWhenSjpnIsNotAdded() {
        //when
        final UpdatePlea updatePlea = PleaBuilder.defaultUpdatePlea(offenceId);
        Stream<Object> eventStream = caseAggregate.updatePlea(updatePlea, now);

        //then
        List<Object> events = asList(eventStream.toArray());
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
    }

    @Test
    public void shouldNotUpdatePleaWhenOffenceDoesNotExist() {
        final UpdatePlea updatePlea = PleaBuilder.defaultUpdatePlea(UUID.randomUUID());
        List<Object> events = caseAggregate.updatePlea(updatePlea, now).collect(toList());

        assertThat(events, hasSize(1));
        assertThat(events.get(0) , instanceOf(OffenceNotFound.class));
    }

    @Test
    public void shouldUpdatePleaToGuiltyWhenPleadedNotGuiltyPreviouslyOnline() {
        //given
        pleadNotGuiltyOnline(PleaType.NOT_GUILTY);

        //when
        final Stream<Object> eventStream = pleadByPost(PleaType.GUILTY);

        //then
        assertHasBothPleaUpdatedAndTrialCancelledEvents(eventStream);
    }

    @Test
    public void shouldUpdatePleaToGuiltyWhenPleadedGuiltyPreviouslyOnline() {
        //given
        pleadNotGuiltyOnline(PleaType.GUILTY);

        //when
        final Stream<Object> eventStream = pleadByPost(PleaType.GUILTY);

        //then
        assertHasPleaUpdatedEventOnly(eventStream);
    }

    @Test
    public void shouldUpdatePleaToGuiltyWhenPleadedGuiltyRequestHearingPreviouslyOnline() {
        //given
        pleadNotGuiltyOnline(PleaType.GUILTY_REQUEST_HEARING);

        //when
        final Stream<Object> eventStream = pleadByPost(PleaType.GUILTY);

        //then
        assertHasPleaUpdatedEventOnly(eventStream);
    }

    @Test
    public void shouldUpdatePleaToGuiltyWhenPleadedNotGuiltyPreviouslyByPost() {
        //given
        pleadByPost(PleaType.NOT_GUILTY);

        //when
        final Stream<Object> eventStream = pleadByPost(PleaType.GUILTY);

        //then
        assertHasBothPleaUpdatedAndTrialCancelledEvents(eventStream);
    }

    @Test
    public void shouldUpdatePleaToNotGuiltyWhenPleadedNotGuiltyPreviouslyOnline() {
        //given
        pleadNotGuiltyOnline(PleaType.NOT_GUILTY);

        //when
        Stream<Object> eventStream = pleadByPost(PleaType.NOT_GUILTY);

        //then
        assertHasPleaUpdatedEventOnly(eventStream);
    }

    @Test
    public void shouldUpdatePleaToNotGuiltyWhenPleadedGuiltyPreviouslyOnline() {
        //given
        pleadNotGuiltyOnline(PleaType.GUILTY);

        //when
        Stream<Object> eventStream = pleadByPost(PleaType.NOT_GUILTY);

        //then
        assertHasBothPleaUpdatedAndTrialRequestedEvents(eventStream);
    }

    @Test
    public void shouldUpdatePleaToNotGuiltyWhenPleadedGuiltyRequestHearingPreviouslyOnline() {
        //given
        pleadNotGuiltyOnline(PleaType.GUILTY_REQUEST_HEARING);

        //when
        Stream<Object> eventStream = pleadByPost(PleaType.NOT_GUILTY);

        //then
        assertHasBothPleaUpdatedAndTrialRequestedEvents(eventStream);
    }

    @Test
    public void shouldUpdatePleaToNotGuiltyWhenPleadedNotGuiltyPreviouslyByPost() {
        //given
        pleadByPost(PleaType.NOT_GUILTY);

        //when
        Stream<Object> eventStream = pleadByPost(PleaType.NOT_GUILTY);

        //then
        assertHasPleaUpdatedEventOnly(eventStream);
    }

    @Test
    public void shouldUpdatePleaToGuiltyRequestHearingWhenPleadedNotGuiltyPreviouslyOnline() {
        //given
        pleadNotGuiltyOnline(PleaType.NOT_GUILTY);

        //when
        Stream<Object> eventStream = pleadByPost(PleaType.GUILTY_REQUEST_HEARING);

        //then
        assertHasBothPleaUpdatedAndTrialCancelledEvents(eventStream);
    }

    @Test
    public void shouldUpdatePleaToGuiltyRequestHearingWhenPleadedGuiltyPreviouslyOnline() {
        //given
        pleadNotGuiltyOnline(PleaType.GUILTY);

        //when
        Stream<Object> eventStream = pleadByPost(PleaType.GUILTY_REQUEST_HEARING);

        //then
        assertHasPleaUpdatedEventOnly(eventStream);
    }

    @Test
    public void shouldUpdatePleaToGuiltyRequestHearingWhenPleadedGuiltyRequestHearingPreviouslyOnline() {
        //given
        pleadNotGuiltyOnline(PleaType.GUILTY_REQUEST_HEARING);

        //when
        Stream<Object> eventStream = pleadByPost(PleaType.GUILTY_REQUEST_HEARING);

        //then
        assertHasPleaUpdatedEventOnly(eventStream);
    }

    @Test
    public void shouldUpdatePleaToGuiltyRequestHearingWhenPleadedNotGuiltyPreviouslyByPost() {
        //given
        pleadByPost(PleaType.NOT_GUILTY);

        //when
        Stream<Object> eventStream = pleadByPost(PleaType.GUILTY_REQUEST_HEARING);

        //then
        assertHasBothPleaUpdatedAndTrialCancelledEvents(eventStream);
    }

    @Test
    public void shouldCancelPleaWhenPleadedNotGuiltyPreviouslyOnline() {
        //given
        pleadNotGuiltyOnline(PleaType.NOT_GUILTY);

        //when
        final CancelPlea cancelPlea = new CancelPlea(caseId, offenceId);
        Stream<Object> eventStream = caseAggregate.cancelPlea(cancelPlea, now);

        //then
        assertHasBothPleaCancelledAndTrialCancelledEvents(eventStream);
    }

    @Test
    public void shouldCancelPleaWhenPleadedGuiltyPreviouslyOnline() {
        //given
        pleadNotGuiltyOnline(PleaType.GUILTY);

        //when
        final CancelPlea cancelPlea = new CancelPlea(caseId, offenceId);
        Stream<Object> eventStream = caseAggregate.cancelPlea(cancelPlea, now);

        //then
        assertHasPleaCancelledEventOnly(eventStream);
    }

    @Test
    public void shouldCancelPleaWhenPleadedGuiltyRequestHearingPreviouslyOnline() {
        //given
        pleadNotGuiltyOnline(PleaType.GUILTY_REQUEST_HEARING);

        //when
        final CancelPlea cancelPlea = new CancelPlea(caseId, offenceId);
        Stream<Object> eventStream = caseAggregate.cancelPlea(cancelPlea, now);

        //then
        assertHasPleaCancelledEventOnly(eventStream);
    }

    @Test
    public void shouldCancelPleaWhenPleadedNotGuiltyPreviouslyByPost() {
        //given
        pleadByPost(PleaType.NOT_GUILTY);

        //when
        final CancelPlea cancelPlea = new CancelPlea(caseId, offenceId);
        Stream<Object> eventStream = caseAggregate.cancelPlea(cancelPlea, now);

        //then
        assertHasBothPleaCancelledAndTrialCancelledEvents(eventStream);
    }

    @Test
    public void shouldToggleTrialEventsWhenUpdateAndCancelPleasRepeatedlyWhenInitialPleaWasOnline() {
        //given pleas online NOT_GUILTY
        pleadNotGuiltyOnline(PleaType.NOT_GUILTY);

        //when toggle plea
        toggleTrialEventsByUpdatingAndCancellingPleasRepeatedly();
    }

    @Test
    public void shouldNotToggleTrialEventsWhenUpdateAndCancelPleasRepeatedlyWhenInitialPleaWasByPost() {
        //given plead by post NOT_GUILTY
        pleadByPost(PleaType.NOT_GUILTY);

        //when toggle plea
        toggleTrialEventsByUpdatingAndCancellingPleasRepeatedly();
    }

    private PleadOnline pleadNotGuiltyOnline(PleaType pleaType) {
        PleadOnline pleadOnline = null;
        if (pleaType.equals(PleaType.NOT_GUILTY)) {
            pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithNotGuiltyPlea(offenceId,
                    defendantId.toString(), null, true);
        }
        else if (pleaType.equals(PleaType.GUILTY_REQUEST_HEARING)) {
            pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyRequestHearingPlea(offenceId,
                    defendantId.toString(), null);
        }
        else if (pleaType.equals(PleaType.GUILTY)) {
            pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId.toString());
        }
        caseAggregate.pleadOnline(caseId, pleadOnline, now);
        return pleadOnline;
    }

    private Stream<Object> pleadByPost(PleaType pleaType) {
        UpdatePlea updatePlea = null;
        if (pleaType.equals(PleaType.GUILTY)) {
            updatePlea = PleaBuilder.updatePleaGuilty(offenceId);
        }
        else if (pleaType.equals(PleaType.GUILTY_REQUEST_HEARING)) {
            updatePlea = PleaBuilder.updatePleaGuiltyRequestHearing(offenceId);
        }
        else if (pleaType.equals(PleaType.NOT_GUILTY)) {
            updatePlea = PleaBuilder.updatePleaNotGuilty(offenceId);
        }
        return caseAggregate.updatePlea(updatePlea, now);
    }

    private void assertHasPleaUpdatedEventOnly(Stream<Object> eventStream) {
        List<Object> events = asList(eventStream.toArray());
        assertThat(events, hasSize(1));
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
    }

    private void assertHasPleaCancelledEventOnly(Stream<Object> eventStream) {
        List<Object> events = asList(eventStream.toArray());
        assertThat(events, hasSize(1));
        assertThat("Has PleaCancelled event", events, hasItem(isA(PleaCancelled.class)));
    }

    private void assertHasBothPleaUpdatedAndTrialCancelledEvents(Stream<Object> eventStream) {
        List<Object> events = asList(eventStream.toArray());
        assertThat(events, hasSize(2));
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
        assertThat("Has TrialRequestCancelled event", events, hasItem(isA(TrialRequestCancelled.class)));

        TrialRequestCancelled trialRequestCancelled = (TrialRequestCancelled) events.get(1);
        assertThat(trialRequestCancelled.getCaseId(), equalTo(caseId));
    }

    private void assertHasBothPleaCancelledAndTrialCancelledEvents(Stream<Object> eventStream) {
        List<Object> events = asList(eventStream.toArray());
        assertThat(events, hasSize(2));
        assertThat("Has PleaCancelled event", events, hasItem(isA(PleaCancelled.class)));
        assertThat("Has TrialRequestCancelled event", events, hasItem(isA(TrialRequestCancelled.class)));

        TrialRequestCancelled trialRequestCancelled = (TrialRequestCancelled) events.get(1);
        assertThat(trialRequestCancelled.getCaseId(), equalTo(caseId));
    }

    private void assertHasBothPleaUpdatedAndTrialRequestedEvents(Stream<Object> eventStream) {
        List<Object> events = asList(eventStream.toArray());
        assertThat(events, hasSize(2));
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
        assertThat("Has TrialRequested event", events, hasItem(isA(TrialRequested.class)));

        TrialRequested trialRequested = (TrialRequested) events.get(1);
        assertThat(trialRequested.getUnavailability(), equalTo(trialRequested.getUnavailability()));
        assertThat(trialRequested.getWitnessDetails(), equalTo(trialRequested.getWitnessDetails()));
        assertThat(trialRequested.getWitnessDispute(), equalTo(trialRequested.getWitnessDispute()));
    }

    private void toggleTrialEventsByUpdatingAndCancellingPleasRepeatedly() {
        //then update to GUILTY
        Stream<Object> eventStream = pleadByPost(PleaType.GUILTY);
        assertHasBothPleaUpdatedAndTrialCancelledEvents(eventStream);

        //then update to NOT_GUILTY
        eventStream = pleadByPost(PleaType.NOT_GUILTY);
        assertHasBothPleaUpdatedAndTrialRequestedEvents(eventStream);

        //then update to GUILTY
        eventStream = pleadByPost(PleaType.GUILTY_REQUEST_HEARING);
        assertHasBothPleaUpdatedAndTrialCancelledEvents(eventStream);

        //then update to NOT_GUILTY
        eventStream = pleadByPost(PleaType.NOT_GUILTY);
        assertHasBothPleaUpdatedAndTrialRequestedEvents(eventStream);

        //then cancels plea
        final CancelPlea cancelPlea = new CancelPlea(caseId, offenceId);
        eventStream = caseAggregate.cancelPlea(cancelPlea, now);
        assertHasBothPleaCancelledAndTrialCancelledEvents(eventStream);

        //then update to NOT_GUILTY
        eventStream = pleadByPost(PleaType.NOT_GUILTY);
        assertHasBothPleaUpdatedAndTrialRequestedEvents(eventStream);
    }
}
