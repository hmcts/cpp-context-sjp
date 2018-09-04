package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseUnmarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;
import uk.gov.moj.cpp.sjp.persistence.repository.ReadyCaseRepository;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class ReadyCaseListener {

    @Inject
    private ReadyCaseRepository readyCaseRepository;

    @Transactional
    @Handles(CaseMarkedReadyForDecision.EVENT_NAME)
    public void handleCaseMarkedReadyForDecision(final JsonEnvelope caseMarkedReadyForDecisionEvent) {
        final JsonObject caseMarkedReadyForDecision = caseMarkedReadyForDecisionEvent.payloadAsJsonObject();
        final ReadyCase readyCase = new ReadyCase(
                UUID.fromString(caseMarkedReadyForDecision.getString("caseId")),
                CaseReadinessReason.valueOf(caseMarkedReadyForDecision.getString("reason"))
        );
        readyCaseRepository.save(readyCase);
    }

    @Transactional
    @Handles(CaseUnmarkedReadyForDecision.EVENT_NAME)
    public void handleCaseUnmarkedReadyForDecision(final JsonEnvelope caseUnmarkedReadyForDecisionEvent) {
        final JsonObject caseUnmarkedReadyForDecision = caseUnmarkedReadyForDecisionEvent.payloadAsJsonObject();
        final ReadyCase readyCase = readyCaseRepository.findBy(UUID.fromString(caseUnmarkedReadyForDecision.getString("caseId")));
        readyCaseRepository.remove(readyCase);
    }

}
