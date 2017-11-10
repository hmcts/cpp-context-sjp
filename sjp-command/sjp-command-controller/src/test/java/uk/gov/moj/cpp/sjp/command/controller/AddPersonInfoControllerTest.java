package uk.gov.moj.cpp.sjp.command.controller;

import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.moj.cpp.sjp.command.controller.AddPersonInfoController;

import org.junit.Test;

public class AddPersonInfoControllerTest {

    @Test
    public void shouldAddPersonInfo() {
    assertThat(AddPersonInfoController.class, isHandlerClass(COMMAND_CONTROLLER)
                .with(method("addPersonInfo")
                        .thatHandles("sjp.command.add-person-info")
                        .withSenderPassThrough()));
    }

}