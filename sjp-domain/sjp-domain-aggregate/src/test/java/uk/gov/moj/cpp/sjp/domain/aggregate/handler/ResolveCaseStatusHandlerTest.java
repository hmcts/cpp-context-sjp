package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.ResolveCaseStatusHandler.INSTANCE;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.common.CaseState;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.event.CaseStatusChanged;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ResolveCaseStatusHandlerTest {

    private final CaseAggregateState caseAggregateState = new CaseAggregateState();

    @BeforeEach
    public void setUp() {
        // given
        caseAggregateState.setCaseId(UUID.randomUUID());
    }

    @Test
    public void shouldCreateCaseStatusChangedEvent() {
        // when
        final List<Object> eventStream = INSTANCE.resolveCaseStatus(caseAggregateState, new CaseState(CaseStatus.NO_PLEA_RECEIVED)).collect(toList());

        // then
        assertThat(eventStream, containsInAnyOrder(new CaseStatusChanged(caseAggregateState.getCaseId(), CaseStatus.NO_PLEA_RECEIVED)));
    }
}