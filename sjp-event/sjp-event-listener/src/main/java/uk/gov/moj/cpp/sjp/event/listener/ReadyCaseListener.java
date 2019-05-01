package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.sjp.persistence.entity.CasePublishStatus.createFirstPublishedCasePublishStatus;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseUnmarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CasePublishStatus;
import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;
import uk.gov.moj.cpp.sjp.persistence.repository.CasePublishStatusRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.ReadyCaseRepository;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class ReadyCaseListener {

    @Inject
    private ReadyCaseRepository readyCaseRepository;

    @Inject
    private CasePublishStatusRepository casePublishStatusRepository;

    @Transactional
    @Handles(CaseMarkedReadyForDecision.EVENT_NAME)
    public void handleCaseMarkedReadyForDecision(final JsonEnvelope caseMarkedReadyForDecisionEvent) {
        final JsonObject caseMarkedReadyForDecision = caseMarkedReadyForDecisionEvent.payloadAsJsonObject();
        final ReadyCase readyCase = new ReadyCase(
                fromString(caseMarkedReadyForDecision.getString("caseId")),
                CaseReadinessReason.valueOf(caseMarkedReadyForDecision.getString("reason"))
        );
        readyCaseRepository.save(readyCase);

        createCasePublishStatusIfNotExists(readyCase.getCaseId());
    }

    @Transactional
    @Handles(CaseUnmarkedReadyForDecision.EVENT_NAME)
    public void handleCaseUnmarkedReadyForDecision(final JsonEnvelope caseUnmarkedReadyForDecisionEvent) {
        final JsonObject caseUnmarkedReadyForDecision = caseUnmarkedReadyForDecisionEvent.payloadAsJsonObject();
        final UUID caseId = fromString(caseUnmarkedReadyForDecision.getString("caseId"));
        removeCaseDetails(caseId);
        resetCasePublishedCount(caseId);
    }

    private void createCasePublishStatusIfNotExists(final UUID caseId) {
        final CasePublishStatus casePublishStatus = casePublishStatusRepository.findBy(caseId);
        if (casePublishStatus == null) {
            final CasePublishStatus newCasePublishStatus = createFirstPublishedCasePublishStatus(caseId);
            casePublishStatusRepository.save(newCasePublishStatus);
        }
    }

    private void removeCaseDetails(final UUID caseId) {
        final ReadyCase readyCase = readyCaseRepository.findBy(caseId);
        readyCaseRepository.remove(readyCase);
    }

    private void resetCasePublishedCount(final UUID caseId) {
        final CasePublishStatus casePublishStatus = casePublishStatusRepository.findBy(caseId);

        // HANDLES TRANSITION PERIOD WHEN AN EXISTING READY CASE DOES NOT CONTAIN THE casePublishStatus
        if (casePublishStatus != null) {
            casePublishStatus.setNumberOfPublishes(0);
            casePublishStatusRepository.save(casePublishStatus);
        }
    }

}
