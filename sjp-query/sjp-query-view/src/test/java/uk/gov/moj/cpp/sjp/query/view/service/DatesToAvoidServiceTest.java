package uk.gov.moj.cpp.sjp.query.view.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityAccess;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityProvider;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.persistence.entity.PendingDatesToAvoid;
import uk.gov.moj.cpp.sjp.persistence.repository.PendingDatesToAvoidRepository;
import uk.gov.moj.cpp.sjp.query.view.converter.ProsecutingAuthorityAccessFilterConverter;
import uk.gov.moj.cpp.sjp.query.view.response.DatesToAvoidsView;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DatesToAvoidServiceTest {

    @Mock
    private PendingDatesToAvoidRepository pendingDatesToAvoidRepository;

    @Mock
    private ProsecutingAuthorityProvider prosecutingAuthorityProvider;

    @Mock
    private ProsecutingAuthorityAccessFilterConverter prosecutingAuthorityAccessFilterConverter;

    @InjectMocks
    private DatesToAvoidService datesToAvoidService;

    @Test
    public void shouldReturnFinancialMeansIfExists() {
        final JsonEnvelope envelope = envelope()
                .with(metadataWithRandomUUID("sjp.pending-dates-to-avoid"))
                .build();
        final ProsecutingAuthorityAccess prosecutingAuthorityAccess = ProsecutingAuthorityAccess.of(ProsecutingAuthority.TFL.toString());
        final String prosecutingAuthorityFilterValue = ProsecutingAuthority.TFL.toString();
        final List<PendingDatesToAvoid> pendingDatesToAvoidList = Arrays.asList(new PendingDatesToAvoid(UUID.randomUUID()));

        when(prosecutingAuthorityProvider.getCurrentUsersProsecutingAuthorityAccess(envelope)).thenReturn(prosecutingAuthorityAccess);
        when(prosecutingAuthorityAccessFilterConverter.convertToProsecutingAuthorityAccessFilter(prosecutingAuthorityAccess)).thenReturn(prosecutingAuthorityFilterValue);
        when(pendingDatesToAvoidRepository.findCasesPendingDatesToAvoid(prosecutingAuthorityFilterValue)).thenReturn(pendingDatesToAvoidList);

        final DatesToAvoidsView datesToAvoidsView = datesToAvoidService.findCasesPendingDatesToAvoid(envelope);

        assertThat(datesToAvoidsView.getPendingDatesToAvoid().size(), is(1));
        assertThat(datesToAvoidsView.getPendingDatesToAvoid().get(0).getCaseId(), is(pendingDatesToAvoidList.get(0).getCaseId()));
        assertThat(datesToAvoidsView.getPendingDatesToAvoidCount(), is(1));
    }
}
