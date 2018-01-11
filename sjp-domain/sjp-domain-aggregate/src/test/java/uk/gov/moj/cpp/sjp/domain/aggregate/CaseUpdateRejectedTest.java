package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

public class CaseUpdateRejectedTest extends CaseAggregateBaseTest {

    @Test
    public void shouldRejectCaseUpdate() {
        final List<Object> events = caseAggregate.caseUpdateRejected(UUID.randomUUID().toString(), CaseUpdateRejected.RejectReason.CASE_COMPLETED)
                .collect(Collectors.toList());

        Assert.assertEquals(1, events.size());

        final Object event = events.get(0);
        Assert.assertEquals(CaseUpdateRejected.class, event.getClass());

        final CaseUpdateRejected caseUpdateRejectedEvent = (CaseUpdateRejected) event;
        Assert.assertEquals(aCase.getId(), caseUpdateRejectedEvent.getCaseId());
        Assert.assertEquals(CaseUpdateRejected.RejectReason.CASE_COMPLETED, caseUpdateRejectedEvent.getReason());

    }

    @Test
    public void shouldThrowRuntimeExceptionWhenTheCaseIsNotCreated() {
        final CaseAggregate caseAggregate = new CaseAggregate();
        Stream<Object> eventStream = caseAggregate.caseUpdateRejected(UUID.randomUUID().toString(), CaseUpdateRejected.RejectReason.CASE_COMPLETED);
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events.size(), is(1));

    }
}

