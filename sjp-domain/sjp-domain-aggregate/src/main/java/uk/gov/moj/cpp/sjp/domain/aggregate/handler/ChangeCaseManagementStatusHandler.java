package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.common.CaseManagementStatus;
import uk.gov.moj.cpp.sjp.event.casemanagement.CaseManagementStatusChanged;

import java.util.stream.Stream;

public class ChangeCaseManagementStatusHandler {

    public static final ChangeCaseManagementStatusHandler INSTANCE = new ChangeCaseManagementStatusHandler();

    private ChangeCaseManagementStatusHandler() {
    }

    public Stream<Object> changeCaseManagementStatus(CaseAggregateState caseAggregateState, CaseManagementStatus caseManagementStatus) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(new CaseManagementStatusChanged(caseAggregateState.getCaseId(), caseManagementStatus));

        return streamBuilder.build();
    }
}
