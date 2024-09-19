package uk.gov.moj.cpp.sjp.query.accesscontrol;

import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CaseSearchByMaterialIdTest extends BaseDroolsAccessControlTest {

    private static final String CONTENT_TYPE = "sjp.query.cases-search-by-material-id";

    private Action action;

    public CaseSearchByMaterialIdTest() {
        super("QUERY_CONTROLLER_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return new HashMap<>();
    }

    @BeforeEach
    public void setUp() {
        action = createActionFor(CONTENT_TYPE);
    }

    @Test
    public void shouldFallThrough() {
        assertSuccessfulOutcome(executeRulesWith(action));
    }
}