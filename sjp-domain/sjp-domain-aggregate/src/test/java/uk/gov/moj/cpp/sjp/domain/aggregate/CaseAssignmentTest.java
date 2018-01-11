package uk.gov.moj.cpp.sjp.domain.aggregate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

import uk.gov.moj.cpp.sjp.domain.CaseAssignment;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.event.CaseAssignmentCreated;
import uk.gov.moj.cpp.sjp.event.CaseAssignmentDeleted;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class CaseAssignmentTest extends CaseAggregateBaseTest {

    private final static CaseAssignmentType CASE_ASSIGNMENT_TYPE = CaseAssignmentType.MAGISTRATE_DECISION;

    private CaseAssignment caseAssignment;

    @Before
    public void setup() {
        super.setUp();
        caseAssignment = new CaseAssignment(
                caseAggregate.getCaseId().toString(),
                CASE_ASSIGNMENT_TYPE.toString());
    }

    @Test
    public void shouldMarkCaseAssigned() {
        final List<Object> events = when(caseAggregate::caseAssignmentCreated);

        assertThat(caseAggregate.isCaseAssigned(), is(true));

        assertThat(events.size(), is(1));

        final Object event = events.get(0);
        assertThat(event, is(instanceOf(CaseAssignmentCreated.class)));

        verify(((CaseAssignmentCreated) event).getCaseAssignment());
    }

    @Test
    public void shouldMarkCaseUnassigned() {
        final List<Object> events = when(caseAggregate::caseAssignmentDeleted);

        assertThat(caseAggregate.isCaseAssigned(), is(false));

        assertThat(events.size(), is(1));

        final Object event = events.get(0);
        assertThat(event, is(instanceOf(CaseAssignmentDeleted.class)));

        verify(((CaseAssignmentDeleted) event).getCaseAssignment());
    }

    private List<Object> when(Function<CaseAssignment, Stream<Object>> f) {
        return f.apply(caseAssignment).collect(Collectors.toList());
    }

    private void verify(CaseAssignment caseAssignment) {
        assertEquals(aCase.getId().toString(), caseAssignment.getCaseId());
        assertEquals(CASE_ASSIGNMENT_TYPE, caseAssignment.getCaseAssignmentType());
    }
}

