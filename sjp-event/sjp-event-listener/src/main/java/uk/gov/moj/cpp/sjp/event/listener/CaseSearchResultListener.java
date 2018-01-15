package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;

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

    @Handles("sjp.events.case-assigned")
    @Transactional
    public void caseAssigned(final JsonEnvelope envelope) {
        final JsonObject assignment = envelope.payloadAsJsonObject();
        final UUID caseId = UUID.fromString(assignment.getString("caseId"));
        final UUID assigneeId = UUID.fromString(assignment.getString("assigneeId"));

        updateCaseAssignment(caseId, true);
        updateCaseDetailsAssignment(caseId, assigneeId);
    }

    @Handles("sjp.events.case-assignment-deleted")
    @Transactional
    public void caseAssignmentDeleted(final JsonEnvelope envelope) {
        final UUID caseId = UUID.fromString(envelope.payloadAsJsonObject().getString("caseId"));

        updateCaseAssignment(caseId, false);
        updateCaseDetailsAssignment(caseId, null);
    }

    private void updateCaseAssignment(final UUID caseId, boolean assigned) {
        final List<CaseSearchResult> results = repository.findByCaseId(caseId);
        results.forEach(result -> result.setAssigned(assigned));
    }

    private void updateCaseDetailsAssignment(final UUID caseId, final UUID assigneeId) {
        final CaseDetail caseDetail = caseRepository.findBy(caseId);
        caseDetail.setAssigneeId(assigneeId);
    }
}
