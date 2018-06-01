package uk.gov.moj.cpp.sjp.command.controller;

import static org.junit.Assert.*;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import org.junit.Test;

public class PleadOnlineControllerTest {

    @Test
    public void shouldPleadOnline() {
        assertThat(PleadOnlineController.class, isHandlerClass(COMMAND_CONTROLLER)
                .with(method("pleadOnline")
                        .thatHandles("sjp.command.plead-online")
                        .withSenderPassThrough()));
    }

}