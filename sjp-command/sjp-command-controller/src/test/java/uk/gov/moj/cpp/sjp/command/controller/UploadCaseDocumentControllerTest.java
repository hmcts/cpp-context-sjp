package uk.gov.moj.cpp.sjp.command.controller;

import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.moj.cpp.sjp.command.controller.UploadCaseDocumentController;

import org.junit.Test;

public class UploadCaseDocumentControllerTest {

    @Test
    public void testAssociateEnterpriseIdHandlesExpectedActionAsPassThrough() {
        assertThat(UploadCaseDocumentController.class, isHandlerClass(COMMAND_CONTROLLER)
                .with(method("uploadCaseDocument")
                        .thatHandles("sjp.command.upload-case-document")
                        .withSenderPassThrough()));
    }
}
