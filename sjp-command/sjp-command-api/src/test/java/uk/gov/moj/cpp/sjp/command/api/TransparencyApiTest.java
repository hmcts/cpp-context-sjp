package uk.gov.moj.cpp.sjp.command.api;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getRequestTransparencyReportGroups;
import static uk.gov.moj.cpp.sjp.domain.DocumentFormat.PDF;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TransparencyApiTest extends BaseDroolsAccessControlTest {

    public static final String SJP_REQUEST_TRANSPARENCY_REPORT = "sjp.request-transparency-report";
    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @InjectMocks
    private TransparencyApi transparencyApi;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
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