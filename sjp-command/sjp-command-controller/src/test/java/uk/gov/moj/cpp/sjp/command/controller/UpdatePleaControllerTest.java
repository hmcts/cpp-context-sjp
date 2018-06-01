package uk.gov.moj.cpp.sjp.command.controller;

import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdatePleaControllerTest {

    @Test
    public void shouldUpdatePlea() {
        assertThat(UpdatePleaController.class, isHandlerClass(COMMAND_CONTROLLER)
                .with(method("updatePlea")
                        .thatHandles("sjp.command.update-plea")
                        .withSenderPassThrough()));
    }

    @Test
    public void shouldCancelPlea() {
        assertThat(UpdatePleaController.class, isHandlerClass(COMMAND_CONTROLLER)
                .with(method("cancelPlea")
                        .thatHandles("sjp.command.cancel-plea")
                        .withSenderPassThrough()));
    }
}