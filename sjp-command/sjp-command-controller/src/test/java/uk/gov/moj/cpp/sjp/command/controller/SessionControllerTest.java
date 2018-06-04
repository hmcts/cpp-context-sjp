package uk.gov.moj.cpp.sjp.command.controller;

import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SessionControllerTest {

    @Test
    public void shouldHandleStartSessionCommand() {
        assertThat(SessionController.class, isHandlerClass(COMMAND_CONTROLLER)
                .with(allOf(
                        method("startSession").thatHandles("sjp.command.start-session").withSenderPassThrough(),
                        method("endSession").thatHandles("sjp.command.end-session").withSenderPassThrough(),
                        method("migrateSession").thatHandles("sjp.command.migrate-session").withSenderPassThrough(),
                        method("assignCase").thatHandles("sjp.command.assign-case").withSenderPassThrough(),
                        method("unassignCase").thatHandles("sjp.command.unassign-case").withSenderPassThrough()
                )));
    }
}
