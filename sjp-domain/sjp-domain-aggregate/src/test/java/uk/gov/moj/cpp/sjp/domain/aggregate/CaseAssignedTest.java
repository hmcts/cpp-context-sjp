package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregateBaseTest.AggregateTester.when;
import static uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected.RejectReason.CASE_ASSIGNED_TO_OTHER_USER;
import static uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected.RejectReason.CASE_COMPLETED;

import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.event.session.CaseAlreadyAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link CaseAggregate#assignCase}
 */
public class CaseAssignedTest extends CaseAggregateBaseTest {

    private static final CaseAssignmentType CASE_ASSIGNMENT_TYPE = CaseAssignmentType.MAGISTRATE_DECISION;

    private UUID assigneeId;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        assigneeId = caseId;
    }

    @Test
    public void shouldAssignUncompletedCase() {
        when(callAggregateAssignCase(assigneeId))
                .thenExpect(new CaseAssigned(caseId, assigneeId, clock.now(), CASE_ASSIGNMENT_TYPE));

        when(callAggregateAssignCase(assigneeId))
                .reason("if we recall the aggregate multiple times it should not redo the same action!")
                .thenExpect(new CaseAlreadyAssigned(caseId, assigneeId));
    }

    @Test
    public void shouldNotAssignCaseAlreadyAssigned() {
        callAggregateAssignCase(assigneeId);
        final UUID aDifferentAssigneeId = randomUUID();

        when(callAggregateAssignCase(aDifferentAssigneeId))
                .thenExpect(new CaseAssignmentRejected(CASE_ASSIGNED_TO_OTHER_USER));
    }

    @Test
    public void shouldNotAssignCompletedCase() {
        caseAggregate.completeCase();

        when(callAggregateAssignCase(assigneeId))
                .thenExpect(new CaseAssignmentRejected(CASE_COMPLETED));
    }

    private Stream<Object> callAggregateAssignCase(final UUID assigneeId) {
        return caseAggregate.assignCase(assigneeId, clock.now(), CASE_ASSIGNMENT_TYPE);
    }

}
