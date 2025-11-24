package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CCApplicationStatusUpdated;

public class UpdateCaseStatusOnCCApplicationResultMutator implements AggregateStateMutator<CCApplicationStatusUpdated, CaseAggregateState> {

    static final UpdateCaseStatusOnCCApplicationResultMutator INSTANCE = new UpdateCaseStatusOnCCApplicationResultMutator();

    private UpdateCaseStatusOnCCApplicationResultMutator() {
    }

    @Override
    public void apply(final CCApplicationStatusUpdated event, final CaseAggregateState state) {
       final ApplicationStatus applicationStatus = event.getStatus();
        if (applicationStatus.equals(ApplicationStatus.STATUTORY_DECLARATION_GRANTED)
                || applicationStatus.equals(ApplicationStatus.REOPENING_GRANTED)) {

            state.setCaseRelisted(true);
        } else if(applicationStatus.equals(ApplicationStatus.APPEAL_ALLOWED)) {
            state.setCaseAppealed(true);

        }
    }
}
