package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getUpdateDefendantNationalInsuranceNumberGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class UpdateDefendantInsuranceNumberTest extends BaseDroolsAccessControlTest {

    private static final String SJP_UPDATE_DEFENDANT_INSURANCE_NUMBER = "sjp.update-defendant-national-insurance-number";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public UpdateDefendantInsuranceNumberTest() {
        super("COMMAND_API_SESSION");
    }

    @Test
    public void shouldAllowUserInAuthorisedGroup() {
        final Action action = createActionFor(SJP_UPDATE_DEFENDANT_INSURANCE_NUMBER);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getUpdateDefendantNationalInsuranceNumberGroups())).willReturn(true);

        assertSuccessfulOutcome(executeRulesWith(action));
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroup() {
        final Action action = createActionFor(SJP_UPDATE_DEFENDANT_INSURANCE_NUMBER);

        assertFailureOutcome(executeRulesWith(action));
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}
