package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getRequestPressTransparencyReportGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class RequestPressTransparencyReportTest extends BaseDroolsAccessControlTest {

    private static final String SJP_COMMAND_REQUEST_PRESS_TRANSPARENCY_REPORT = "sjp.request-press-transparency-report";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public RequestPressTransparencyReportTest() {
        super("COMMAND_API_SESSION");
    }

    @Test
    public void shouldAcceptRequestTransparencyReport() {
        final Action action = createActionFor(SJP_COMMAND_REQUEST_PRESS_TRANSPARENCY_REPORT);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getRequestPressTransparencyReportGroups()))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldRejectRequestTransparencyReport() {
        final Action action = createActionFor(SJP_COMMAND_REQUEST_PRESS_TRANSPARENCY_REPORT);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getRequestPressTransparencyReportGroups()))
                .willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}
