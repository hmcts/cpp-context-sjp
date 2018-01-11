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

import uk.gov.moj.cpp.sjp.domain.command.CancelPlea;
import uk.gov.moj.cpp.sjp.domain.command.UpdatePlea;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.testutils.PleaBuilder;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.cpp.sjp.event.InterpreterCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.OffenceNotFound;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class UpdatePleaTest extends CaseAggregateBaseTest {

    private static UUID caseId;
    private UUID offenceId;

    @Before
    public void setup() {
        setUp();
        caseId = aCase.getId();
        offenceId = aCase.getDefendant().getOffences().get(0).getId();
    }

    @Test
    public void shouldUpdatePlea() {
        //when
        final UpdatePlea updatePlea = PleaBuilder.defaultUpdatePlea(offenceId);
        Stream<Object> eventStream = caseAggregate.updatePlea(updatePlea);

        //then
        List<Object> events = asList(eventStream.toArray());
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
    }

    private void shouldUpdatePlea(final String plea, final String interpreterLanguage, final Class expectedInterpreterEvent) {
        //when
        final UpdatePlea updatePlea = new UpdatePlea(caseId, offenceId, plea, true, interpreterLanguage);
        final Stream<Object> eventStream = caseAggregate.updatePlea(updatePlea);

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events.size(), is(expectedInterpreterEvent == null ? 1 : 2));
        final PleaUpdated pleaUpdated = (PleaUpdated)events.get(0);
        assertThat(pleaUpdated.getCaseId(), equalTo(caseId.toString()));
        assertThat(pleaUpdated.getOffenceId(), equalTo(offenceId.toString()));
        assertThat(pleaUpdated.getPlea(), equalTo(plea));
        if (expectedInterpreterEvent == InterpreterUpdatedForDefendant.class) {
            final InterpreterUpdatedForDefendant interpreterUpdated = (InterpreterUpdatedForDefendant)events.get(1);
            assertThat(interpreterUpdated.getCaseId(), is(caseId));
            assertThat(interpreterUpdated.getDefendantId(), isA(UUID.class));
            assertThat(interpreterUpdated.getInterpreter().getLanguage(), equalTo(interpreterLanguage));
        }
        else if (expectedInterpreterEvent == InterpreterCancelledForDefendant.class) {
            final InterpreterCancelledForDefendant interpreterCancelled = (InterpreterCancelledForDefendant)events.get(1);
            assertThat(interpreterCancelled.getCaseId(), is(caseId));
            assertThat(interpreterCancelled.getDefendantId(), isA(UUID.class));
        }
    }

    private void shouldCancelPlea(final boolean cancelInterpreter) {
        //when
        final CancelPlea cancelPlea = new CancelPlea(caseId, offenceId);
        final Stream<Object> eventStream = caseAggregate.cancelPlea(cancelPlea);

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
        shouldUpdatePlea(Plea.Type.GUILTY_REQUEST_HEARING.name(), null, null);

        // add interpreter
        shouldUpdatePlea(Plea.Type.GUILTY_REQUEST_HEARING.name(), interpreterLanguage1, InterpreterUpdatedForDefendant.class);

        // update just plea
        shouldUpdatePlea(Plea.Type.NOT_GUILTY.name(), interpreterLanguage1, null);

        // update just interpreter
        shouldUpdatePlea(Plea.Type.NOT_GUILTY.name(), interpreterLanguage2, InterpreterUpdatedForDefendant.class);

        // cancel the interpreter
        shouldUpdatePlea(Plea.Type.NOT_GUILTY.name(), null, InterpreterCancelledForDefendant.class);

        // update plea and interpreter
        shouldUpdatePlea(Plea.Type.GUILTY_REQUEST_HEARING.name(), interpreterLanguage1, InterpreterUpdatedForDefendant.class);

        // cancel plea (and interpreter)
        shouldCancelPlea(true);

        // add plea
        shouldUpdatePlea(Plea.Type.GUILTY_REQUEST_HEARING.name(), null, null);

        // cancel plea
        shouldCancelPlea(false);
    }

    @Test
    public void shouldNotUpdatePleaWhenWithdrawalOffencesRequested() {
        //given
        caseAggregate.requestWithdrawalAllOffences(caseId.toString());

        //when
        final UpdatePlea updatePlea = PleaBuilder.defaultUpdatePlea(offenceId);
        Stream<Object> eventStream = caseAggregate.updatePlea(updatePlea);

        //then
        List<Object> events = asList(eventStream.toArray());
        assertThat("Has CaseUpdateRejected event", events, hasItem(isA(CaseUpdateRejected.class)));
        assertThat(((CaseUpdateRejected) events.get(0)).getReason(),
                        is(CaseUpdateRejected.RejectReason.WITHDRAWAL_PENDING));
    }

    @Test
    public void shouldUpdatePleaWhenWithdrawalOffencesRequestCancelled() {
        //given
        caseAggregate.requestWithdrawalAllOffences(caseId.toString());
        caseAggregate.cancelRequestWithdrawalAllOffences(caseId.toString());

        //when
        final UpdatePlea updatePlea = PleaBuilder.defaultUpdatePlea(offenceId);
        Stream<Object> eventStream = caseAggregate.updatePlea(updatePlea);

        //then
        List<Object> events = asList(eventStream.toArray());
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
    }

    @Test
    public void shouldUpdatePleaWhenSjpnIsNotAdded() {
        //when
        final UpdatePlea updatePlea = PleaBuilder.defaultUpdatePlea(offenceId);
        Stream<Object> eventStream = caseAggregate.updatePlea(updatePlea);

        //then
        List<Object> events = asList(eventStream.toArray());
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
    }

    @Test
    public void shouldNotUpdatePleaWhenOffenceDoesNotExist() {
        final UpdatePlea updatePlea = PleaBuilder.defaultUpdatePlea(UUID.randomUUID());
        List<Object> events = caseAggregate.updatePlea(updatePlea).collect(toList());

        assertThat(events, hasSize(1));
        assertThat(events.get(0) , instanceOf(OffenceNotFound.class));
    }

}
