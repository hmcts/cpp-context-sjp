package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.ZonedDateTime.*;
import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static org.mockito.Mockito.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.event.CaseReserved;
import uk.gov.moj.cpp.sjp.event.CaseUnReserved;
import uk.gov.moj.cpp.sjp.persistence.entity.ReserveCase;
import uk.gov.moj.cpp.sjp.persistence.repository.ReserveCaseRepository;

@ExtendWith(MockitoExtension.class)
public class CaseReservedListenerTest {

    @Mock
    private ReserveCaseRepository reserveCaseRepository;

    @InjectMocks
    private CaseReservedListener caseReservedListener;

    @Captor
    private ArgumentCaptor<ReserveCase> reserveCaseArgumentCaptor;

    @Test
    public void shouldSaveReservedCase(){
        final CaseReserved caseReserved = new CaseReserved(randomUUID(), "CASEURN", now(), randomUUID());
        final Envelope<CaseReserved> caseReservedEnvelope = envelopeFrom(metadataWithRandomUUID("sjp.events.case-reserved"), caseReserved);

        caseReservedListener.handleCaseReserved(caseReservedEnvelope);

        verify(reserveCaseRepository).save(reserveCaseArgumentCaptor.capture());

        final ReserveCase savedCaseReserved = reserveCaseArgumentCaptor.getValue();

        assertThat(caseReserved.getCaseId(), is(savedCaseReserved.getCaseId()));
        assertThat(caseReserved.getCaseUrn(), is(savedCaseReserved.getCaseUrn()));
        assertThat(caseReserved.getReservedBy(), is(savedCaseReserved.getReservedBy()));
        assertThat(caseReserved.getReservedAt(), is(savedCaseReserved.getReservedAt()));
    }

    @Test
    public void shouldRemoveReservedCase(){
        final CaseUnReserved caseUnReserved = new CaseUnReserved(randomUUID(), "CASEURN", randomUUID());
        final Envelope<CaseUnReserved> caseReservedEnvelope = envelopeFrom(metadataWithRandomUUID("sjp.events.case-unreserved"), caseUnReserved);

        when(reserveCaseRepository.findByCaseId(caseUnReserved.getCaseId())).thenReturn(Collections.singletonList(new ReserveCase(caseUnReserved.getCaseId(), caseUnReserved.getCaseUrn(), caseUnReserved.getReservedBy(), now())));
        caseReservedListener.handleCaseUnReserved(caseReservedEnvelope);

        verify(reserveCaseRepository).remove(reserveCaseArgumentCaptor.capture());

        final ReserveCase removedCaseReserved = reserveCaseArgumentCaptor.getValue();

        assertThat(caseUnReserved.getCaseId(), is(removedCaseReserved.getCaseId()));
        assertThat(caseUnReserved.getCaseUrn(), is(removedCaseReserved.getCaseUrn()));
        assertThat(caseUnReserved.getReservedBy(), is(removedCaseReserved.getReservedBy()));
    }
}
