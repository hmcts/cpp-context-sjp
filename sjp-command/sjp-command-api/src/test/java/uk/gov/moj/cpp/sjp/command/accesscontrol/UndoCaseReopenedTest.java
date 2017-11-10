package uk.gov.moj.cpp.sjp.command.accesscontrol;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getCaseReopenedActionGroups;

public class UndoCaseReopenedTest extends BaseDroolsAccessControlTest {

    private static final String UNDO_CASE_REOPENED_COMMAND = "sjp.undo-case-reopened-in-libra";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    private String unallowedUserGroup = UUID.randomUUID().toString();

    @Test
    public void shouldAllowAuthorisedUserToUndoCaseReopened() {

        final Action action = createActionFor(UNDO_CASE_REOPENED_COMMAND);

        given(
                userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(
                        action, getCaseReopenedActionGroups())
        ).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToUndoCaseReopened() {

        final Action action = createActionFor(UNDO_CASE_REOPENED_COMMAND);

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
