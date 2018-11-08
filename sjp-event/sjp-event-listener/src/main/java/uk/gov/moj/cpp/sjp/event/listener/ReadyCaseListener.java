package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseUnmarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.ReadyCaseRepository;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class ReadyCaseListener {

    @Inject
    private ReadyCaseRepository readyCaseRepository;

    @Inject
    private CaseRepository caseRepository;

    @Transactional
    @Handles(CaseMarkedReadyForDecision.EVENT_NAME)
    public void handleCaseMarkedReadyForDecision(final JsonEnvelope caseMarkedReadyForDecisionEvent) {
        final JsonObject caseMarkedReadyForDecision = caseMarkedReadyForDecisionEvent.payloadAsJsonObject();
        final ReadyCase readyCase = new ReadyCase(
                UUID.fromString(caseMarkedReadyForDecision.getString("caseId")),
                CaseReadinessReason.valueOf(caseMarkedReadyForDecision.getString("reason"))
        );

        final CaseDetail caseDetail = caseRepository.findBy(readyCase.getCaseId());

        /*
        NOTE: When a withdrawal is cancelled the case status goes back to the relevant status based on the rules below i.e.
        'No plea received' when no plea received and the certificate of service date < 28 days,
        'Plea received - ready for decision'  when there is a Guilty plea etc.
        When plea not guilty received and plea updated date > 10 days (activiti makes the decision)

         */
        caseDetail.setStatus(caseDetail.getStatus().markReadyCase(readyCase.getReason(), caseDetail.getDatesToAvoid()));

        readyCaseRepository.save(readyCase);
    }

    @Transactional
    @Handles(CaseUnmarkedReadyForDecision.EVENT_NAME)
    public void handleCaseUnmarkedReadyForDecision(final JsonEnvelope caseUnmarkedReadyForDecisionEvent) {
        final JsonObject caseUnmarkedReadyForDecision = caseUnmarkedReadyForDecisionEvent.payloadAsJsonObject();
        final ReadyCase readyCase = readyCaseRepository.findBy(UUID.fromString(caseUnmarkedReadyForDecision.getString("caseId")));
        readyCaseRepository.remove(readyCase);

        final CaseDetail caseDetail = caseRepository.findBy(UUID.fromString(caseUnmarkedReadyForDecision.getString("caseId")));
        final String plea = caseUnmarkedReadyForDecision.getString("pleaType", null);
        final PleaType pleaType = Optional.ofNullable(plea).map(PleaType::valueOf).orElse(null);

        /*
        NOTE: When a withdrawal is cancelled the case status goes back to the relevant status based on the rules below
        i.e. 'No plea received' when no plea received and the certificate of service date < 28 days,
         'Plea received - ready for decision'  when there is a Guilty plea etc.
         */
        caseDetail.setStatus(caseDetail.getStatus().unmarkReadyCase(pleaType, readyCase.getReason(), caseDetail.getDatesToAvoid()));
    }

}
