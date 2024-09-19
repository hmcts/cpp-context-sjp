package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getPleadOnlineActionGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class PleadOnlineTest extends BaseDroolsAccessControlTest {

    private static final String SJP_COMMAND_PLEAD_ONLINE = "sjp.plead-online";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public PleadOnlineTest() {
        super("COMMAND_API_SESSION");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToUpdatePlea() {
        final Action action = createActionFor(SJP_COMMAND_PLEAD_ONLINE);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getPleadOnlineActionGroups()))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroupToUpdatePlea() {
        final Action action = createActionFor(SJP_COMMAND_PLEAD_ONLINE);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}
