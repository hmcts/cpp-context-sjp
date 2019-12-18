package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getSetPleasGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class SetPleasTest extends BaseDroolsAccessControlTest {

    private static final String SJP_SET_PLEAS = "sjp.set-pleas";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

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
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, asList("random group")))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }
}
