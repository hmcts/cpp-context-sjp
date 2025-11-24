package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getAddFinancialImpositionAccountNumberBdfGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class AddFinancialImpositionAccountNumberBdfTest extends BaseDroolsAccessControlTest {

    private static final String SJP_COMMAND_ADD_FINANCIAL_IMPOSITION_ACCOUNT_NUMBER = "sjp.add-financial-imposition-account-number-bdf";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public AddFinancialImpositionAccountNumberBdfTest() {
        super("COMMAND_API_SESSION");
    }

    @Test
    public void shouldRejectAddFinancialImpositionAccountNumber() {
        final Action action = createActionFor(SJP_COMMAND_ADD_FINANCIAL_IMPOSITION_ACCOUNT_NUMBER);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getAddFinancialImpositionAccountNumberBdfGroups()))
                .willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}
