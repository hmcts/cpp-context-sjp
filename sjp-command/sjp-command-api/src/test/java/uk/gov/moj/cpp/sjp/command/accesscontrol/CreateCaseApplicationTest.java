package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getCreateCaseApplicationGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class CreateCaseApplicationTest extends BaseDroolsAccessControlTest {

    private static final String SJP_COMMAND_CREATE_CASE_APPLICATION = "sjp.create-case-application";

    public CreateCaseApplicationTest() {
        super("COMMAND_API_SESSION");
    }

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Test
    public void shouldAcceptCreateCaseApplication() {
        final Action action = createActionFor(SJP_COMMAND_CREATE_CASE_APPLICATION);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getCreateCaseApplicationGroups()))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldRejectCreateCaseApplication() {
        final Action action = createActionFor(SJP_COMMAND_CREATE_CASE_APPLICATION);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getCreateCaseApplicationGroups()))
                .willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}