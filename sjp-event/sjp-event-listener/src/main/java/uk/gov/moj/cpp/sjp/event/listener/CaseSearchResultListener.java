package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.listener.handler.CaseSearchResultService;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.ReadyCaseRepository;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class CaseSearchResultListener {

    @Inject
    private JsonObjectToObjectConverter converter;

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private CaseSearchResultService caseSearchResultService;

    @Inject
    private ReadyCaseRepository readyCaseRepository;

    @Handles(CaseAssigned.EVENT_NAME)
    @Transactional
    public void caseAssigned(final JsonEnvelope envelope) {
        final JsonObject assignment = envelope.payloadAsJsonObject();
        final UUID caseId = UUID.fromString(assignment.getString("caseId"));
        final UUID assigneeId = UUID.fromString(assignment.getString("assigneeId"));

        caseSearchResultService.caseAssigned(caseId);
        updateCaseDetailsAssignment(caseId, assigneeId);
    }

    @Handles(CaseUnassigned.EVENT_NAME)
    @Transactional
    public void caseUnassigned(final JsonEnvelope envelope) {
        final UUID caseId = UUID.fromString(envelope.payloadAsJsonObject().getString("caseId"));

        caseSearchResultService.caseUnassigned(caseId);
        updateCaseDetailsAssignment(caseId, null);
    }

    private void updateCaseDetailsAssignment(final UUID caseId, final UUID assigneeId) {
        final CaseDetail caseDetail = caseRepository.findBy(caseId);
        if (caseDetail != null) {
            caseDetail.setAssigneeId(assigneeId);
            Optional.ofNullable(readyCaseRepository.findBy(caseId)).ifPresent(readyCase -> readyCase.setAssigneeId(assigneeId));
        }
    }
}
