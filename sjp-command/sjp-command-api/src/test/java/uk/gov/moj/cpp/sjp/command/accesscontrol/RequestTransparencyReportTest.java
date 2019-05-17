package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getRequestTransparencyReportGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class RequestTransparencyReportTest extends BaseDroolsAccessControlTest {

    private static final String SJP_COMMAND_REQUEST_TRANSPARENCY_REPORT = "sjp.request-transparency-report";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Test
    public void shouldAcceptRequestTransparencyReport() {
        final Action action = createActionFor(SJP_COMMAND_REQUEST_TRANSPARENCY_REPORT);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getRequestTransparencyReportGroups()))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldRejectRequestTransparencyReport() {
        final Action action = createActionFor(SJP_COMMAND_REQUEST_TRANSPARENCY_REPORT);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getRequestTransparencyReportGroups()))
                .willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }
}
