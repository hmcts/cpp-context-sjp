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

public class SaveApplicationDecisionTest extends BaseDroolsAccessControlTest {

    private static final String SJP_SAVE_APPLICATION_DECISION = "sjp.save-application-decision";

    public SaveApplicationDecisionTest() {
        super("COMMAND_API_SESSION");
    }

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Test
    public void shouldAllowLegaAdviser() {
        final Action action = createActionFor(SJP_SAVE_APPLICATION_DECISION);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getSaveDecisionActionGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowOtherRoles() {
        final Action action = createActionFor(SJP_SAVE_APPLICATION_DECISION);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}
