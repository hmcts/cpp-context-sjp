package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;
import uk.gov.moj.cpp.sjp.persistence.repository.ReadyCasesRepository;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class ReadyCaseListener {

    @Inject
    private ReadyCasesRepository readyCasesRepository;

    @Transactional
    @Handles("sjp.events.case-marked-ready-for-decision")
    public void handleCaseMarkedReadyForDecisison(final JsonEnvelope caseMarkedReadyForDecisionEvent) {
        final JsonObject caseMarkedReadyForDecision = caseMarkedReadyForDecisionEvent.payloadAsJsonObject();
        final ReadyCase readyCase = new ReadyCase(
                UUID.fromString(caseMarkedReadyForDecision.getString("caseId")),
                caseMarkedReadyForDecision.getString("reason")
        );
        readyCasesRepository.save(readyCase);
    }

    @Transactional
    @Handles("sjp.events.case-unmarked-ready-for-decision")
    public void handleCaseUnmarkedReadyForDecisison(final JsonEnvelope caseUnmarkedReadyForDecisionEvent) {
        final JsonObject caseUnmarkedReadyForDecison = caseUnmarkedReadyForDecisionEvent.payloadAsJsonObject();
        final ReadyCase readyCase = readyCasesRepository.findBy(UUID.fromString(caseUnmarkedReadyForDecison.getString("caseId")));
        readyCasesRepository.remove(readyCase);
    }

}
