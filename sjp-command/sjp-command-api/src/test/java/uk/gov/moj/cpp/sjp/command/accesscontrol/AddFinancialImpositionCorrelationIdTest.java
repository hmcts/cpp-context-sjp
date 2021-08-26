package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getAddFinancialImpositionCorrelationIdGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class AddFinancialImpositionCorrelationIdTest extends BaseDroolsAccessControlTest {

    private static final String SJP_COMMAND_ADD_FINANCIAL_IMPOSITION_CORRELATION_ID = "sjp.add-financial-imposition-correlation-id";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

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
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }
}
