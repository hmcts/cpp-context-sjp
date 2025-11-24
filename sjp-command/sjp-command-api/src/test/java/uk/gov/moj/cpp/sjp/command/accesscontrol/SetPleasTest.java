package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getSetPleasGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class SetPleasTest extends BaseDroolsAccessControlTest {

    private static final String SJP_SET_PLEAS = "sjp.set-pleas";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public SetPleasTest() {
        super("COMMAND_API_SESSION");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToUpdatePlea() {
        final Action action = createActionFor(SJP_SET_PLEAS);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getSetPleasGroups()))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroupToUpdatePlea() {
        final Action action = createActionFor(SJP_SET_PLEAS);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}
