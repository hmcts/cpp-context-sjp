package uk.gov.moj.cpp.sjp.event.listener;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.PLEA_RECEIVED_NOT_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.WITHDRAWAL_REQUEST_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.PendingDatesToAvoidRepository;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DatesToAvoidAddedListenerTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private PendingDatesToAvoidRepository pendingDatesToAvoidRepository;

    @InjectMocks
    private DatesToAvoidAddedListener datesToAvoidAddedListener;

    @Test
    public void shouldAddDatesToAvoid() {
        final UUID caseId = UUID.randomUUID();
        final String datesToAvoid = "Away first two weeks of July 2018";
        final JsonEnvelope event = envelope()
                .withPayloadOf(caseId.toString(), "caseId")
                .withPayloadOf(datesToAvoid, "datesToAvoid")
                .build();

        datesToAvoidAddedListener.addDatesToAvoid(event);

        verify(caseRepository).updateDatesToAvoid(caseId, datesToAvoid);
        verify(pendingDatesToAvoidRepository).removeByCaseId(caseId);
    }
}