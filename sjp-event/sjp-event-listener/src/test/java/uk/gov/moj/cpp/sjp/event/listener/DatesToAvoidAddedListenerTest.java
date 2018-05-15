package uk.gov.moj.cpp.sjp.event.listener;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.messaging.JsonEnvelope;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.PendingDatesToAvoidRepository;

@RunWith(MockitoJUnitRunner.class)
public class DatesToAvoidAddedListenerTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private PendingDatesToAvoidRepository pendingDatesToAvoidRepository;

    @InjectMocks
    private DatesToAvoidAddedListener datesToAvoidAddedListener;

    @Test
    public void shouldAddDatesToAvoid() {
        final UUID caseId = UUID.randomUUID();
        final String datesToAvoid = "Away first two weeks of July 2018";
        final JsonEnvelope event = envelope()
                .withPayloadOf(caseId.toString(), "caseId")
                .withPayloadOf(datesToAvoid, "datesToAvoid")
                .build();

        datesToAvoidAddedListener.addDatesToAvoid(event);

        verify(caseRepository).updateDatesToAvoid(caseId, datesToAvoid);
        verify(pendingDatesToAvoidRepository).removeByCaseId(caseId);

    }
}