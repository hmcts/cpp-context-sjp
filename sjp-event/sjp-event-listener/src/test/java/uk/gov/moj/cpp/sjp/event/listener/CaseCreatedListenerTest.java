package uk.gov.moj.cpp.sjp.event.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.SjpCaseCreated;
import uk.gov.moj.cpp.sjp.event.listener.converter.SjpCaseCreatedToCase;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseCreatedListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    @SuppressWarnings("deprecation")
    private SjpCaseCreatedToCase sjpConverter;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private SjpCaseCreated sjpEvent;

    @Mock
    private CaseDetail caseDetail;

    @Mock
    private JsonObject payload;

    @InjectMocks
    private CaseCreatedListener listener;

    @Test
    @SuppressWarnings("deprecation")
    public void shouldHandleCreateSjpCaseEvent() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, SjpCaseCreated.class)).thenReturn(sjpEvent);
        when(sjpConverter.convert(sjpEvent)).thenReturn(caseDetail);

        listener.sjpCaseCreated(envelope);

        verify(caseRepository).save(caseDetail);
    }

}