package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getRequestDeleteDocsGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class RequestDeleteDocsTest extends BaseDroolsAccessControlTest {

    private static final String SJP_COMMAND_REQUEST_DELETE_DOCS = "sjp.request-delete-docs";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public RequestDeleteDocsTest() {
        super("COMMAND_API_SESSION");
    }

    @Test
    public void shouldAcceptRequestDeleteDocs() {
        final Action action = createActionFor(SJP_COMMAND_REQUEST_DELETE_DOCS);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getRequestDeleteDocsGroups()))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldRejectRequestDeleteDocs() {
        final Action action = createActionFor(SJP_COMMAND_REQUEST_DELETE_DOCS);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getRequestDeleteDocsGroups()))
                .willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}
