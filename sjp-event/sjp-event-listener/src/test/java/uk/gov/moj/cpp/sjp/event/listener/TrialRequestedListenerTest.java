package uk.gov.moj.cpp.sjp.event.listener;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.TrialRequested;
import uk.gov.moj.cpp.sjp.event.listener.converter.OnlinePleaConverter;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class TrialRequestedListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private OnlinePleaConverter onlinePleaConverter;

    @Mock
    private OnlinePleaRepository.TrialOnlinePleaRepository onlinePleaRepository;

    @Mock
    private TrialRequested trialRequested;

    @Mock
    private OnlinePlea onlinePlea;

    @InjectMocks
    private TrialRequestedListener trialRequestedListener = new TrialRequestedListener();

    @Test
    public void shouldSaveFinancialMeansUpdatedEvent() {
        final UUID caseId = UUID.randomUUID();
        final JsonEnvelope event = envelope()
                .withPayloadOf(caseId.toString(), "caseId")
                .build();
        when(jsonObjectConverter.convert(event.payloadAsJsonObject(), TrialRequested.class)).thenReturn(trialRequested);
        when(onlinePleaConverter.convertToOnlinePleaEntity(trialRequested)).thenReturn(onlinePlea);

        trialRequestedListener.updateTrial(event);

        verify(jsonObjectConverter).convert(event.payloadAsJsonObject(), TrialRequested.class);
        verify(onlinePleaConverter).convertToOnlinePleaEntity(trialRequested);
        verify(onlinePleaRepository).saveOnlinePlea(eq(onlinePlea));
    }

}
