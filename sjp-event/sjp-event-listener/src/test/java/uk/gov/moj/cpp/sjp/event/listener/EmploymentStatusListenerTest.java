package uk.gov.moj.cpp.sjp.event.listener;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialMeans;
import uk.gov.moj.cpp.sjp.persistence.repository.FinancialMeansRepository;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class EmploymentStatusListenerTest {

    @Mock
    private FinancialMeansRepository financialMeansRepository;

    @InjectMocks
    private EmploymentStatusListener employmentStatusListener = new EmploymentStatusListener();

    @Captor
    private ArgumentCaptor<FinancialMeans> financialMeansCaptor;

    private UUID defendantId;
    private String employmentStatus;
    private JsonEnvelope updateEmploymentStatus;

    @Before
    public void init() {
        defendantId = UUID.randomUUID();
        employmentStatus = "EMPLOYED";
        updateEmploymentStatus = envelope().with(metadataWithRandomUUID("sjp.events.employment-status-updated"))
                .withPayloadOf(defendantId.toString(), "defendantId")
                .withPayloadOf(employmentStatus, "employmentStatus")
                .build();

    }

    @Test
    public void shouldUpdateFinancialMeansWithEmploymentStatus() {
        final FinancialMeans existingFinancialMeans = new FinancialMeans();

        when(financialMeansRepository.findBy(defendantId)).thenReturn(existingFinancialMeans);

        employmentStatusListener.updateEmploymentStatus(updateEmploymentStatus);

        assertThat(existingFinancialMeans.getEmploymentStatus(), equalTo(employmentStatus));
        verify(financialMeansRepository).save(existingFinancialMeans);
    }

    @Test
    public void shouldCreateFinancialMeansWithEmploymentStatus() {
        when(financialMeansRepository.findBy(defendantId)).thenReturn(null);

        employmentStatusListener.updateEmploymentStatus(updateEmploymentStatus);

        verify(financialMeansRepository).save(financialMeansCaptor.capture());
        final FinancialMeans financialMeans = financialMeansCaptor.getValue();

        assertThat(financialMeans.getDefendantId(), equalTo(defendantId));
        assertThat(financialMeans.getEmploymentStatus(), equalTo(employmentStatus));
    }

}
