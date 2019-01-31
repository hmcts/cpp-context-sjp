package uk.gov.moj.cpp.sjp.domain.aggregate;

import static uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregateBaseTest.AggregateTester.when;

import uk.gov.moj.cpp.sjp.event.CaseAlreadyCompleted;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;

import org.junit.Test;

/**
 * Tests for {@link CaseAggregate#completeCase()}
 */
public class CaseCompletedTest extends CaseAggregateBaseTest {

    @Test
    public void shouldCompleteCase() {
        when(caseAggregate.completeCase())
                .thenExpect(
                        new CaseUnassigned(caseId),
                        new CaseCompleted(caseId));
    }

    @Test
    public void shouldNotCompleteCaseAlreadyCompleted() {
        caseAggregate.completeCase();

        when(caseAggregate.completeCase())
                .thenExpect(
                        new CaseAlreadyCompleted(caseId, "Complete Case"));
    }

}