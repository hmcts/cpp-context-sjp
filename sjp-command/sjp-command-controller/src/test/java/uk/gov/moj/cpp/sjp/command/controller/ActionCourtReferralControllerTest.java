package uk.gov.moj.cpp.sjp.command.controller;

import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.moj.cpp.sjp.command.controller.ActionCourtReferralController;

import org.junit.Test;

public class ActionCourtReferralControllerTest {

    @Test
    public void shouldActionCourtReferral() {
        assertThat(ActionCourtReferralController.class, isHandlerClass(COMMAND_CONTROLLER)
                .with(method("actionCourtReferral")
                        .thatHandles("sjp.command.action-court-referral")
                        .withSenderPassThrough()));
    }
}