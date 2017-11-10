package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.moj.cpp.sjp.query.api.RuleConstants.getQueryAwaitingCasesActionGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

public class AwaitingCasesTest extends BaseDroolsAccessControlTest {

    private static final String CONTENT_TYPE = "sjp.query.awaiting-cases";

    private Action action;

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Captor
    private ArgumentCaptor<List<String>> listCaptor;

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }

    @Before
    public void setUp() {
        action = createActionFor(CONTENT_TYPE);
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToQueryAwaitingCases() {
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getQueryAwaitingCasesActionGroups())).willReturn(true);
        assertSuccessfulOutcome(executeRulesWith(action));
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroupToQueryAwaitingCases() {
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getQueryAwaitingCasesActionGroups())).willReturn(false);
        assertFailureOutcome(executeRulesWith(action));
    }

    @After
    public void tearDown() {
        verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(eq(action), listCaptor.capture());
        assertThat(listCaptor.getValue(), containsInAnyOrder(getQueryAwaitingCasesActionGroups().toArray()));
        verifyNoMoreInteractions(userAndGroupProvider);
    }
}