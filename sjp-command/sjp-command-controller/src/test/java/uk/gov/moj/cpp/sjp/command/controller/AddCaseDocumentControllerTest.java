package uk.gov.moj.cpp.sjp.command.controller;

import static org.mockito.Mockito.verify;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.command.controller.AddCaseDocumentController;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AddCaseDocumentControllerTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope command;

    @InjectMocks
    private AddCaseDocumentController controller;

    @Test
    public void shouldAddCaseDocument() {
        controller.addCaseDocument(command);
        verify(sender).send(command);
    }
}
