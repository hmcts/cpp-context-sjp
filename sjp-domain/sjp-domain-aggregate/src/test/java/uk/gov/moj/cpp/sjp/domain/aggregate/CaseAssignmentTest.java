package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.event.CaseAssigned;
import uk.gov.moj.cpp.sjp.event.CaseAssignmentDeleted;

import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class CaseAssignmentTest extends CaseAggregateBaseTest {

    private final static CaseAssignmentType CASE_ASSIGNMENT_TYPE = CaseAssignmentType.MAGISTRATE_DECISION;
    private UUID caseId, assigneeId;

    @Before
    public void setup() {
        caseId = UUID.randomUUID();
        assigneeId = UUID.randomUUID();
    }

    @Test
    public void shouldMarkCaseAssigned() {
        final List<Object> events = caseAggregate.caseAssignmentCreated(caseId, assigneeId, CASE_ASSIGNMENT_TYPE).collect(toList());

        assertThat(caseAggregate.isCaseAssigned(), is(true));

        assertThat(events.size(), is(1));

        final Object event = events.get(0);
        assertThat(event, instanceOf(CaseAssigned.class));

        final CaseAssigned caseAssigned = (CaseAssigned) event;

        assertThat(caseAssigned.getCaseId(), is(caseId));
        assertThat(caseAssigned.getAssigneeId(), is(assigneeId));
        assertThat(caseAssigned.getCaseAssignmentType(), is(CASE_ASSIGNMENT_TYPE));
    }

    @Test
    public void shouldMarkCaseUnassigned() {
        final List<Object> events = caseAggregate.caseAssignmentDeleted(caseId, CASE_ASSIGNMENT_TYPE).collect(toList());

        assertThat(caseAggregate.isCaseAssigned(), is(false));

        assertThat(events.size(), is(1));

        final Object event = events.get(0);
        assertThat(event, is(instanceOf(CaseAssignmentDeleted.class)));

        final CaseAssignmentDeleted assignmentDeletedEvent = (CaseAssignmentDeleted) event;

        assertThat(assignmentDeletedEvent.getCaseId(), is(caseId));
        assertThat(assignmentDeletedEvent.getCaseAssignmentType(), is(CASE_ASSIGNMENT_TYPE));
    }
}

