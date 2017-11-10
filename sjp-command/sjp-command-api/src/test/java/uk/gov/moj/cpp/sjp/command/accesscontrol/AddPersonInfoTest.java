package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getAddPersonInfoGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mockito.Mock;

public class AddPersonInfoTest extends BaseDroolsAccessControlTest {

    private static final String STRUCTURE_COMMAND = "sjp.add-person-info";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Test
    public void shouldAllowUserInAuthorisedGroupToUpdateFinancialMeansCommand() {
        final Action action = createActionFor(STRUCTURE_COMMAND);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getAddPersonInfoGroups())).willReturn(true);

        assertSuccessfulOutcome(executeRulesWith(action));
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroupToUpdateFinancialPleasCommand() {
        final Action action = createActionFor(STRUCTURE_COMMAND);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, Arrays.asList("random group"))).willReturn(false);

        assertFailureOutcome(executeRulesWith(action));
    }

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }
}
