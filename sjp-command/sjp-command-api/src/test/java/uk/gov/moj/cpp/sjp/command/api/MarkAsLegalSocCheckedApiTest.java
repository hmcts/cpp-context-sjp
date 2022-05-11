package uk.gov.moj.cpp.sjp.command.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

@RunWith(MockitoJUnitRunner.class)
public class MarkAsLegalSocCheckedApiTest {

    private static final String COMMAND_NAME = "sjp.mark-as-legal-soc-checked";
    private static final String NEW_COMMAND_NAME = "sjp.command.mark-as-legal-soc-checked";

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @InjectMocks
    private MarkAsLegalSocCheckedApi markAsLegalSocCheckedApi;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void shouldHandleCommand() {
        assertThat(MarkAsLegalSocCheckedApi.class, isHandlerClass(COMMAND_API)
                .with(method("markAsLegalSocChecked").thatHandles(COMMAND_NAME)));
    }

//    @Test
//    public void shouldRenameCommand() {
//        final JsonEnvelope command = envelope().with(metadataWithRandomUUID(COMMAND_NAME)).build();
//
//        markCaseLegalSocCheckedApi.markCaseLegalSocChecked(command);
//
//        verify(sender).send(envelopeCaptor.capture());
//
//        final JsonEnvelope newCommand = envelopeCaptor.getValue();
//        assertThat(newCommand.metadata(), withMetadataEnvelopedFrom(command).withName(NEW_COMMAND_NAME));
//        assertThat(newCommand.payloadAsJsonObject(), equalTo(command.payloadAsJsonObject()));
//    }

}
