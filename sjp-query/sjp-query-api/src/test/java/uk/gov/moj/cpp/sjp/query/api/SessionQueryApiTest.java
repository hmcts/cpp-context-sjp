package uk.gov.moj.cpp.sjp.query.api;

import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.core.annotation.Component;

import org.junit.Test;

public class SessionQueryApiTest {

    @Test
    public void shouldHandleQuery() {
        assertThat(SessionQueryApi.class, isHandlerClass(Component.QUERY_API)
                .with(method("findSession").thatHandles("sjp.query.session").withRequesterPassThrough()));
    }
}
