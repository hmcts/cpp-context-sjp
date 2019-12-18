package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.common.CaseState;
import uk.gov.moj.cpp.sjp.event.CaseStatusChanged;

import java.util.stream.Stream;

public class ResolveCaseStatusHandler {

    public static final ResolveCaseStatusHandler INSTANCE = new ResolveCaseStatusHandler();

    private ResolveCaseStatusHandler() {
    }

    public Stream<Object> resolveCaseStatus(CaseAggregateState caseAggregateState, CaseState caseState) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(new CaseStatusChanged(caseAggregateState.getCaseId(), caseState.getCaseStatus()));

        return streamBuilder.build();
    }
}