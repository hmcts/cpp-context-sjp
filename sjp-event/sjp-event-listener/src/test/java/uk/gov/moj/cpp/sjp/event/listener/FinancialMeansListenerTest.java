package uk.gov.moj.cpp.sjp.event.listener;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
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
import uk.gov.moj.cpp.sjp.event.FinancialMeansDeleted;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.event.listener.converter.FinancialMeansConverter;
import uk.gov.moj.cpp.sjp.event.listener.converter.OnlinePleaConverter;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialMeans;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseDocumentRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.DefendantRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.FinancialMeansRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @Mock
    private CaseDocumentRepository caseDocumentRepository;

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

    @Test
    public void shouldSaveFinancialMeansDeletedEventAndDeleteFinancialMeansInformationData() {

        final UUID defendantId = UUID.randomUUID();
        final FinancialMeansDeleted financialMeansDeleted = new FinancialMeansDeleted(defendantId, addCaseDocuments());

        setMocks(financialMeansDeleted);

        financialMeansListener.deleteFinancialMeans(eventEnvelope);

        verify(jsonObjectConverter).convert(payload, FinancialMeansDeleted.class);
        verify(eventEnvelope).payloadAsJsonObject();
        verify(financialMeansRepository).findBy(defendantId);
        verify(financialMeansRepository).remove(anyObject());
        verify(defendantRepository).findCaseIdByDefendantId(defendantId);
        verify(onlinePleaRepository).findOnlinePleaByDefendantIdAndCaseId(anyObject(),
                anyObject());
        verify(onlinePleaRepository).save(anyObject());
        verify(caseDocumentRepository).findByMaterialId(financialMeansDeleted.getMaterialIds().get(0));
        verify(caseDocumentRepository).remove(anyObject());
    }

    private List<UUID> addCaseDocuments() {
        final UUID materialId = UUID.randomUUID();
        List<UUID> materialIds = new ArrayList<>();
        materialIds.add(materialId);
        return materialIds;
    }

    private void setMocks(final FinancialMeansDeleted financialMeansDeleted) {
        when(jsonObjectConverter.convert(payload, FinancialMeansDeleted.class)).thenReturn(financialMeansDeleted);
        when(eventEnvelope.payloadAsJsonObject()).thenReturn(payload);
        final FinancialMeans financialMeans = new FinancialMeans(financialMeansDeleted.getDefendantId(), "SELF-EMPLOYED");
        when(financialMeansRepository.findBy(financialMeans.getDefendantId())).thenReturn(financialMeans);
        doNothing().when(financialMeansRepository).remove(financialMeans);
        final UUID caseId = UUID.randomUUID();
        when(defendantRepository.findCaseIdByDefendantId(financialMeansDeleted.getDefendantId())).thenReturn(caseId);
        final OnlinePlea onlinePlea = new OnlinePlea();
        when(onlinePleaRepository.findOnlinePleaByDefendantIdAndCaseId(caseId, financialMeansDeleted.getDefendantId())).thenReturn(onlinePlea);
        when(onlinePleaRepository.save(onlinePlea)).thenReturn(onlinePlea);
        uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument caseDocumentEntity = new uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument();
        when(caseDocumentRepository.findByMaterialId(financialMeansDeleted.getMaterialIds().get(0))).thenReturn(caseDocumentEntity);
        doNothing().when(caseDocumentRepository).remove(caseDocumentEntity);
    }

}
