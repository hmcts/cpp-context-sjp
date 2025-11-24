package uk.gov.moj.cpp.sjp.event.listener;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.event.CCApplicationStatusCreated;
import uk.gov.moj.cpp.sjp.event.CCApplicationStatusUpdated;
import uk.gov.moj.cpp.sjp.event.listener.service.CaseService;
import uk.gov.moj.cpp.sjp.persistence.entity.ApplicationStatus;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;

import javax.inject.Inject;

import java.util.UUID;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@ServiceComponent(EVENT_LISTENER)
public class CCApplicationStatusListener {

    @Inject
    private CaseService caseService;

    @Handles("sjp.events.cc-application-status-created")
    public void handleCCApplicationStatusCreated(final Envelope<CCApplicationStatusCreated> envelope) {
        final CCApplicationStatusCreated ccApplicationStatusCreated = envelope.payload();
        setCCApplicationStatusForCase(ccApplicationStatusCreated.getCaseId(), ccApplicationStatusCreated.getStatus());
    }

    @Handles("sjp.events.cc-application-status-updated")
    public void handleCCApplicationStatusUpdated(final Envelope<CCApplicationStatusUpdated> envelope) {
        final CCApplicationStatusUpdated ccApplicationStatusUpdated = envelope.payload();
        setCCApplicationStatusForCase(ccApplicationStatusUpdated.getCaseId(), ccApplicationStatusUpdated.getStatus());
    }

    private void setCCApplicationStatusForCase(final UUID caseId, final uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus applicationStatus) {
        final CaseDetail caseDetail = caseService.findById(caseId);
        caseDetail.setCcApplicationStatus(ApplicationStatus.valueOf(applicationStatus.name()));
        caseService.saveCaseDetail(caseDetail);
    }
}
