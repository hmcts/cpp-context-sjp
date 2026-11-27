package uk.gov.moj.cpp.sjp.command.api;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.DocumentFormat.PDF;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TransparencyApiTest extends BaseDroolsAccessControlTest {

    public static final String SJP_REQUEST_TRANSPARENCY_REPORT = "sjp.request-transparency-report";
    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @InjectMocks
    private TransparencyApi transparencyApi;

    @Captor
    private ArgumentCaptor<Envelope> envelopeCaptor;

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public TransparencyApiTest() {
        super("COMMAND_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @Test
    public void shouldRequestTransparencyReport() {
        final JsonEnvelope command = envelopeFrom(
                metadataWithRandomUUID(SJP_REQUEST_TRANSPARENCY_REPORT),
                createObjectBuilder()
                        .add("format", PDF.name()));
        transparencyApi.requestTransparencyReport(command);
        verify(sender).send(envelopeCaptor.capture());

        final Envelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), is("sjp.command.request-transparency-report"));
        assertThat(newCommand.metadata().id(), is(command.metadata().id()));
    }
}