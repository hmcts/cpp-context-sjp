package uk.gov.moj.cpp.sjp.event.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.event.listener.converter.FinancialMeansConverter;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialMeans;
import uk.gov.moj.cpp.sjp.persistence.repository.FinancialMeansRepository;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class FinancialMeansListenerTest {

    @Mock
    private JsonEnvelope eventEnvelope;

    @Mock
    private JsonObject payload;

    @Mock
    private FinancialMeans financialMeans;

    @Mock
    private FinancialMeansUpdated financialMeansUpdated;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private FinancialMeansRepository financialMeansRepository;

    @Mock
    private FinancialMeansConverter financialMeansConverter;

    @InjectMocks
    private FinancialMeansListener financialMeansListener = new FinancialMeansListener();

    @Test
    public void shouldSaveFinancialMeansUpdatedEvent() {
        when(eventEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectConverter.convert(payload, FinancialMeansUpdated.class)).thenReturn(financialMeansUpdated);
        when(financialMeansConverter.convertToFinancialMeansEntity(financialMeansUpdated)).thenReturn(financialMeans);

        financialMeansListener.updateFinancialMeans(eventEnvelope);

        verify(financialMeansRepository).save(financialMeans);
    }

}
