package uk.gov.moj.cpp.sjp.event.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OnlinePleaReceivedListenerTest {

    @Mock
    private CaseRepository caseRepository;

    @InjectMocks
    private OnlinePleaReceivedListener onlinePleaReceivedListener;

    @Test
    public void shouldUpdatePlea() {
        final UUID caseId = UUID.randomUUID();
        final JsonEnvelope event = envelope()
                .withPayloadOf(caseId.toString(), "caseId")
                .build();
        final CaseDetail caseDetail = new CaseDetail();
        caseDetail.setId(caseId);

        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);

        onlinePleaReceivedListener.onlinePleaReceived(event);

        verify(caseRepository).findBy(caseId);
        verify(caseRepository).save(caseDetail);
    }
}
