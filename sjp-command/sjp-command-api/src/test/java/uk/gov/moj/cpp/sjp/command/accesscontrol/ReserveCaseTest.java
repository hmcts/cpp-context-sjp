package uk.gov.moj.cpp.sjp.command.accesscontrol;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getSetPleasGroups;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.reserveCaseGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class ReserveCaseTest extends BaseDroolsAccessControlTest {

    private static final String RESERVE_CASE = "sjp.reserve-case";
    private static final String UNRESERVE_CASE = "sjp.undo-reserve-case";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public ReserveCaseTest() {
        super("COMMAND_API_SESSION");
    }

    @Test
    public void shouldAllowLegalAdviserUserToReserveCase() {
        final Action action = createActionFor(RESERVE_CASE);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, singletonList("Legal Advisers")))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldAllowSystemUserOrLegalAdviserToUnreserveCase() {
        final Action action = createActionFor(UNRESERVE_CASE);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, Arrays.asList("System Users", "Legal Advisers")))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}
