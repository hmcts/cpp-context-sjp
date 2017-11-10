package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getUpdateEmployerGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mockito.Mock;

public class UpdateEmployerTest extends BaseDroolsAccessControlTest {

    private static final String STRUCTURE_COMMAND_UPDATE_EMPLOYER = "sjp.update-employer";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Test
    public void shouldAllowUserInAuthorisedGroup() {
        final Action action = createActionFor(STRUCTURE_COMMAND_UPDATE_EMPLOYER);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getUpdateEmployerGroups())).willReturn(true);

        assertSuccessfulOutcome(executeRulesWith(action));
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroup() {
        final Action action = createActionFor(STRUCTURE_COMMAND_UPDATE_EMPLOYER);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, Arrays.asList("random group"))).willReturn(false);

        assertFailureOutcome(executeRulesWith(action));
    }

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }
}
