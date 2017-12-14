package uk.gov.moj.cpp.sjp.command.controller;

import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import org.junit.Test;

public class RemovePersonInfoControllerTest {

    @Test
    public void shouldPassThroughDuplicatePersonInfo() {
        assertThat(RemovePersonInfoController.class, isHandlerClass(COMMAND_CONTROLLER)
                .with(method("removePersonInfo")
                        .thatHandles("sjp.command.remove-person-info")
                        .withSenderPassThrough()));
    }

}