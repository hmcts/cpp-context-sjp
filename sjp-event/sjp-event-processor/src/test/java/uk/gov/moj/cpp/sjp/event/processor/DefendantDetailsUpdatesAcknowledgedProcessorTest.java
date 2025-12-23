package uk.gov.moj.cpp.sjp.event.processor;

import static java.time.ZoneOffset.UTC;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.sjp.event.processor.DefendantDetailsUpdatesAcknowledgedProcessor.PUBLIC_SJP_EVENTS_DEFENDANT_DETAILS_UPDATES_ACKNOWLEDGED;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdatesAcknowledged;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantDetailsUpdatesAcknowledgedProcessorTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @InjectMocks
    private DefendantDetailsUpdatesAcknowledgedProcessor defendantDetailsUpdatesAcknowledgedProcessor;

    @Test
    public void shouldPublishPublicEvent() {
        String caseId = UUID.randomUUID().toString();
        String defendantId = UUID.randomUUID().toString();

        JsonObject eventPayload = createObjectBuilder()
                .add("caseId", caseId)
                .add("defendantId", defendantId)
                .add("acknowledgedAt", ZonedDateTime.now(UTC).format(DateTimeFormatter.ISO_DATE_TIME))
                .build();

        final JsonEnvelope eventEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID(DefendantDetailsUpdatesAcknowledged.EVENT_NAME),
                eventPayload);

        defendantDetailsUpdatesAcknowledgedProcessor.publish(eventEnvelope);

        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is(PUBLIC_SJP_EVENTS_DEFENDANT_DETAILS_UPDATES_ACKNOWLEDGED));
        assertThat(
                envelopeCaptor.getValue().payloadAsJsonObject(),
                is(createObjectBuilder()
                        .add("caseId", caseId)
                        .add("defendantId", defendantId)
                        .build()));
    }
}
