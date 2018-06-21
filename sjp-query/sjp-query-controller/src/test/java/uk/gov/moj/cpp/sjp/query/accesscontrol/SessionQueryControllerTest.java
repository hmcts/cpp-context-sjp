package uk.gov.moj.cpp.sjp.query.accesscontrol;

import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.moj.cpp.sjp.query.controller.SessionQueryController;

import org.junit.Test;

public class SessionQueryControllerTest {

    @Test
    public void shouldHandlesQuery() {
        assertThat(SessionQueryController.class, isHandlerClass(Component.QUERY_CONTROLLER)
                .with(method("findSession").thatHandles("sjp.query.session").withRequesterPassThrough()));
    }
}
