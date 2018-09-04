package uk.gov.moj.cpp.sjp.event.listener;


import static java.time.LocalDate.now;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequestCancelled;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequested;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAdded;
import uk.gov.moj.cpp.sjp.event.listener.converter.CaseDocumentAddedToCaseDocument;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseDocumentRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.ReadyCaseRepository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class CaseUpdatedListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private CaseDocumentAddedToCaseDocument caseDocumentAddedConverter;

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private CaseDocumentRepository caseDocumentRepository;

    @Inject
    private CaseSearchResultRepository searchResultRepository;

    @Inject
    private ReadyCaseRepository readyCaseRepository;

    @Handles(CaseCompleted.EVENT_NAME)
    @Transactional
    public void caseCompleted(final JsonEnvelope envelope) {
        final CaseCompleted caseCompletedEvent = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), CaseCompleted.class);
        caseRepository.completeCase(caseCompletedEvent.getCaseId());

        //Conditional removal needed in case of event replay of old cases which have CaseCompleted but not CaseMarkedReadyEvent
        Optional.ofNullable(readyCaseRepository.findBy(caseCompletedEvent.getCaseId()))
                .ifPresent(readyCaseRepository::remove);
    }

    @Handles(AllOffencesWithdrawalRequested.EVENT_NAME)
    @Transactional
    public void allOffencesWithdrawalRequested(final JsonEnvelope envelope) {
        final AllOffencesWithdrawalRequested event = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), AllOffencesWithdrawalRequested.class);
        caseRepository.requestWithdrawalAllOffences(event.getCaseId());

        updateWithdrawalRequestedDate(event.getCaseId(), envelope.metadata().createdAt().map(ZonedDateTime::toLocalDate).orElse(now()));
    }

    @Handles(AllOffencesWithdrawalRequestCancelled.EVENT_NAME)
    @Transactional
    public void allOffencesWithdrawalRequestCancelled(final JsonEnvelope envelope) {
        final AllOffencesWithdrawalRequestCancelled event = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), AllOffencesWithdrawalRequestCancelled.class);
        caseRepository.cancelRequestWithdrawalAllOffences(event.getCaseId());

        updateWithdrawalRequestedDate(event.getCaseId(), null);
    }

    @Handles(CaseDocumentAdded.EVENT_NAME)
    @Transactional
    public void addCaseDocument(final JsonEnvelope envelope) {
        CaseDocumentAdded caseDocumentAdded = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), CaseDocumentAdded.class);

        CaseDocument caseDocument = caseDocumentAddedConverter.convert(caseDocumentAdded);
        caseDocumentRepository.save(caseDocument);
    }

    private CaseDetail findCaseById(UUID caseId) {
        return caseRepository.findBy(caseId);
    }

    @Handles("sjp.events.case-reopened-in-libra")
    @Transactional
    public void markCaseReopened(final JsonEnvelope envelope) {
        handleCaseReopened(envelope);
    }

    @Handles("sjp.events.case-reopened-in-libra-updated")
    @Transactional
    public void updateCaseReopened(final JsonEnvelope envelope) {
        handleCaseReopened(envelope);
    }

    @Handles("sjp.events.case-reopened-in-libra-undone")
    @Transactional
    public void undoCaseReopened(final JsonEnvelope envelope) {
        CaseDetail caseDetail = findCaseById(
                UUID.fromString(envelope.payloadAsJsonObject().getString("caseId"))
        );
        caseDetail.undoReopenCase();
    }

    private void handleCaseReopened(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        final UUID caseId = UUID.fromString(payload.getString("caseId"));
        final CaseDetail caseDetail = findCaseById(caseId);
        caseDetail.setReopenedDate(LocalDates.from(payload.getString("reopenedDate")));
        caseDetail.setLibraCaseNumber(payload.getString("libraCaseNumber"));
        caseDetail.setReopenedInLibraReason(payload.getString("reason"));
    }

    private void updateWithdrawalRequestedDate(final UUID caseId, final LocalDate withdrawalRequestedDate) {
        searchResultRepository.findByCaseId(caseId).forEach(searchResult ->
                searchResult.setWithdrawalRequestedDate(withdrawalRequestedDate));
    }
}
