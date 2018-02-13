package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getCancelRequestWithdrawalOfAllOffencesGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class CancelRequestWithdrawalAllOffencesTest extends BaseDroolsAccessControlTest {

    private static final String SJP_COMMAND_CANCEL_REQUEST_WITHDRAWAL_ALL_OFFENCES = "sjp.cancel-request-withdrawal-all-offences";
    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Test
    public void shouldAcceptWithdrawalOffencesRequestCancelForAppriopriateUsers() {
        final Action action = createActionFor(SJP_COMMAND_CANCEL_REQUEST_WITHDRAWAL_ALL_OFFENCES);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getCancelRequestWithdrawalOfAllOffencesGroups()))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldRejectTheWithdrawalRequestCancel() {
        final Action action = createActionFor(SJP_COMMAND_CANCEL_REQUEST_WITHDRAWAL_ALL_OFFENCES);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getCancelRequestWithdrawalOfAllOffencesGroups()))
                .willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }
}
