package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getAddCaseAssignmentRestrictionActionGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class CaseAssignmentRestrictionTest extends BaseDroolsAccessControlTest {

    private static final String SJP_COMMAND_ADD_CASE_ASSIGNMENT_RESTRICTION = "sjp.add-case-assignment-restriction";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Test
    public void shouldAllowAuthorisedUserToAddCaseAssignmentRestriction() {
        final Action action = createActionFor(SJP_COMMAND_ADD_CASE_ASSIGNMENT_RESTRICTION);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getAddCaseAssignmentRestrictionActionGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToAddCaseAssignmentRestriction() {
        final Action action = createActionFor(SJP_COMMAND_ADD_CASE_ASSIGNMENT_RESTRICTION);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, eq(anyListOf(String.class))))
                .willReturn(false);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }
}
