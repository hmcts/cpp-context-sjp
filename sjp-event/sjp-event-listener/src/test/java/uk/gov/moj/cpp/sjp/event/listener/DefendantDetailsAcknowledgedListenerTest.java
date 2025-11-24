package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.ZoneOffset.UTC;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdatesAcknowledged;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantDetailsAcknowledgedListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private CaseRepository caseRepository;

    @InjectMocks
    private DefendantDetailsAcknowledgedListener defendantDetailsAcknowledgedListener;

    @Test
    public void shouldMarkDefendantDetailsUpdatesAsAcknowledged() {
        ZonedDateTime acknowledgedAt = ZonedDateTime.now(UTC);

        UUID caseId = UUID.randomUUID();
        UUID defendantId = UUID.randomUUID();

        JsonObject eventPayload = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("defendantId", defendantId.toString())
                .add("acknowledgedAt", acknowledgedAt.format(DateTimeFormatter.ISO_DATE_TIME))
                .build();
        JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID(DefendantDetailsUpdatesAcknowledged.EVENT_NAME),
                eventPayload);

        final CaseDetail caseDetailMock = mock(CaseDetail.class);

        when(jsonObjectToObjectConverter.convert(eventPayload, DefendantDetailsUpdatesAcknowledged.class))
                .thenReturn(new DefendantDetailsUpdatesAcknowledged(caseId, defendantId, acknowledgedAt));
        when(caseRepository.findBy(caseId)).thenReturn(caseDetailMock);

        defendantDetailsAcknowledgedListener.defendantDetailsUpdatesAcknowledged(event);

        verify(caseDetailMock).acknowledgeDefendantDetailsUpdates(acknowledgedAt);
    }
}
