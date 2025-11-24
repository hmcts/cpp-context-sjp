package uk.gov.moj.cpp.sjp.domain.aggregate.mutator;

import org.junit.jupiter.api.Test;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CCApplicationStatusUpdated;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UpdateCaseStatusOnCCApplicationResultMutatorTest {

    @Test
    public void shouldRelistedCaseWhenApplicationStatusIsStatutoryDeclarationGranted() {
        CCApplicationStatusUpdated ccApplicationStatusUpdated = CCApplicationStatusUpdated.ccApplicationStatusUpdated()
                .withApplicationId(randomUUID())
                .withCaseId(randomUUID())
                .withStatus(ApplicationStatus.STATUTORY_DECLARATION_GRANTED)
                .build();
        CaseAggregateState state = new CaseAggregateState();
        UpdateCaseStatusOnCCApplicationResultMutator.INSTANCE.apply(ccApplicationStatusUpdated, state);

        assertTrue(state.isCaseRelisted());
    }

    @Test
    public void shouldRelistedCaseWhenApplicationStatusIsReopeningGranted() {
        CCApplicationStatusUpdated ccApplicationStatusUpdated = CCApplicationStatusUpdated.ccApplicationStatusUpdated()
                .withApplicationId(randomUUID())
                .withCaseId(randomUUID())
                .withStatus(ApplicationStatus.REOPENING_GRANTED)
                .build();
        CaseAggregateState state = new CaseAggregateState();
        UpdateCaseStatusOnCCApplicationResultMutator.INSTANCE.apply(ccApplicationStatusUpdated, state);

        assertTrue(state.isCaseRelisted());
    }

    @Test
    public void shouldAppealedCaseWhenApplicationStatusIsReopeningGranted() {
        CCApplicationStatusUpdated ccApplicationStatusUpdated = CCApplicationStatusUpdated.ccApplicationStatusUpdated()
                .withApplicationId(randomUUID())
                .withCaseId(randomUUID())
                .withStatus(ApplicationStatus.APPEAL_ALLOWED)
                .build();
        CaseAggregateState state = new CaseAggregateState();
        UpdateCaseStatusOnCCApplicationResultMutator.INSTANCE.apply(ccApplicationStatusUpdated, state);

        assertTrue(state.isCaseAppealed());
    }
}
