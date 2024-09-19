package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getAddCaseDocumentActionGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class AddCaseDocumentTest extends BaseDroolsAccessControlTest {

    private static final String SJP_COMMAND_ADD_CASE_DOCUMENT = "sjp.add-case-document";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public AddCaseDocumentTest() {
        super("COMMAND_API_SESSION");
    }

    @Test
    public void shouldAllowAuthorisedUserToAddCaseDocument() {
        final Action action = createActionFor(SJP_COMMAND_ADD_CASE_DOCUMENT);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getAddCaseDocumentActionGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToAddCaseDocument() {
        final Action action = createActionFor(SJP_COMMAND_ADD_CASE_DOCUMENT);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

}
