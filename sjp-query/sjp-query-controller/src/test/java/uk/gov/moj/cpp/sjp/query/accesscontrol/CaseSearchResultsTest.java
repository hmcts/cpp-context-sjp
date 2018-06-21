package uk.gov.moj.cpp.sjp.query.accesscontrol;

import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class CaseSearchResultsTest extends BaseDroolsAccessControlTest {

    private static final String CONTENT_TYPE = "sjp.query.case-search-results";

    private Action action;

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return new HashMap<>();
    }

    @Before
    public void setUp() {
        action = createActionFor(CONTENT_TYPE);
    }

    @Test
    public void shouldFallThrough() {
        assertSuccessfulOutcome(executeRulesWith(action));
    }
}