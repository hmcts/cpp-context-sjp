package uk.gov.moj.cpp.sjp.query.view.service;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.sjp.persistence.entity.FinancialMeans;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.repository.FinancialMeansRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;
import uk.gov.moj.cpp.sjp.query.view.converter.FinancialMeansConverter;

import java.util.Optional;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FinancialMeansServiceTest {

    @Mock
    private FinancialMeansConverter financialMeansConverter;

    @Mock
    private FinancialMeans financialMeansEntity;

    @Mock
    private OnlinePlea onlinePleaEntity;

    @Mock
    private uk.gov.moj.cpp.sjp.domain.FinancialMeans financialMeans;

    @Mock
    private FinancialMeansRepository financialMeansRepository;

    @Mock
    private OnlinePleaRepository.LegalEntityDetailsOnlinePleaRepository onlinePleaRepository;

    @InjectMocks
    private FinancialMeansService financialMeansService;

    @Test
    public void shouldReturnFinancialMeansIfExists() {

        final UUID defendantId = UUID.randomUUID();

        when(financialMeansRepository.findBy(defendantId)).thenReturn(financialMeansEntity);
        when(onlinePleaRepository.findBy(defendantId)).thenReturn(onlinePleaEntity);
        when(financialMeansConverter.convertToFinancialMeans(financialMeansEntity, onlinePleaEntity)).thenReturn(financialMeans);

        final Optional<uk.gov.moj.cpp.sjp.domain.FinancialMeans> actualFinancialMeans = financialMeansService.getFinancialMeans(defendantId);

        assertThat(actualFinancialMeans.isPresent(), is(true));
        assertThat(actualFinancialMeans.get(), equalTo(financialMeans));
    }

    @Test
    public void shouldReturnEmptyFinancialMeansIfNotExists() {
        final UUID defendantId = UUID.randomUUID();

        when(financialMeansRepository.findBy(defendantId)).thenReturn(null);

        final Optional<uk.gov.moj.cpp.sjp.domain.FinancialMeans> actualFinancialMeans = financialMeansService.getFinancialMeans(defendantId);

        assertThat(actualFinancialMeans.isPresent(), is(false));
    }

}
