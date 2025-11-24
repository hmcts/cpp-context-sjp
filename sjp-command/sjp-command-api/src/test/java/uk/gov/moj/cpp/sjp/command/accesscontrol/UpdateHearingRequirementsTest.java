package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getUpdateHearingRequirementsGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class UpdateHearingRequirementsTest extends BaseDroolsAccessControlTest {

    private static final String UPDATE_COURT_INTERPRETER_COMMAND_NAME = "sjp.update-hearing-requirements";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public UpdateHearingRequirementsTest() {
        super("COMMAND_API_SESSION");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroup() {
        final Action action = createActionFor(UPDATE_COURT_INTERPRETER_COMMAND_NAME);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getUpdateHearingRequirementsGroups())).willReturn(true);

        assertSuccessfulOutcome(executeRulesWith(action));
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroup() {
        final Action action = createActionFor(UPDATE_COURT_INTERPRETER_COMMAND_NAME);

        assertFailureOutcome(executeRulesWith(action));
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}
