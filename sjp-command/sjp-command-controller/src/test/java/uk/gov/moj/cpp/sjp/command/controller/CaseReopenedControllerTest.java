package uk.gov.moj.cpp.sjp.command.controller;

import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseReopenedControllerTest {
    @Test
    public void shouldHandleMarkCaseReopenedInLibraCommand() {
        assertThat(CaseReopenedController.class, isHandlerClass(COMMAND_CONTROLLER)
                .with(
                        method("markCaseReopenedInLibra")
                                .thatHandles("sjp.command.mark-case-reopened-in-libra")
                                .withSenderPassThrough()
                ));
    }

    @Test
    public void shouldHandleUpdateCaseReopenedInLibraCommand() {
        assertThat(CaseReopenedController.class, isHandlerClass(COMMAND_CONTROLLER)
                .with(
                        method("updateCaseReopenedInLibra")
                                .thatHandles("sjp.command.update-case-reopened-in-libra")
                                .withSenderPassThrough()
                ));
    }

    @Test
    public void shouldHandleUndoCaseReopenedInLibraCommand() {
        assertThat(CaseReopenedController.class, isHandlerClass(COMMAND_CONTROLLER)
                .with(
                        method("undoCaseReopenedInLibra")
                                .thatHandles("sjp.command.undo-case-reopened-in-libra")
                                .withSenderPassThrough()
                ));
    }
}