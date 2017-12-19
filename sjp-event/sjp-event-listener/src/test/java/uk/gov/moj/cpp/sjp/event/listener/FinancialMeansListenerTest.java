package uk.gov.moj.cpp.sjp.event.listener;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.domain.IncomeFrequency;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.event.listener.converter.FinancialMeansConverter;
import uk.gov.moj.cpp.sjp.event.listener.converter.OnlinePleaConverter;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialMeans;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.repository.DefendantRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.FinancialMeansRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

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
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private FinancialMeansRepository financialMeansRepository;

    @Mock
    private FinancialMeansConverter financialMeansConverter;

    @Mock
    private OnlinePleaConverter onlinePleaConverter;

    @Mock
    private OnlinePleaRepository.FinancialMeansOnlinePleaRepository onlinePleaRepository;

    @Mock
    private OnlinePlea onlinePlea;

    @Mock
    private DefendantRepository defendantRepository;

    @Mock
    private DefendantDetail defendantDetail;

    @InjectMocks
    private FinancialMeansListener financialMeansListener = new FinancialMeansListener();

    private Clock clock = new StoppedClock(ZonedDateTime.now());
    private ZonedDateTime now = clock.now();

    @Test
    public void shouldSaveFinancialMeansUpdatedEvent() {
        final FinancialMeansUpdated financialMeansUpdated = FinancialMeansUpdated.createEvent(UUID.randomUUID(),
                new Income(IncomeFrequency.WEEKLY, BigDecimal.valueOf(1000)), new Benefits(), "EMPLOYED");
        when(eventEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectConverter.convert(payload, FinancialMeansUpdated.class)).thenReturn(financialMeansUpdated);
        when(financialMeansConverter.convertToFinancialMeansEntity(financialMeansUpdated)).thenReturn(financialMeans);
        when(defendantRepository.findBy(financialMeansUpdated.getDefendantId())).thenReturn(defendantDetail);
        when(onlinePleaConverter.convertToOnlinePleaEntity(defendantDetail, financialMeansUpdated)).thenReturn(onlinePlea);

        financialMeansListener.updateFinancialMeans(eventEnvelope);

        verify(financialMeansRepository).save(financialMeans);
        verify(jsonObjectConverter).convert(payload, FinancialMeansUpdated.class);
        verify(defendantRepository, never()).findBy(financialMeansUpdated.getDefendantId());
        verify(onlinePleaConverter, never()).convertToOnlinePleaEntity(defendantDetail, financialMeansUpdated);
        verify(onlinePleaRepository, never()).saveOnlinePlea(eq(onlinePlea));
    }

    @Test
    public void shouldSaveFinancialMeansUpdatedEventAndSaveOnlinePlea() {
        final FinancialMeansUpdated financialMeansUpdated = FinancialMeansUpdated.createEventForOnlinePlea(UUID.randomUUID(),
                new Income(IncomeFrequency.WEEKLY, BigDecimal.valueOf(1000)), new Benefits(), "EMPLOYED", null, now);
        when(eventEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectConverter.convert(payload, FinancialMeansUpdated.class)).thenReturn(financialMeansUpdated);
        when(financialMeansConverter.convertToFinancialMeansEntity(financialMeansUpdated)).thenReturn(financialMeans);
        when(defendantRepository.findBy(financialMeansUpdated.getDefendantId())).thenReturn(defendantDetail);
        when(onlinePleaConverter.convertToOnlinePleaEntity(defendantDetail, financialMeansUpdated)).thenReturn(onlinePlea);

        financialMeansListener.updateFinancialMeans(eventEnvelope);

        verify(financialMeansRepository).save(financialMeans);
        verify(jsonObjectConverter).convert(payload, FinancialMeansUpdated.class);
        verify(defendantRepository).findBy(financialMeansUpdated.getDefendantId());
        verify(onlinePleaConverter).convertToOnlinePleaEntity(defendantDetail, financialMeansUpdated);
        verify(onlinePleaRepository).saveOnlinePlea(eq(onlinePlea));
    }

}
