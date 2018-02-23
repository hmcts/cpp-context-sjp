package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getUpdateDefendantNationalInsuranceNumberGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mockito.Mock;

public class UpdateDefendantInsuranceNumberTest extends BaseDroolsAccessControlTest {

    private static final String SJP_UPDATE_DEFENDANT_INSURANCE_NUMBER = "sjp.update-defendant-national-insurance-number";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Test
    public void shouldAllowUserInAuthorisedGroup() {
        final Action action = createActionFor(SJP_UPDATE_DEFENDANT_INSURANCE_NUMBER);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getUpdateDefendantNationalInsuranceNumberGroups())).willReturn(true);

        assertSuccessfulOutcome(executeRulesWith(action));
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroup() {
        final Action action = createActionFor(SJP_UPDATE_DEFENDANT_INSURANCE_NUMBER);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, Arrays.asList("random group"))).willReturn(false);

        assertFailureOutcome(executeRulesWith(action));
    }

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }
}
