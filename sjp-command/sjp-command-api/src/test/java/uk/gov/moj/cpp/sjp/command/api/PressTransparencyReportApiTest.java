package uk.gov.moj.cpp.sjp.command.api;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getRequestPressTransparencyReportGroups;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
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
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PressTransparencyReportApiTest extends BaseDroolsAccessControlTest {

    public static final String SJP_REQUEST_PRESS_TRANSPARENCY_REPORT = "sjp.request-press-transparency-report";
    @Mock
    private Sender sender;

    @InjectMocks
    private PressTransparencyReportApi pressTransparencyReportApi;

    @Captor
    private ArgumentCaptor<Envelope> envelopeCaptor;

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Override
    protected Map<Class, Object> getProviderMocks() {
        return ImmutableMap.<Class, Object>builder().put(UserAndGroupProvider.class, userAndGroupProvider).build();
    }

    @Test
    public void shouldAllowAuthorisedUserToPressTransparencyReport() {
        final Action action = createActionFor(SJP_REQUEST_PRESS_TRANSPARENCY_REPORT);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getRequestPressTransparencyReportGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }


    @Test
    public void shouldRequestPressTransparencyReport() {
        final JsonEnvelope command = envelope().with(metadataWithRandomUUID(SJP_REQUEST_PRESS_TRANSPARENCY_REPORT)).build();

        pressTransparencyReportApi.requestTransparencyReport(command);
        verify(sender).send(envelopeCaptor.capture());

        final Envelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata().name(), is("sjp.command.request-press-transparency-report"));
        assertThat(newCommand.metadata().id(), is(command.metadata().id()));
    }
}