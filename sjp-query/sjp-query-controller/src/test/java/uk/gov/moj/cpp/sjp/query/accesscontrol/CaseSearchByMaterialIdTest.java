package uk.gov.moj.cpp.sjp.query.accesscontrol;

import org.junit.Before;
import org.junit.Test;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.HashMap;
import java.util.Map;

public class CaseSearchByMaterialIdTest extends BaseDroolsAccessControlTest {

    private static final String CONTENT_TYPE = "sjp.query.cases-search-by-material-id";

    private Action action;

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return new HashMap<>();
    }

    @Before
    public void setUp() throws Exception {
        action = createActionFor(CONTENT_TYPE);
    }

    @Test
    public void shouldFallThrough() {
        assertSuccessfulOutcome(executeRulesWith(action));
    }
}