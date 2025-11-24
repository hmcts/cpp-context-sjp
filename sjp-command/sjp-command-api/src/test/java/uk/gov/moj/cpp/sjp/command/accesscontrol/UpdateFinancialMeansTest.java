package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getUpdateFinancialMeansGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class UpdateFinancialMeansTest extends BaseDroolsAccessControlTest {

    private static final String SJP_COMMAND_UPDATE_FINANCIAL_MEANS = "sjp.update-financial-means";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public UpdateFinancialMeansTest() {
        super("COMMAND_API_SESSION");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroup() {
        final Action action = createActionFor(SJP_COMMAND_UPDATE_FINANCIAL_MEANS);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getUpdateFinancialMeansGroups())).willReturn(true);

        assertSuccessfulOutcome(executeRulesWith(action));
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroup() {
        final Action action = createActionFor(SJP_COMMAND_UPDATE_FINANCIAL_MEANS);

        assertFailureOutcome(executeRulesWith(action));
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}
