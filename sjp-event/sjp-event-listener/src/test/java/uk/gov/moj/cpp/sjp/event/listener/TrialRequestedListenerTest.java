package uk.gov.moj.cpp.sjp.event.listener;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.event.TrialRequested;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.entity.PendingDatesToAvoid;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.PendingDatesToAvoidRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class TrialRequestedListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private OnlinePleaRepository.TrialOnlinePleaRepository onlinePleaRepository;

    @Mock
    private PendingDatesToAvoidRepository pendingDatesToAvoidRepository;

    @InjectMocks
    private TrialRequestedListener trialRequestedListener;

    @Captor
    private ArgumentCaptor<OnlinePlea> onlinePleaCaptor;
    @Captor
    private ArgumentCaptor<PendingDatesToAvoid> pendingDatesToAvoidCaptor;

    private Clock clock = new UtcClock();
    private ZonedDateTime now = clock.now();

    @Test
    public void shouldSaveToAppropriateRepositoriesWhenTrialRequestedEvent() {
        final UUID caseId = UUID.randomUUID();
        final JsonEnvelope event = envelope()
                .withPayloadOf(caseId.toString(), "caseId")
                .build();
        final TrialRequested trialRequested = new TrialRequested(UUID.randomUUID(), "unavailability", "witnessDetails",
                "witnessDispute", now);
        when(jsonObjectConverter.convert(event.payloadAsJsonObject(), TrialRequested.class)).thenReturn(trialRequested);

        trialRequestedListener.updateTrial(event);

        verify(jsonObjectConverter).convert(event.payloadAsJsonObject(), TrialRequested.class);
        verify(onlinePleaRepository).saveOnlinePlea(onlinePleaCaptor.capture());
        verify(pendingDatesToAvoidRepository).save(pendingDatesToAvoidCaptor.capture());

        assertThat(onlinePleaCaptor.getValue().getCaseId(), equalTo(trialRequested.getCaseId()));
        assertThat(onlinePleaCaptor.getValue().getPleaDetails().getUnavailability(), equalTo(trialRequested.getUnavailability()));
        assertThat(onlinePleaCaptor.getValue().getPleaDetails().getWitnessDetails(), equalTo(trialRequested.getWitnessDetails()));
        assertThat(onlinePleaCaptor.getValue().getPleaDetails().getWitnessDispute(), equalTo(trialRequested.getWitnessDispute()));
        assertThat(onlinePleaCaptor.getValue().getSubmittedOn(), equalTo(trialRequested.getUpdatedDate()));
        assertThat(pendingDatesToAvoidCaptor.getValue().getCaseId(), equalTo(trialRequested.getCaseId()));
        assertThat(pendingDatesToAvoidCaptor.getValue().getPleaDate(), equalTo(trialRequested.getUpdatedDate()));
    }

}
