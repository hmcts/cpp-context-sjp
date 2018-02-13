package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getCancelPleaActionGroups;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getUpdatePleaActionGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class UpdatePleaTest extends BaseDroolsAccessControlTest {

    private static final String SJP_COMMAND_UPDATE_PLEA = "sjp.update-plea";
    private static final String SJP_COMMAND_CANCEL_PLEA = "sjp.cancel-plea";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Test
    public void shouldAllowUserInAuthorisedGroupToUpdatePlea() {
        final Action action = createActionFor(SJP_COMMAND_UPDATE_PLEA);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getUpdatePleaActionGroups()))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToCancelPlea() {
        final Action action = createActionFor(SJP_COMMAND_CANCEL_PLEA);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getCancelPleaActionGroups()))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroupToUpdateCpsPlea() {
        final Action action = createActionFor(SJP_COMMAND_UPDATE_PLEA);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, Arrays.asList("random group")))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroupToUpdatePlea() {
        final Action action = createActionFor(SJP_COMMAND_UPDATE_PLEA);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, Arrays.asList("random group")))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroupToCancelPlea() {
        final Action action = createActionFor(SJP_COMMAND_CANCEL_PLEA);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, Arrays.asList("random group")))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }
}
