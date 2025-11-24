package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.event.processor.DatesToAvoidProcessor.DATES_TO_AVOID_ADDED_PUBLIC_EVENT_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.DatesToAvoidProcessor.DATES_TO_AVOID_UPDATED_PUBLIC_EVENT_NAME;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidRequired;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;
import uk.gov.moj.cpp.sjp.event.processor.service.timers.TimerService;

import java.util.UUID;

import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DatesToAvoidProcessorTest {

    @InjectMocks
    private DatesToAvoidProcessor processor;

    @Mock
    private Sender sender;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Captor
    private ArgumentCaptor<Envelope<JsonValue>> envelopeCaptor;

    @Mock
    private CaseStateService caseStateService;

    @Mock
    private TimerService timerService;

    @Test
    public void raisesDatesToAvoidAddedPublicEvent() {
        final UUID caseId = UUID.randomUUID();
        final String datesToAvoid = "Tue - Wed";
        final JsonEnvelope privateEvent = createEnvelope("sjp.events.dates-to-avoid-added",
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("datesToAvoid", datesToAvoid)
                        .build()
        );

        processor.publishDatesToAvoidAdded(privateEvent);
        raiseDatesToAvoidPublicEvent(caseId, datesToAvoid, privateEvent, DATES_TO_AVOID_ADDED_PUBLIC_EVENT_NAME);
        verify(caseStateService).datesToAvoidAdded(caseId, datesToAvoid, privateEvent.metadata());
    }

    @Test
    public void shouldStartDatesToAvoidExpirationTimer() {
        final UUID caseId = UUID.randomUUID();
        final String datesToAvoidExpirationDate = "2019-06-06";

        final JsonEnvelope datesToAvoidRequiredEvent = createEnvelope(DatesToAvoidRequired.EVENT_NAME,
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("datesToAvoidExpirationDate", datesToAvoidExpirationDate)
                        .build()
        );

        processor.datesToAvoidRequired(datesToAvoidRequiredEvent);

        verify(timerService).startTimerForDatesToAvoid(caseId, LocalDates.from(datesToAvoidExpirationDate), datesToAvoidRequiredEvent.metadata());
    }

    @Test
    public void raisesDatesToAvoidUpdatedPublicEvent() {
        final UUID caseId = UUID.randomUUID();
        final String datesToAvoid = "Tue - Wed";
        final JsonEnvelope privateEvent = createEnvelope("sjp.events.dates-to-avoid-updated",
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("datesToAvoid", datesToAvoid)
                        .build()
        );

        processor.publishDatesToAvoidUpdated(privateEvent);
        raiseDatesToAvoidPublicEvent(caseId, datesToAvoid, privateEvent, DATES_TO_AVOID_UPDATED_PUBLIC_EVENT_NAME);
    }

    private void raiseDatesToAvoidPublicEvent(final UUID caseId, final String datesToAvoid, final JsonEnvelope privateEvent, final String publicEventRaised) {
        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonValue> sentEnvelope = envelopeCaptor.getValue();

        assertThat(sentEnvelope.metadata(),
                withMetadataEnvelopedFrom(privateEvent)
                        .withName(publicEventRaised));

        assertThat(sentEnvelope.payload(),
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.datesToAvoid", equalTo(datesToAvoid)))));
    }
}