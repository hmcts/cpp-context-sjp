package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;


import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.Priority;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseAlreadyReserved;
import uk.gov.moj.cpp.sjp.event.CaseAlreadyUnReserved;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseReserved;
import uk.gov.moj.cpp.sjp.event.CaseUnReserved;

public class ReserveCaseHandlerTest {

    private CaseAggregateState caseAggregateState;

    private final UUID userId = UUID.randomUUID();
    @Before
    public void setUp() {
        // given
        caseAggregateState = new CaseAggregateState();
        caseAggregateState.setCaseId(UUID.randomUUID());
        caseAggregateState.setUrn("ABC0123");

    }

    @Test
    public void shouldReserveCase(){

        final List<Object> eventStream =  ReserveCaseHandler.INSTANCE.reserveCase(caseAggregateState, userId).collect(Collectors.toList());

        assertThat(eventStream.size(), is(2));
        final CaseReserved caseReserved = (CaseReserved) eventStream.get(0);
        assertThat(caseReserved.getCaseId(), is(caseAggregateState.getCaseId()));
        assertThat(caseReserved.getCaseUrn(), is("ABC0123"));
        assertThat(caseReserved.getReservedBy(), is(userId));
        assertThat(caseReserved.getReservedAt(), is(notNullValue()));

        final CaseMarkedReadyForDecision caseMarkedReadyForDecision = (CaseMarkedReadyForDecision) eventStream.get(1);
        assertThat(caseMarkedReadyForDecision.getCaseId(), is(caseAggregateState.getCaseId()));
        assertThat(caseMarkedReadyForDecision.getReason(), is(CaseReadinessReason.UNKNOWN));
        assertThat(caseMarkedReadyForDecision.getMarkedAt(), is(notNullValue()));
        assertThat(caseMarkedReadyForDecision.getSessionType(), is(SessionType.MAGISTRATE));
        assertThat(caseMarkedReadyForDecision.getPriority(), is(Priority.HIGH));

    }

    @Test
    public void shouldNotReserveWhenCaseIsReserved(){
        caseAggregateState.markCaseReserved();
        final List<Object> eventStream =  ReserveCaseHandler.INSTANCE.reserveCase(caseAggregateState, userId).collect(Collectors.toList());

        assertThat(eventStream.size(), is(1));
        final CaseAlreadyReserved caseReserved = (CaseAlreadyReserved) eventStream.get(0);
        assertThat(caseReserved.getCaseId(), is(caseAggregateState.getCaseId()));
    }

    @Test
    public void shouldUnReserveCase(){
        caseAggregateState.markCaseReserved();
        caseAggregateState.setPostingDate(LocalDate.now());

        final List<Object> eventStream =  ReserveCaseHandler.INSTANCE.unReserveCase(caseAggregateState, userId).collect(Collectors.toList());

        assertThat(eventStream.size(), is(1));
        final CaseUnReserved caseUnReserved = (CaseUnReserved) eventStream.get(0);
        assertThat(caseUnReserved.getCaseId(), is(caseAggregateState.getCaseId()));
        assertThat(caseUnReserved.getCaseUrn(), is("ABC0123"));
        assertThat(caseUnReserved.getReservedBy(), is(userId));

    }

    @Test
    public void shouldNotUnReserveWhenCaseIsUnReserved(){
        final List<Object> eventStream =  ReserveCaseHandler.INSTANCE.unReserveCase(caseAggregateState, userId).collect(Collectors.toList());

        assertThat(eventStream.size(), is(1));
        final CaseAlreadyUnReserved caseUnReserved = (CaseAlreadyUnReserved) eventStream.get(0);
        assertThat(caseUnReserved.getCaseId(), is(caseAggregateState.getCaseId()));
    }
}
