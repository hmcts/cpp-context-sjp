package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.ChangeCaseManagementStatusHandler.INSTANCE;
import static uk.gov.moj.cpp.sjp.domain.common.CaseManagementStatus.IN_PROGRESS;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.casemanagement.CaseManagementStatusChanged;

import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class ChangeCaseManagementStatusHandlerTest {

    private final CaseAggregateState caseAggregateState = new CaseAggregateState();

    @Before
    public void setUp() {
        // given
        caseAggregateState.setCaseId(UUID.randomUUID());
    }

    @Test
    public void shouldUpdateCaseManagementStatus() {
        // when
        final List<Object> eventStream = INSTANCE.changeCaseManagementStatus(caseAggregateState, IN_PROGRESS).collect(toList());

        // then
        assertThat(eventStream, containsInAnyOrder(new CaseManagementStatusChanged(caseAggregateState.getCaseId(), IN_PROGRESS)));
    }
}
