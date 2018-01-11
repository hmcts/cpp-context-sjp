package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class CaseSearchResultListener {

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private CaseSearchResultRepository repository;

    @Handles("sjp.events.person-info-added")
    public void personInfoAdded(final JsonEnvelope envelope) {

        final JsonObject payload = envelope.payloadAsJsonObject();

        final CaseSearchResult caseSearchResult = new CaseSearchResult(
                fromString(payload.getString("id")),
                fromString(payload.getString("caseId")),
                payload.getString("firstName", null),
                payload.getString("lastName"),
                ofNullable(payload.getString("dateOfBirth", null))
                        .map(LocalDate::parse).orElse(null),
                payload.getString("postCode", null)
        );
        //postCode contains typo and it is already present in old events
        repository.save(caseSearchResult);
    }

    @Handles("sjp.events.person-info-updated")
    public void personInfoUpdated(final JsonEnvelope event) {
        final JsonObject eventPayload = event.payloadAsJsonObject();
        final List<CaseSearchResult> searchResults = repository.findByCaseId(
                fromString(eventPayload.getString("caseId")));

        searchResults.forEach(caseSearchResult -> {

            ofNullable(eventPayload.getString("firstName", null)).ifPresent(caseSearchResult::setFirstName);
            ofNullable(eventPayload.getString("lastName", null)).ifPresent(caseSearchResult::setLastName);
            ofNullable(eventPayload.getString("dateOfBirth", null)).map(LocalDate::parse).ifPresent(caseSearchResult::setDateOfBirth);
            ofNullable(eventPayload.getString("postCode", null)).ifPresent(caseSearchResult::setPostCode);

            repository.save(caseSearchResult);
        });
    }

    @Handles("sjp.events.case-assignment-created")
    @Transactional
    public void caseAssignmentCreated(final JsonEnvelope envelope) {
        updateCaseAssignment(envelope, true);
        updateCaseDetailsAssignment(envelope, true);
    }

    @Handles("sjp.events.case-assignment-deleted")
    @Transactional
    public void caseAssignmentDeleted(final JsonEnvelope envelope) {
        updateCaseAssignment(envelope, false);
        updateCaseDetailsAssignment(envelope, false);
    }

    private void updateCaseAssignment(final JsonEnvelope envelope, final boolean assigned) {
        final UUID caseId = UUID.fromString(envelope.payloadAsJsonObject().getString("caseId"));
        final List<CaseSearchResult> results = repository.findByCaseId(caseId);
        results.forEach(result -> result.setAssigned(assigned));
    }

    private void updateCaseDetailsAssignment(final JsonEnvelope envelope, final boolean assigned) {
        final UUID caseId = UUID.fromString(envelope.payloadAsJsonObject().getString("caseId"));
        final CaseDetail caseDetail = caseRepository.findBy(caseId);
        caseDetail.setAssigned(assigned);
    }
}
