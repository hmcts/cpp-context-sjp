package uk.gov.moj.cpp.sjp.event.listener;


import static java.lang.Boolean.TRUE;
import static java.time.LocalDate.now;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.common.CaseByManagementStatus;
import uk.gov.moj.cpp.sjp.domain.common.CaseManagementStatus;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequestCancelled;
import uk.gov.moj.cpp.sjp.event.AllOffencesWithdrawalRequested;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAdded;
import uk.gov.moj.cpp.sjp.event.CaseListedInCriminalCourts;
import uk.gov.moj.cpp.sjp.event.CaseListedInCriminalCourtsV2;
import uk.gov.moj.cpp.sjp.event.CaseOffenceListedInCriminalCourts;
import uk.gov.moj.cpp.sjp.event.CaseStatusChanged;
import uk.gov.moj.cpp.sjp.event.casemanagement.UpdateCasesManagementStatus;
import uk.gov.moj.cpp.sjp.event.listener.converter.CaseDocumentAddedToCaseDocument;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseDocumentRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.ReadyCaseRepository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class CaseUpdatedListener {

    private static final String CASE_ID = "caseId";

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

    @Handles(CaseStatusChanged.EVENT_NAME)
    @Transactional
    public void caseStatusChanged(final JsonEnvelope envelope) {
        final CaseStatusChanged caseStatusChanged = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), CaseStatusChanged.class);
        final CaseDetail caseDetail = findCaseById(caseStatusChanged.getCaseId());
        caseDetail.setCaseStatus(caseStatusChanged.getCaseStatus());
    }

    @Handles(AllOffencesWithdrawalRequested.EVENT_NAME)
    @Transactional
    public void allOffencesWithdrawalRequested(final JsonEnvelope envelope) {
        final AllOffencesWithdrawalRequested event = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), AllOffencesWithdrawalRequested.class);

        //ATCM-4086 - As a part of this story update the withdrawalRequestReasonId in Offence to other.
        updateWithdrawalRequestedDate(event.getCaseId(), envelope.metadata().createdAt().map(ZonedDateTime::toLocalDate).orElse(now()));
    }

    @Handles(AllOffencesWithdrawalRequestCancelled.EVENT_NAME)
    @Transactional
    public void allOffencesWithdrawalRequestCancelled(final JsonEnvelope envelope) {
        final AllOffencesWithdrawalRequestCancelled event = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), AllOffencesWithdrawalRequestCancelled.class);

        //ATCM-4086 - As a part of this story update the withdrawalRequestReasonId to null.
        updateWithdrawalRequestedDate(event.getCaseId(), null);
    }

    @Handles(CaseDocumentAdded.EVENT_NAME)
    @Transactional
    public void addCaseDocument(final JsonEnvelope envelope) {
        final CaseDocumentAdded caseDocumentAdded = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), CaseDocumentAdded.class);

        final CaseDocument caseDocument = caseDocumentAddedConverter.convert(caseDocumentAdded);
        caseDocumentRepository.save(caseDocument);
    }

    private CaseDetail findCaseById(final UUID caseId) {
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
        final CaseDetail caseDetail = findCaseById(
                fromString(envelope.payloadAsJsonObject().getString(CASE_ID))
        );
        caseDetail.undoReopenCase();
    }

    @Handles(CaseListedInCriminalCourts.EVENT_NAME)
    @Transactional
    public void updateCaseListedInCriminalCourts(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final CaseDetail caseDetail = findCaseById(
                fromString(payload.getString(CASE_ID))
        );
        caseDetail.setListedInCriminalCourts(TRUE);
        caseDetail.setHearingCourtName(Optional.ofNullable(payload.getString("hearingCourtName")).orElse(null));
        caseDetail.setHearingTime(ZonedDateTime.parse(payload.getString("hearingTime")));
    }

    @Handles(CaseListedInCriminalCourtsV2.EVENT_NAME)
    @Transactional
    @SuppressWarnings({"squid:S3655"})
    public void updateCaseListedInCCForReferToCourt(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        final CaseListedInCriminalCourtsV2 caseListedInCriminalCourtsV2 = jsonObjectToObjectConverter.convert(payload, CaseListedInCriminalCourtsV2.class);

        // find the first hearing
        final ZonedDateTime firstSittingDate = caseListedInCriminalCourtsV2
                .getOffenceHearings()
                .stream()
                .flatMap(e -> e.getHearingDays().stream())
                .map(HearingDay::getSittingDay)
                .sorted()
                .findFirst()
                .get();

        final CaseOffenceListedInCriminalCourts caseOffenceListedInCriminalCourts = caseListedInCriminalCourtsV2
                .getOffenceHearings()
                .stream()
                .filter(e -> e.getHearingDays()
                        .stream()
                        .anyMatch(b -> b.getSittingDay().equals(firstSittingDate)))
                .findFirst()
                .get();


        final CaseDetail caseDetail = findCaseById(caseOffenceListedInCriminalCourts.getCaseId());
        final String courtName = caseOffenceListedInCriminalCourts.getCourtCentre().getName();
        caseDetail.setHearingCourtName(Optional.ofNullable(courtName).orElse(null));

        caseDetail.setHearingTime(firstSittingDate);
        caseDetail.setListedInCriminalCourts(TRUE);
    }

    @Handles(UpdateCasesManagementStatus.EVENT_NAME)
    @Transactional
    public void updateCaseManagementStatus(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final List<CaseByManagementStatus> cases = payload.getJsonArray("cases")
                .getValuesAs(JsonObject.class)
                .stream()
                .map(caseJson -> new CaseByManagementStatus(fromString(caseJson.getString(CASE_ID)), CaseManagementStatus.valueOf(caseJson.getString("caseManagementStatus"))))
                .collect(Collectors.toList());

        cases.forEach(caseByManagementStatus -> {
            final CaseDetail caseDetail = findCaseById(caseByManagementStatus.getCaseId());
            caseDetail.setCaseManagementStatus(caseByManagementStatus.getCaseManagementStatus());
        });
    }

    private void handleCaseReopened(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        final UUID caseId = fromString(payload.getString(CASE_ID));
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
