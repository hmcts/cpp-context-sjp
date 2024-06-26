package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static uk.gov.moj.cpp.sjp.domain.aggregate.domain.SessionRules.getPriority;


import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseAlreadyReserved;
import uk.gov.moj.cpp.sjp.event.CaseAlreadyUnReserved;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseReserved;
import uk.gov.moj.cpp.sjp.event.CaseUnReserved;

public class ReserveCaseHandler {

    public static final ReserveCaseHandler INSTANCE = new ReserveCaseHandler();

    private ReserveCaseHandler() {
    }

    public Stream<Object> reserveCase(CaseAggregateState caseAggregateState, UUID reservedBy) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if(caseAggregateState.getCaseReserved()){
            streamBuilder.add(new CaseAlreadyReserved(caseAggregateState.getCaseId()));
        }else {
            final ZonedDateTime reservedAt = ZonedDateTime.now();
            streamBuilder.add(new CaseReserved(caseAggregateState.getCaseId(), caseAggregateState.getUrn(), reservedAt, reservedBy));
            streamBuilder.add(new CaseMarkedReadyForDecision(caseAggregateState.getCaseId(), CaseReadinessReason.UNKNOWN, reservedAt, SessionType.MAGISTRATE, getPriority(caseAggregateState)));
        }
        return streamBuilder.build();
    }

    public Stream<Object> unReserveCase(CaseAggregateState caseAggregateState, UUID reservedBy) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if(caseAggregateState.getCaseReserved()){
            streamBuilder.add(new CaseUnReserved(caseAggregateState.getCaseId(), caseAggregateState.getUrn(), reservedBy));
        }else {
            streamBuilder.add(new CaseAlreadyUnReserved(caseAggregateState.getCaseId()));
        }
        return streamBuilder.build();
    }
}