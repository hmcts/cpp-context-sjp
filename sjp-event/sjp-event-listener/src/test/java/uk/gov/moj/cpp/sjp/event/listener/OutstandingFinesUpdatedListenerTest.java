package uk.gov.moj.cpp.sjp.event.listener;

import static java.lang.Boolean.TRUE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.OutstandingFinesUpdated;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OutstandingFinesUpdatedListenerTest {
    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private OnlinePleaRepository.OutstandingFinesOnlinePleaRepository onlinePleaRepository;

    @InjectMocks
    private OutstandingFinesUpdatedListener outstandingFinesUpdatedListener;

    @Captor
    private ArgumentCaptor<OnlinePlea> onlinePleaCaptor;

    private Clock clock = new UtcClock();
    private ZonedDateTime now = clock.now();

    @Test
    public void shouldSaveToAppropriateRepositoriesWhenOutstandingFinesUpdatedEvent() {
        final UUID caseId = UUID.randomUUID();
        final JsonEnvelope event = envelope()
                .withPayloadOf(caseId.toString(), "caseId")
                .build();
        final OutstandingFinesUpdated outstandingFinesUpdated = new OutstandingFinesUpdated(UUID.randomUUID(), TRUE, now);
        when(jsonObjectConverter.convert(event.payloadAsJsonObject(), OutstandingFinesUpdated.class)).thenReturn(outstandingFinesUpdated);

        outstandingFinesUpdatedListener.updateOutstandingFines(event);

        verify(jsonObjectConverter).convert(event.payloadAsJsonObject(), OutstandingFinesUpdated.class);
        verify(onlinePleaRepository).saveOnlinePlea(onlinePleaCaptor.capture());

        assertThat(onlinePleaCaptor.getValue().getCaseId(), equalTo(outstandingFinesUpdated.getCaseId()));
        assertThat(onlinePleaCaptor.getValue().getPleaDetails().getOutstandingFines(), equalTo(outstandingFinesUpdated.getOutstandingFines()));
    }
}
