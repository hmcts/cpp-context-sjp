package uk.gov.moj.cpp.sjp.command.controller;

import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EmployerControllerTest {

    @Test
    public void shouldUpdateEmployer() {
        assertThat(EmployerController.class, isHandlerClass(COMMAND_CONTROLLER)
                .with(method("updateEmployer")
                        .thatHandles("sjp.command.update-employer")
                        .withSenderPassThrough()));
    }

    @Test
    public void shouldDeleteEmployer() {
        assertThat(EmployerController.class, isHandlerClass(COMMAND_CONTROLLER)
                .with(method("deleteEmployer")
                        .thatHandles("sjp.command.delete-employer")
                        .withSenderPassThrough()));
    }
}