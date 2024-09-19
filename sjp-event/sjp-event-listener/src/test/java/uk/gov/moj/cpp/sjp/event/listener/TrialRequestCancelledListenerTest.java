package uk.gov.moj.cpp.sjp.event.listener;

import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.repository.PendingDatesToAvoidRepository;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class TrialRequestCancelledListenerTest {

    @Mock
    private PendingDatesToAvoidRepository pendingDatesToAvoidRepository;

    @InjectMocks
    private TrialRequestCancelledListener trialRequestCancelledListener;

    @Test
    public void shouldRemoveFromDatesToAvoidWhenTrialRequestCancelledEvent() {
        final UUID caseId = UUID.randomUUID();
        final JsonEnvelope event = envelope()
                .withPayloadOf(caseId.toString(), "caseId")
                .build();

        trialRequestCancelledListener.cancelTrial(event);

        verify(pendingDatesToAvoidRepository).removeByCaseId(caseId);
    }

}
