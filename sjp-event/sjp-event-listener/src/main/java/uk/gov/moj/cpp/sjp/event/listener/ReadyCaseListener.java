package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.sjp.persistence.entity.CasePublishStatus.createFirstPublishedCasePublishStatus;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.Priority;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseUnmarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CasePublishStatus;
import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;
import uk.gov.moj.cpp.sjp.persistence.repository.CasePublishStatusRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.ReadyCaseRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
    private CasePublishStatusRepository casePublishStatusRepository;
    @Inject
    private CaseRepository caseRepository;

    @Transactional
    @Handles(CaseMarkedReadyForDecision.EVENT_NAME)
    public void handleCaseMarkedReadyForDecision(final JsonEnvelope caseMarkedReadyForDecisionEvent) {
        final JsonObject caseMarkedReadyForDecision = caseMarkedReadyForDecisionEvent.payloadAsJsonObject();
        final UUID caseId = fromString(caseMarkedReadyForDecision.getString("caseId"));
        final CaseDetail caseDetail = caseRepository.findBy(caseId);


        ReadyCase readyCase = readyCaseRepository.findBy(caseId);
        final LocalDate markedAt = Instant.parse(caseMarkedReadyForDecision.getString("markedAt")).atZone(ZoneOffset.UTC).toLocalDate();
        if (null != readyCase) {
            readyCase.setReason(CaseReadinessReason.valueOf(caseMarkedReadyForDecision.getString("reason")));
            readyCase.setSessionType(SessionType.valueOf(caseMarkedReadyForDecision.getString("sessionType")));
            readyCase.setPriority(Priority.valueOf(caseMarkedReadyForDecision.getString("priority")).getIntValue());
            readyCase.setProsecutionAuthority(caseDetail.getProsecutingAuthority());
            readyCase.setPostingDate(caseDetail.getPostingDate());
            readyCase.setMarkedAt(markedAt);
        } else {
            readyCase = new ReadyCase(
                    caseId,
                    CaseReadinessReason.valueOf(caseMarkedReadyForDecision.getString("reason")),
                    null,
                    SessionType.valueOf(caseMarkedReadyForDecision.getString("sessionType")),
                    Priority.valueOf(caseMarkedReadyForDecision.getString("priority")).getIntValue(),
                    caseDetail.getProsecutingAuthority(),
                    caseDetail.getPostingDate(),
                    markedAt
            );
        }
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
        Optional.ofNullable(readyCaseRepository.findBy(caseId))
                .ifPresent(readyCaseRepository::remove);
    }

    private void resetCasePublishedCount(final UUID caseId) {
        final CasePublishStatus casePublishStatus = casePublishStatusRepository.findBy(caseId);

        if (casePublishStatus != null) {
            casePublishStatus.setNumberOfPublishes(0);
            casePublishStatusRepository.save(casePublishStatus);
        }
    }

}
