package uk.gov.moj.cpp.sjp.command.controller;

import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CancelRequestWithdrawalAllOffencesControllerTest {

    @Test
    public void shouldCancelRequestWithdrawalAllOffences() {
        assertThat(CancelRequestWithdrawalAllOffencesController.class, isHandlerClass(COMMAND_CONTROLLER)
                .with(method("cancelRequestWithdrawalAllOffences")
                        .thatHandles("sjp.command.cancel-request-withdrawal-all-offences")
                        .withSenderPassThrough()));
    }
}
