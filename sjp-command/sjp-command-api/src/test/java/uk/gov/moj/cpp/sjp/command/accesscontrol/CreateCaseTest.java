package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getCreateSjpCaseActionGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class CreateCaseTest extends BaseDroolsAccessControlTest {

    private static final String SJP_COMMAND_CREATE_CASE = "sjp.create-sjp-case";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public CreateCaseTest() {
        super("COMMAND_API_SESSION");
    }

    @Test
    public void shouldAllowAuthorisedUserToCreateCase() {
        final Action action = createActionFor(SJP_COMMAND_CREATE_CASE);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getCreateSjpCaseActionGroups()))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToCreateCase() {
        final String unAllowedGroup = "un allowed group";
        final Action action = createActionFor(SJP_COMMAND_CREATE_CASE);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}
