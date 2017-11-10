package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class AssociateEnterpriseIdApiAccessControlTest extends BaseDroolsAccessControlTest {

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Test
    public void shouldAllowSystemUserToAssociateEnterpriseId() {
        final Action action = createActionFor("sjp.associate-enterprise-id");
        given(userAndGroupProvider.isSystemUser(action))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);

        verify(userAndGroupProvider).isSystemUser(action);
    }

    @Test
    public void shouldNotAllowSystemUserToAssociateEnterpriseId() {
        final Action action = createActionFor("sjp.associate-enterprise-id");
        given(userAndGroupProvider.isSystemUser(action))
                .willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);

        verify(userAndGroupProvider).isSystemUser(action);
    }

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }
}
