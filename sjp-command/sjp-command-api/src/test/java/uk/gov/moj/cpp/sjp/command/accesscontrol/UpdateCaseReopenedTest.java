package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getCaseReopenedActionGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class UpdateCaseReopenedTest extends BaseDroolsAccessControlTest {

    private static final String UPDATE_CASE_REOPENED_COMMAND = "sjp.update-case-reopened-in-libra";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    private String unallowedUserGroup = UUID.randomUUID().toString();

    @Test
    public void shouldAllowAuthorisedUserToUpdateCaseAsReopened() {

        final Action action = createActionFor(UPDATE_CASE_REOPENED_COMMAND);

        given(
                userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(
                        action, getCaseReopenedActionGroups())
        ).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToUpdateCaseAsReopened() {

        final Action action = createActionFor(UPDATE_CASE_REOPENED_COMMAND);

        given(
                userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, unallowedUserGroup)
        ).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }
}
