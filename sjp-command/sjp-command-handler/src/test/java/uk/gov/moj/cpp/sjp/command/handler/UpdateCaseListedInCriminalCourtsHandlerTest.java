package uk.gov.moj.cpp.sjp.command.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregateBaseTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdateCaseListedInCriminalCourtsHandlerTest extends CaseAggregateBaseTest {

    @Test
    public void shouldHandleUpdateCaseListedInCriminalCourtsCommand() {
        assertThat(UpdateCaseListedInCriminalCourtsHandler.class, isHandlerClass(COMMAND_HANDLER)
                .with(method("updateCaseListedInCriminalCourts").thatHandles(UpdateCaseListedInCriminalCourtsHandler.COMMAND_NAME)));
    }

}
