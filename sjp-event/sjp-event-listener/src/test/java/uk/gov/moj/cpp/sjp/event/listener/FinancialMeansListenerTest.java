package uk.gov.moj.cpp.sjp.event.listener;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.domain.IncomeFrequency;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.event.listener.converter.FinancialMeansConverter;
import uk.gov.moj.cpp.sjp.event.listener.converter.OnlinePleaConverter;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialMeans;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.repository.DefendantRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.FinancialMeansRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
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

    @InjectMocks
    private FinancialMeansListener financialMeansListener = new FinancialMeansListener();

    private Clock clock = new UtcClock();
    private ZonedDateTime now = clock.now();

    private UUID caseId;
    
    private UUID defendantId;

    @Before
    public void init() {
        caseId = UUID.randomUUID();
        defendantId = UUID.randomUUID();
    }

    @Test
    public void shouldSaveFinancialMeansUpdatedEvent() {
        final FinancialMeansUpdated financialMeansUpdated = FinancialMeansUpdated.createEvent(defendantId,
                new Income(IncomeFrequency.WEEKLY, BigDecimal.valueOf(1000)), new Benefits(), "EMPLOYED");
        setMocks(financialMeansUpdated);

        financialMeansListener.updateFinancialMeans(eventEnvelope);

        verify(financialMeansRepository).save(financialMeans);
        verify(jsonObjectConverter).convert(payload, FinancialMeansUpdated.class);

        verifyZeroInteractions(defendantRepository);
        verifyZeroInteractions(onlinePleaRepository);
        verifyZeroInteractions(onlinePleaConverter);
    }

    @Test
    public void shouldSaveFinancialMeansUpdatedEventAndSaveOnlinePlea() {
        final FinancialMeansUpdated financialMeansUpdated = FinancialMeansUpdated.createEventForOnlinePlea(defendantId,
                new Income(IncomeFrequency.WEEKLY, BigDecimal.valueOf(1000)), new Benefits(), "EMPLOYED", null, now);
        setMocks(financialMeansUpdated);

        financialMeansListener.updateFinancialMeans(eventEnvelope);

        verify(financialMeansRepository).save(financialMeans);
        verify(jsonObjectConverter).convert(payload, FinancialMeansUpdated.class);

        verify(defendantRepository).findCaseIdByDefendantId(defendantId);
        verify(onlinePleaConverter).convertToOnlinePleaEntity(caseId, financialMeansUpdated);
        verify(onlinePleaRepository).saveOnlinePlea(eq(onlinePlea));
    }

    private void setMocks(final FinancialMeansUpdated financialMeansUpdated) {
        when(financialMeansConverter.convertToFinancialMeansEntity(financialMeansUpdated)).thenReturn(financialMeans);
        when(defendantRepository.findCaseIdByDefendantId(defendantId)).thenReturn(caseId);
        when(onlinePleaConverter.convertToOnlinePleaEntity(caseId, financialMeansUpdated)).thenReturn(onlinePlea);
        when(eventEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectConverter.convert(payload, FinancialMeansUpdated.class)).thenReturn(financialMeansUpdated);
    }

}
