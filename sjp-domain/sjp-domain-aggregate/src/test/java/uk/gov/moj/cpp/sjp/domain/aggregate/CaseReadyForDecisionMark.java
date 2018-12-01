package uk.gov.moj.cpp.sjp.domain.aggregate;

import static uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregateBaseTest.AggregateTester.when;

import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;

import org.junit.Test;

/**
 * Tests for {@link CaseAggregate#markCaseReadyForDecision}
 */
public class CaseReadyForDecisionMark extends CaseAggregateBaseTest {

    @Test
    public void shouldMarkCaseReadyForDecision() {
        final CaseReadinessReason readinessReason = CaseReadinessReason.PLEADED_GUILTY;

        when(caseAggregate.markCaseReadyForDecision(readinessReason, clock.now()))
                .thenExpect(
                        new CaseMarkedReadyForDecision(caseId, readinessReason, clock.now()));

        when(caseAggregate.markCaseReadyForDecision(readinessReason, clock.now()))
                .reason("if we recall the aggregate multiple times it should not add any events!")
                .thenExpect();

        final CaseReadinessReason aDifferentReadinessReason = CaseReadinessReason.PLEADED_GUILTY_REQUEST_HEARING;
        when(caseAggregate.markCaseReadyForDecision(aDifferentReadinessReason, clock.now()))
                .reason("markCaseReadyForDecision with a different reason")
                .thenExpect(new CaseMarkedReadyForDecision(caseId, aDifferentReadinessReason, clock.now()));
    }

}
