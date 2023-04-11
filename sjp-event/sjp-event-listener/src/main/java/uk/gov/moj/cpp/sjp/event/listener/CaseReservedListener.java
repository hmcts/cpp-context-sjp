package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;


import java.util.List;
import javax.inject.Inject;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.event.CaseReserved;
import uk.gov.moj.cpp.sjp.event.CaseUnReserved;
import uk.gov.moj.cpp.sjp.persistence.entity.ReserveCase;
import uk.gov.moj.cpp.sjp.persistence.repository.ReserveCaseRepository;

@ServiceComponent(EVENT_LISTENER)
public class CaseReservedListener {

    @Inject
    private ReserveCaseRepository reserveCaseRepository;


    @Handles(CaseReserved.EVENT_NAME)
    public void handleCaseReserved(final Envelope<CaseReserved> envelope){
        final CaseReserved caseReserved = envelope.payload();
        final ReserveCase reserveCase = new ReserveCase(caseReserved.getCaseId(), caseReserved.getCaseUrn(), caseReserved.getReservedBy(), caseReserved.getReservedAt());

        reserveCaseRepository.save(reserveCase);

    }

    @Handles(CaseUnReserved.EVENT_NAME)
    public void handleCaseUnReserved(final Envelope<CaseUnReserved> envelope){
        final CaseUnReserved caseUnReserved = envelope.payload();
        final List<ReserveCase> reserveCases = reserveCaseRepository.findByCaseId(caseUnReserved.getCaseId());
        reserveCases.forEach(reserveCase -> reserveCaseRepository.remove(reserveCase));

    }
}
