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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.sjp.persistence.entity.ApplicationStatus.APPLICATION_STATUS_NOT_KNOWN;

@ServiceComponent(EVENT_LISTENER)
public class CCApplicationStatusListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CCApplicationStatusListener.class);

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
        final CaseDetail caseDetail = caseService.findById(ccApplicationStatusUpdated.getCaseId());

        // APPLICATION_STATUS_NOT_KNOWN (e.g. adjournment only) must not overwrite a pending or final status
        if (APPLICATION_STATUS_NOT_KNOWN.name().equals(ccApplicationStatusUpdated.getStatus().name())
                && caseDetail.getCcApplicationStatus() != null) {
            LOGGER.info("Ignoring {} for case {} as CC application status is already {}",
                    APPLICATION_STATUS_NOT_KNOWN, caseDetail.getId(), caseDetail.getCcApplicationStatus());
            return;
        }

        setCCApplicationStatus(caseDetail, ccApplicationStatusUpdated.getStatus());
    }

    private void setCCApplicationStatusForCase(final UUID caseId, final uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus applicationStatus) {
        setCCApplicationStatus(caseService.findById(caseId), applicationStatus);
    }

    private void setCCApplicationStatus(final CaseDetail caseDetail, final uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus applicationStatus) {
        caseDetail.setCcApplicationStatus(ApplicationStatus.valueOf(applicationStatus.name()));
        caseService.saveCaseDetail(caseDetail);
    }
}
