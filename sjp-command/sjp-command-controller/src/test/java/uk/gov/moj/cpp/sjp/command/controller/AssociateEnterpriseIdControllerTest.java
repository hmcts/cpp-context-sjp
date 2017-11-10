package uk.gov.moj.cpp.sjp.command.controller;

import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.moj.cpp.sjp.command.controller.AssociateEnterpriseIdController;

import org.junit.Test;

public class AssociateEnterpriseIdControllerTest {

    @Test
    public void testAssociateEnterpriseIdHandlesExpectedActionAsPassThrough() {
        assertThat(AssociateEnterpriseIdController.class, isHandlerClass(COMMAND_CONTROLLER)
                .with(method("associateEnterpriseId")
                        .thatHandles("sjp.command.associate-enterprise-id")
                        .withSenderPassThrough()));
    }
}
