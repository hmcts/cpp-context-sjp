package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.SjpOffence;
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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class UpdatePleaTest {

    private CaseAggregate caseAggregate = new CaseAggregate();
    private static final UUID caseId = UUID.randomUUID();
    private static final String urn = "TFL123456";
    private static final String INITIATION_CODE = "J";

    private static final UUID offenceId = UUID.randomUUID();

    private Clock clock = new StoppedClock(ZonedDateTime.now());

    @Test
    public void shouldUpdatePlea() {
        //given
        caseAggregate.createCase(createTestCase(), clock.now());

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

        final String maori = "Maori";
        final String welsh = "Welsh";
        caseAggregate.createCase(createTestCase(), clock.now());

        // add plea
        shouldUpdatePlea(Plea.Type.GUILTY_REQUEST_HEARING.name(), null, null);

        // add interpreter
        shouldUpdatePlea(Plea.Type.GUILTY_REQUEST_HEARING.name(), maori, InterpreterUpdatedForDefendant.class);

        // update just plea
        shouldUpdatePlea(Plea.Type.NOT_GUILTY.name(), maori, null);

        // update just interpreter
        shouldUpdatePlea(Plea.Type.NOT_GUILTY.name(), welsh, InterpreterUpdatedForDefendant.class);

        // cancel the interpreter
        shouldUpdatePlea(Plea.Type.NOT_GUILTY.name(), null, InterpreterCancelledForDefendant.class);

        // update plea and interpreter
        shouldUpdatePlea(Plea.Type.GUILTY_REQUEST_HEARING.name(), maori, InterpreterUpdatedForDefendant.class);

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
        caseAggregate.createCase(createTestCase(), clock.now());
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
        caseAggregate.createCase(createTestCase(), clock.now());
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
        //given
        caseAggregate.createCase(createTestCase(), clock.now());

        //when
        final UpdatePlea updatePlea = PleaBuilder.defaultUpdatePlea(offenceId);
        Stream<Object> eventStream = caseAggregate.updatePlea(updatePlea);

        //then
        List<Object> events = asList(eventStream.toArray());
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
    }

    @Test
    public void shouldNotUpdatePleaWhenOffenceDoesNotExist() {
        final UpdatePlea updatePlea = PleaBuilder.defaultUpdatePlea(offenceId);
        List<Object> events = caseAggregate.updatePlea(updatePlea).collect(toList());


        assertThat(events.size(), is(1));

        Object object = events.get(0);
        assertThat(object.getClass() , is(CoreMatchers.equalTo(OffenceNotFound.class)));
    }

    private Case createTestCase() {
        SjpOffence offence = new SjpOffence();
        offence.setId(offenceId);

        return new Case(
                caseId,
                urn,
                null, null, INITIATION_CODE, null, null, null, null,
                null, null, 0, null, null,
                new ArrayList<SjpOffence>() {{ add(offence); }}
        );
    }

}
