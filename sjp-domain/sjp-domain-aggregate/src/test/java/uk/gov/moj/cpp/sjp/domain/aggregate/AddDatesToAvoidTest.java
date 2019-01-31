package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.event.CaseNotFound;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidAdded;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidUpdated;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class AddDatesToAvoidTest extends CaseAggregateBaseTest {

    private static final String DATES_TO_AVOID = "12th July 2018";
    private static final String DATES_TO_AVOID_UPDATED = "13th August 2018";

    @Test
    public void datesToAvoidAddedEvent() {
        //when
        final List<Object> events = caseAggregate.addDatesToAvoid(DATES_TO_AVOID).collect(toList());
        final DatesToAvoidAdded datesToAvoidAdded = (DatesToAvoidAdded) events.get(0);

        //then
        assertThatDatesToAvoidAddedEventWasRaised(events, datesToAvoidAdded);
    }

    @Test
    public void raiseDatesToAvoidUpdatedEvent() {
        //given
        datesToAvoidAddedEvent(DATES_TO_AVOID);

        //when
        final List<Object> datesToAvoidUpdatedEvents = caseAggregate.addDatesToAvoid(DATES_TO_AVOID_UPDATED).collect(toList());
        final DatesToAvoidUpdated datesToAvoidUpdated = (DatesToAvoidUpdated) datesToAvoidUpdatedEvents.get(0);

        //then
        assertThatDatesToAvoidUpdatedEventWasRaised(datesToAvoidUpdatedEvents, datesToAvoidUpdated);
    }

    private void assertThatDatesToAvoidAddedEventWasRaised(List<Object> datesToAvoidAddedEvents, DatesToAvoidAdded datesToAvoidAdded) {
        assertThat(datesToAvoidAddedEvents, hasSize(1));
        assertThat(datesToAvoidAdded.getCaseId(), equalTo(caseId));
        assertThat(datesToAvoidAdded.getDatesToAvoid(), equalTo(DATES_TO_AVOID));
    }

    private void assertThatDatesToAvoidUpdatedEventWasRaised(List<Object> datesToAvoidUpdatedEvents, DatesToAvoidUpdated datesToAvoidUpdated) {
        assertThat(datesToAvoidUpdatedEvents, hasSize(1));
        assertThat(datesToAvoidUpdated.getCaseId(), equalTo(caseId));
        assertThat(datesToAvoidUpdated.getDatesToAvoid(), equalTo(DATES_TO_AVOID_UPDATED));
    }

    private DatesToAvoidAdded datesToAvoidAddedEvent(final String datesToAvoid) {
        final List<Object> datesToAvoidAddedEvents = caseAggregate.addDatesToAvoid(datesToAvoid).collect(toList());
        return (DatesToAvoidAdded) datesToAvoidAddedEvents.get(0);
    }

    @Test
    public void caseCompletedDoesNotAcceptDatesToAvoid() {
        //given a completed case
        caseAggregate.completeCase();

        //when
        final List<Object> events = caseAggregate.addDatesToAvoid(DATES_TO_AVOID).collect(toList());

        //then
        assertThat(events, hasSize(1));
        final CaseUpdateRejected datesToAvoidReceived = (CaseUpdateRejected) events.get(0);
        assertThat(datesToAvoidReceived.getCaseId(), equalTo(aCase.getId()));
        assertThat(datesToAvoidReceived.getReason(), equalTo(CaseUpdateRejected.RejectReason.CASE_COMPLETED));
    }

    @Test
    public void caseAssignedDoesNotAcceptDatesToAvoid() {
        //given an assigned case
        caseAggregate.assignCase(UUID.randomUUID(), clock.now(), CaseAssignmentType.DELEGATED_POWERS_DECISION);

        //when
        final List<Object> events = caseAggregate.addDatesToAvoid(DATES_TO_AVOID).collect(toList());

        //then
        assertThat(events, hasSize(1));
        final CaseUpdateRejected datesToAvoidReceived = (CaseUpdateRejected) events.get(0);
        assertThat(datesToAvoidReceived.getCaseId(), equalTo(aCase.getId()));
        assertThat(datesToAvoidReceived.getReason(), equalTo(CaseUpdateRejected.RejectReason.CASE_ASSIGNED));
    }

    @Test
    public void caseNotFound() {

        //when
        final List<Object> events = new CaseAggregate().addDatesToAvoid(DATES_TO_AVOID).collect(toList());

        //then
        assertThat(events, hasSize(1));
        final CaseNotFound datesToAvoidReceived = (CaseNotFound) events.get(0);
        assertThat(datesToAvoidReceived.getCaseId(), nullValue());
        assertThat(datesToAvoidReceived.getDescription(), equalTo("Add dates to avoid"));
    }

}
