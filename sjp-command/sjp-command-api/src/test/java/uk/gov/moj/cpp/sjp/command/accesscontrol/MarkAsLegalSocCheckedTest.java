package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getMarkAsLegalSocCheckedGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class MarkAsLegalSocCheckedTest extends BaseDroolsAccessControlTest {

    private static final String LEGAL_SOC_CHECKED_COMMAND = "sjp.mark-as-legal-soc-checked";
    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Test
    public void shouldAllowAuthorisedUserToMarkAsLegalSocChecked() {
        final Action action = createActionFor(LEGAL_SOC_CHECKED_COMMAND);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getMarkAsLegalSocCheckedGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToMarkAsLegalSocChecked() {
        final Action action = createActionFor(LEGAL_SOC_CHECKED_COMMAND);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "unAllowedGroups"))
                .willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }
}
