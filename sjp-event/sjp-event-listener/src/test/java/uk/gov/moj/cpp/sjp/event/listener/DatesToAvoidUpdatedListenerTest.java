package uk.gov.moj.cpp.sjp.event.listener;

import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DatesToAvoidUpdatedListenerTest {

    @Mock
    private CaseRepository caseRepository;

    @InjectMocks
    private DatesToAvoidUpdatedListener datesToAvoidUpdatedListener;

    @Test
    public void shouldAddDatesToAvoid() {
        final UUID caseId = UUID.randomUUID();
        final String datesToAvoid = "Away first two weeks of July 2018";
        final JsonEnvelope event = envelope()
                .withPayloadOf(caseId.toString(), "caseId")
                .withPayloadOf(datesToAvoid, "datesToAvoid")
                .build();

        datesToAvoidUpdatedListener.updateDatesToAvoid(event);

        verify(caseRepository).updateDatesToAvoid(caseId, datesToAvoid);
    }
}