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
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class CaseSearchResultListener {

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private CaseSearchResultRepository repository;

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
