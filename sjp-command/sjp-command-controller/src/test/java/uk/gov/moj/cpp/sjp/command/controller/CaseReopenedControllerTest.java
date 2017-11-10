package uk.gov.moj.cpp.sjp.command.controller;

import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.moj.cpp.sjp.command.controller.CaseReopenedController;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseReopenedControllerTest {
    @Test
    public void shouldHandleMarkCaseReopenedInLibraCommand() throws Exception {
        assertThat(CaseReopenedController.class, isHandlerClass(COMMAND_CONTROLLER)
                .with(
                        method("markCaseReopenedInLibra")
                                .thatHandles("sjp.command.mark-case-reopened-in-libra")
                                .withSenderPassThrough()
                ));
    }

    @Test
    public void shouldHandleUpdateCaseReopenedInLibraCommand() throws Exception {
        assertThat(CaseReopenedController.class, isHandlerClass(COMMAND_CONTROLLER)
                .with(
                        method("updateCaseReopenedInLibra")
                                .thatHandles("sjp.command.update-case-reopened-in-libra")
                                .withSenderPassThrough()
                ));
    }
}