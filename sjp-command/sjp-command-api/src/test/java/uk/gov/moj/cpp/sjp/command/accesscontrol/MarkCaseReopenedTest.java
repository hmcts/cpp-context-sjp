package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getCaseReopenedActionGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class MarkCaseReopenedTest extends BaseDroolsAccessControlTest {

    private static final String MARK_CASE_REOPENED_COMMAND = "sjp.mark-case-reopened-in-libra";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public MarkCaseReopenedTest() {
        super("COMMAND_API_SESSION");
    }

    @Test
    public void shouldAllowAuthorisedUserToMarkCaseAsReopened() {
        final Action action = createActionFor(MARK_CASE_REOPENED_COMMAND);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getCaseReopenedActionGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToMarkCaseAsReopened() {
        final Action action = createActionFor(MARK_CASE_REOPENED_COMMAND);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}
