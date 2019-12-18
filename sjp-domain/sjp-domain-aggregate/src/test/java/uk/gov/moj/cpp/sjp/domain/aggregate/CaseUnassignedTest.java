package uk.gov.moj.cpp.sjp.domain.aggregate;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.CaseAssignmentType.MAGISTRATE_DECISION;
import static uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregateBaseTest.AggregateTester.when;
import static uk.gov.moj.cpp.sjp.event.session.CaseUnassignmentRejected.RejectReason.CASE_NOT_ASSIGNED;

import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassignmentRejected;

import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link CaseAggregate#unassignCase}
 */
public class CaseUnassignedTest extends CaseAggregateBaseTest {

    @Before
    public void assignCaseBeforeStartAnyTest() {
        final CaseAssignmentType magistrateDecision = MAGISTRATE_DECISION;
        final Stream<Object> events = caseAggregate.assignCase(caseId, clock.now(), magistrateDecision);

        assertThat(collectFirstEvent(events, CaseAssigned.class).getCaseAssignmentType(), is(magistrateDecision));
    }

    @Test
    public void shouldRejectUnassignCaseWhenNotAssigned() {
        setUp(); // remove assignment made from @before

        when(caseAggregate.unassignCase())
                .thenExpect(new CaseUnassignmentRejected(CASE_NOT_ASSIGNED));
    }

    @Test
    public void shouldUnassignCase() {
        when(caseAggregate.unassignCase())
                .thenExpect(new CaseUnassigned(caseId));

        when(caseAggregate.unassignCase())
                .reason("if we recall the aggregate multiple times it should not unassign the case again!")
                .thenExpect(new CaseUnassignmentRejected(CASE_NOT_ASSIGNED));
    }

}
