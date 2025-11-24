package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getSaveDecisionActionGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class SaveDecisionTest extends BaseDroolsAccessControlTest {

    private static final String SJP_COMMAND_SAVE_DECISION = "sjp.save-decision";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public SaveDecisionTest() {
        super("COMMAND_API_SESSION");
    }

    @Test
    public void shouldAllowLegaAdviserForSaveDecisionAction() {
        final Action action = createActionFor(SJP_COMMAND_SAVE_DECISION);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getSaveDecisionActionGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowOtherRolesFromSaveDecisionAction() {
        final Action action = createActionFor(SJP_COMMAND_SAVE_DECISION);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}
