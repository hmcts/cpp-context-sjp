package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getAddFinancialImpositionCorrelationIdGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class AddFinancialImpositionCorrelationIdTest extends BaseDroolsAccessControlTest {

    private static final String SJP_COMMAND_ADD_FINANCIAL_IMPOSITION_CORRELATION_ID = "sjp.add-financial-imposition-correlation-id";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public AddFinancialImpositionCorrelationIdTest() {
        super("COMMAND_API_SESSION");
    }


    @Test
    public void shouldAcceptAddFinancialImpositionCorrelationId() {
        final Action action = createActionFor(SJP_COMMAND_ADD_FINANCIAL_IMPOSITION_CORRELATION_ID);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getAddFinancialImpositionCorrelationIdGroups()))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldRejectAddFinancialImpositionCorrelationId() {
        final Action action = createActionFor(SJP_COMMAND_ADD_FINANCIAL_IMPOSITION_CORRELATION_ID);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getAddFinancialImpositionCorrelationIdGroups()))
                .willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}
