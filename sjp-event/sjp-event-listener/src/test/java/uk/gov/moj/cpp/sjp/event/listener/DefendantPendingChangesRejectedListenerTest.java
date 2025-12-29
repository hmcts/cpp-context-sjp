package uk.gov.moj.cpp.sjp.event.listener;


import static java.time.ZoneOffset.UTC;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.DefendantPendingChangesRejected;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetailUpdateRequest;
import uk.gov.moj.cpp.sjp.persistence.repository.DefendantDetailUpdateRequestRepository;

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
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantPendingChangesRejectedListenerTest {
    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Mock
    private DefendantDetailUpdateRequestRepository defendantDetailUpdateRequestRepository;
    @InjectMocks
    private DefendantPendingChangesRejectedListener defendantPendingChangesRejectedListener;
    @Captor
    private ArgumentCaptor<DefendantDetailUpdateRequest> captor;

    @Test
    public void shouldMarkDefendantDetailUpdateRequestAsUpdated() {
        ZonedDateTime rejectedAt = ZonedDateTime.now(UTC);

        UUID caseId = UUID.randomUUID();
        UUID defendantId = UUID.randomUUID();
        DefendantDetailUpdateRequest detailUpdateRequest = new DefendantDetailUpdateRequest.Builder()
                .withCaseId(caseId)
                .withStatus(DefendantDetailUpdateRequest.Status.PENDING)
                .withFirstName("firstName")
                .withLastName("lastName")
                .withUpdatedAt(rejectedAt)
                .build();

        JsonObject eventPayload = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("defendantId", defendantId.toString())
                .add("description", "Defendant update rejected")
                .add("rejectedAt", rejectedAt.format(DateTimeFormatter.ISO_DATE_TIME))
                .build();
        JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("sjp.events.defendant-pending-changes-rejected"),
                eventPayload);

        when(jsonObjectToObjectConverter.convert(eventPayload, DefendantPendingChangesRejected.class))
                .thenReturn(new DefendantPendingChangesRejected(caseId, defendantId, "Defendant update rejected", rejectedAt));
        when(defendantDetailUpdateRequestRepository.findBy(caseId)).thenReturn(detailUpdateRequest);

        defendantPendingChangesRejectedListener.defendantPendingChangesRejected(event);

        verify(defendantDetailUpdateRequestRepository, times(1)).save(captor.capture());

        final DefendantDetailUpdateRequest request = captor.getValue();
        assertThat(request.getStatus(), equalTo(DefendantDetailUpdateRequest.Status.REJECTED));
    }
}
