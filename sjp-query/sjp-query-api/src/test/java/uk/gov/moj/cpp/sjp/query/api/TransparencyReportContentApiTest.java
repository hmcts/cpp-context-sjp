package uk.gov.moj.cpp.sjp.query.api;

import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class TransparencyReportContentApiTest {

    @Test
    public void shouldHandleTransparencyReport() {
        assertThat(TransparencyReportContentApi.class,
                isHandlerClass(QUERY_API)
                        .with(method("getTransparencyReportContent")
                                .thatHandles("sjp.query.transparency-report-content")));
    }
}