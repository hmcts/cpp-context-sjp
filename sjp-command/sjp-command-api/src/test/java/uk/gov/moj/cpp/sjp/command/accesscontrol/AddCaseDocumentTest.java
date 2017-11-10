package uk.gov.moj.cpp.sjp.command.accesscontrol;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Arrays;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getAddCaseDocumentActionGroups;

public class AddCaseDocumentTest extends BaseDroolsAccessControlTest {

    private static final String STRUCTURE_COMMAND_ADD_CASE_DOCUMENT = "sjp.add-case-document";
    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Test
    public void shouldAllowAuthorisedUserToAddCaseDocument() {
        final Action action = createActionFor(STRUCTURE_COMMAND_ADD_CASE_DOCUMENT);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getAddCaseDocumentActionGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToAddCaseDocument() {
        final Action action = createActionFor(STRUCTURE_COMMAND_ADD_CASE_DOCUMENT);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, Arrays.asList("Random group")))
                .willReturn(false);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }

}
