package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProsecutionCasesReferredToCourtProcessorTest {

    private final UUID caseId = randomUUID();

    @InjectMocks
    private ProsecutionCasesReferredToCourtProcessor prosecutionCasesReferredToCourtProcessor;

    @Mock
    private Sender sender;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void shouldSendUpdateCaseListedInCriminalCourtsCommand() {
        final JsonEnvelope prosecutionCasesReferredToCourtEvent = envelopeFrom(metadataWithRandomUUID(ProsecutionCasesReferredToCourtProcessor.EVENT_NAME), createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .build());

        prosecutionCasesReferredToCourtProcessor.handleProsecutionCasesReferredToCourtEvent(prosecutionCasesReferredToCourtEvent);

        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(),
                is("sjp.command.update-case-listed-in-criminal-courts")
        );
        assertThat(envelopeCaptor.getValue().payloadAsJsonObject(),
                is(createObjectBuilder()
                        .add(CASE_ID, caseId.toString())
                        .build()
                )
        );
    }

    @Test
    public void shouldHandleProsecutionCasesReferredToCourtEventMessage() {
        assertThat(ProsecutionCasesReferredToCourtProcessor.class, isHandlerClass(EVENT_PROCESSOR).
                with(method("handleProsecutionCasesReferredToCourtEvent").
                        thatHandles(ProsecutionCasesReferredToCourtProcessor.EVENT_NAME)));
    }
}
