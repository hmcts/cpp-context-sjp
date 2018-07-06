package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.SjpProvider;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

public abstract class SjpDroolsAccessControlTest extends BaseDroolsAccessControlTest {

    @Mock
    protected UserAndGroupProvider userAndGroupProvider;

    @Mock
    protected SjpProvider sjpProvider;

    @Captor
    private ArgumentCaptor<List<String>> listCaptor;

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.of(UserAndGroupProvider.class, userAndGroupProvider, SjpProvider.class, sjpProvider);
    }

    private String actionName;
    private List<String> groups;
    private Action action;

    public SjpDroolsAccessControlTest(final String actionName, List<String> groups) {
        this.actionName = actionName;
        this.groups = groups;
    }

    @Before
    public void setup() {
        super.setup();
        action = createActionFor(actionName);
    }

    public ExecutionResults executeRules() {
        return executeRulesWith(action);
    }

    public void givenUserIsMemberOfAnyOfTheSuppliedGroups() {
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, groups)).willReturn(true);
    }

    public void givenUserIsNotMemberOfAnyOfTheSuppliedGroups() {
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, groups)).willReturn(false);
    }

    public void givenUserHasProsecutingAuthority() {
        given(sjpProvider.hasProsecutingAuthorityToCase(action)).willReturn(true);
    }

    public void givenUserHasNotProsecutingAuthority() {
        given(sjpProvider.hasProsecutingAuthorityToCase(action)).willReturn(false);
    }

    @After
    public void tearDown() {
        verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(eq(action), listCaptor.capture());
        assertThat(listCaptor.getValue(), containsInAnyOrder(groups.toArray()));
    }
}
