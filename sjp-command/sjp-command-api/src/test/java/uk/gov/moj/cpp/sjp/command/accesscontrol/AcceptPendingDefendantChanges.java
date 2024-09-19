package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getAcceptPendingDefendantChanges;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class AcceptPendingDefendantChanges extends BaseDroolsAccessControlTest {

    private static final String SJP_COMMAND_ACCEPT_PENDING_DEFENDANT_CHANGES = "sjp.accept-pending-defendant-changes";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public AcceptPendingDefendantChanges() {
        super("COMMAND_API_SESSION");
    }

    @Test
    public void shouldAllowAuthorisedUserToAcceptPendingDefendantChanges() {
        final Action action = createActionFor(SJP_COMMAND_ACCEPT_PENDING_DEFENDANT_CHANGES);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getAcceptPendingDefendantChanges()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowAuthorisedUserToAcceptPendingDefendantChanges() {
        final Action action = createActionFor(SJP_COMMAND_ACCEPT_PENDING_DEFENDANT_CHANGES);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getAcceptPendingDefendantChanges()))
                .willReturn(false);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

}
