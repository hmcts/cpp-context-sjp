package uk.gov.moj.cpp.sjp.domain.aggregate;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregateBaseTest.AggregateTester.when;

import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.command.UpdatePlea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseUnmarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;

import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link CaseAggregate#unmarkCaseReadyForDecision}
 */
public class CaseReadyForDecisionUnmark extends CaseAggregateBaseTest {

    private final PleaType pleaType = PleaType.GUILTY;

    @Before
    public void markCaseBeforeStartAnyTest() {
        final CaseReadinessReason readinessReason = CaseReadinessReason.PLEADED_GUILTY;

        Stream<Object> events;
        events = caseAggregate.updatePlea(caseId, new UpdatePlea(caseId, offenceId, pleaType), clock.now());
        assertThat(collectSingleEvent(events, PleaUpdated.class).getPlea(), is(pleaType));

        events = caseAggregate.markCaseReadyForDecision(readinessReason, clock.now());
        assertThat(collectSingleEvent(events, CaseMarkedReadyForDecision.class).getReason(), is(readinessReason));
    }

    @Test
    public void shouldNotUnmarkCaseWhenNeverMarked() {
        setUp(); // reinitialise Aggregate as @Before is marking the default case as ready

        when(caseAggregate.unmarkCaseReadyForDecision())
                .reason("Case not unmarked as never been marked")
                .thenExpect();
    }

    @Test
    public void shouldUnmarkCaseReadyForDecision() {
        when(caseAggregate.unmarkCaseReadyForDecision())
                .thenExpect(new CaseUnmarkedReadyForDecision(caseId));

        when(caseAggregate.unmarkCaseReadyForDecision())
                .reason("Case can not be unmarked twice")
                .thenExpect();
    }

}
