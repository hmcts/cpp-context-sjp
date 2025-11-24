package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.sjp.domain.CaseAssignmentType.MAGISTRATE_DECISION;
import static uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregateBaseTest.AggregateTester.when;
import static uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected.RejectReason.CASE_ASSIGNED_TO_OTHER_USER;
import static uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected.RejectReason.CASE_COMPLETED;

import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.testutils.AggregateHelper;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.session.CaseAlreadyAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link CaseAggregate#assignCase}
 */
@SuppressWarnings({"squid:S2699"})
@ExtendWith(MockitoExtension.class)
public class CaseAssignedTest extends CaseAggregateBaseTest {

    private static final CaseAssignmentType CASE_ASSIGNMENT_TYPE = MAGISTRATE_DECISION;

    private UUID assigneeId;



    @BeforeEach
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
    public void shouldAssignCaseToUser() {
        Mockito.when(session.getSessionType()).thenReturn(SessionType.DELEGATED_POWERS);
        AggregateHelper.saveDecision(caseAggregate, aCase, session, VerdictType.FOUND_NOT_GUILTY);

        caseAggregate.assignCaseToUser(assigneeId, now());

        when(callAggregateAssignCase(assigneeId))
                .thenExpect(new CaseAssignmentRejected(CASE_COMPLETED));
    }

    @Test
    public void shouldNotAssignCompletedCase() {
        Mockito.when(session.getSessionType()).thenReturn(SessionType.DELEGATED_POWERS);
        AggregateHelper.saveDecision(caseAggregate, aCase, session, VerdictType.FOUND_NOT_GUILTY);

        caseAggregate.assignCase(assigneeId, now(), MAGISTRATE_DECISION);

        when(callAggregateAssignCase(assigneeId))
                .thenExpect(new CaseAssignmentRejected(CASE_COMPLETED));
    }

    private Stream<Object> callAggregateAssignCase(final UUID assigneeId) {
        return caseAggregate.assignCase(assigneeId, clock.now(), CASE_ASSIGNMENT_TYPE);
    }

}
