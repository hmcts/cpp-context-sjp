package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getCaseReopenedActionGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class UndoCaseReopenedTest extends BaseDroolsAccessControlTest {

    private static final String UNDO_CASE_REOPENED_COMMAND = "sjp.undo-case-reopened-in-libra";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    private String unallowedUserGroup = UUID.randomUUID().toString();

    public UndoCaseReopenedTest() {
        super("COMMAND_API_SESSION");
    }

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


        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}
