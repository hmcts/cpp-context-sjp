package uk.gov.moj.cpp.sjp.domain.aggregate;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected.RejectReason;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Test;

public class CaseUpdateRejectedTest extends CaseAggregateBaseTest {

    @Test
    public void shouldRejectCaseUpdate() {
        final List<Object> events = caseAggregate.caseUpdateRejected(UUID.randomUUID(), RejectReason.CASE_COMPLETED)
                .collect(Collectors.toList());

        assertThat(events, hasSize(1));

        final Object event = events.get(0);
        assertThat(event, instanceOf(CaseUpdateRejected.class));

        final CaseUpdateRejected caseUpdateRejectedEvent = (CaseUpdateRejected) event;
        assertEquals(aCase.getId(), caseUpdateRejectedEvent.getCaseId());
        assertEquals(RejectReason.CASE_COMPLETED, caseUpdateRejectedEvent.getReason());
    }

}

