package uk.gov.moj.cpp.sjp.command.api;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.command.api.service.CaseService;

import java.time.ZonedDateTime;

import javax.json.JsonObjectBuilder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResolveConvictingCourtApiTest {

    private static final String SJP_RESOLVE_CONVICTION_COURT_BDF = "sjp.resolve-conviction-court-bdf";
    private static final String SAVE_APPLICATION_DECISION_COMMAND_NAME = "sjp.update-save-decision-bdf";

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @Mock
    private CaseService caseService;

    @Spy
    private Clock clock = new StoppedClock(ZonedDateTime.now());

    @InjectMocks
    private ResolveConvictionCourtApi resolveConvictionCourtApi;

    @Captor
    private ArgumentCaptor<Envelope> envelopeCaptor;

    @Test
    public void shouldHandleResolveConvictionCourtCommands() {
        assertThat(ResolveConvictionCourtApi.class, isHandlerClass(COMMAND_API)
                .with(method("resolveConvictionCourt").thatHandles(SJP_RESOLVE_CONVICTION_COURT_BDF))
        );
    }

    @Test
    public void shouldCallController() {
        final JsonObjectBuilder caseDecisionCommandBuilder = createObjectBuilder().add("caseId", randomUUID().toString());
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.command.resolve-conviction-court-bdf"), caseDecisionCommandBuilder.build());
        resolveConvictionCourtApi.resolveConvictionCourt(envelope);
        verify(sender).send(envelopeCaptor.capture());
    }

}
