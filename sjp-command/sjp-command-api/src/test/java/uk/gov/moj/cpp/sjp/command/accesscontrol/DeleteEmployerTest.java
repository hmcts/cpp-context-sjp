package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static java.util.Collections.singletonMap;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getDeleteEmployerGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class DeleteEmployerTest extends BaseDroolsAccessControlTest {

    private static final String SJP_COMMAND_DELETE_EMPLOYER = "sjp.delete-employer";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public DeleteEmployerTest() {
        super("COMMAND_API_SESSION");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroup() {
        final Action action = createActionFor(SJP_COMMAND_DELETE_EMPLOYER);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getDeleteEmployerGroups())).willReturn(true);

        assertSuccessfulOutcome(executeRulesWith(action));
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroup() {
        final Action action = createActionFor(SJP_COMMAND_DELETE_EMPLOYER);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(eq(action), anyList())).willReturn(false);

        assertFailureOutcome(executeRulesWith(action));
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}
