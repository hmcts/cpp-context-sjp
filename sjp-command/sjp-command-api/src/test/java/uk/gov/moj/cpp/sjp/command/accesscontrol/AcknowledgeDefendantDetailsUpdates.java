package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getAcknowledgeDefendantDetailsUpdatesGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class AcknowledgeDefendantDetailsUpdates extends BaseDroolsAccessControlTest {

    private static final String SJP_COMMAND_ACKNOWLEDGE_DEFENDANT_DETAILS_UPDATES = "sjp.acknowledge-defendant-details-updates";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public AcknowledgeDefendantDetailsUpdates() {
        super("COMMAND_API_SESSION");
    }

    @Test
    public void shouldAllowAuthorisedUserToAcknowledgeDefendantDetailsUpdates() {
        final Action action = createActionFor(SJP_COMMAND_ACKNOWLEDGE_DEFENDANT_DETAILS_UPDATES);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getAcknowledgeDefendantDetailsUpdatesGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowAuthorisedUserToAcknowledgeDefendantDetailsUpdates() {
        final Action action = createActionFor(SJP_COMMAND_ACKNOWLEDGE_DEFENDANT_DETAILS_UPDATES);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getAcknowledgeDefendantDetailsUpdatesGroups()))
                .willReturn(false);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}
