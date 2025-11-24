package uk.gov.moj.cpp.sjp.event.listener;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.sjp.persistence.entity.ApplicationType.REOPENING;
import static uk.gov.moj.cpp.sjp.persistence.entity.ApplicationType.STAT_DEC;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.event.ApplicationStatusChanged;
import uk.gov.moj.cpp.sjp.event.CaseApplicationForReopeningRecorded;
import uk.gov.moj.cpp.sjp.event.CaseApplicationRecorded;
import uk.gov.moj.cpp.sjp.event.CaseStatDecRecorded;
import uk.gov.moj.cpp.sjp.event.decision.ApplicationDecisionSetAside;
import uk.gov.moj.cpp.sjp.event.listener.service.CaseApplicationService;
import uk.gov.moj.cpp.sjp.event.listener.service.CaseService;
import uk.gov.moj.cpp.sjp.persistence.entity.ApplicationStatus;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseApplication;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;

import java.time.LocalDate;

import javax.inject.Inject;
import javax.json.JsonObject;


@ServiceComponent(EVENT_LISTENER)
public class CaseApplicationListener {

    @Inject
    private CaseService caseService;

    @Inject
    private CaseApplicationService caseApplicationService;

    @Inject
    private  ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("sjp.events.case-application-recorded")
    public void handleCaseApplicationRecorded(final Envelope<CaseApplicationRecorded> envelope) {
        final CaseApplicationRecorded caseApplicationRecorded = envelope.payload();
        final CaseDetail caseDetail = caseService.findById(caseApplicationRecorded.getCaseId());
        final CaseApplication caseApplication = buildCaseApplication(caseApplicationRecorded, caseDetail);
        caseDetail.setCurrentApplication(caseApplication);
        caseService.saveCaseDetail(caseDetail);
        caseApplicationService.saveCaseApplication(caseApplication);
    }

    @Handles("sjp.events.case-stat-dec-recorded")
    public void handleStatDecRecorded(final Envelope<CaseStatDecRecorded> envelope) {
        final CaseStatDecRecorded caseStatDecRecorded = envelope.payload();
        final CaseApplication caseApplication = caseApplicationService.findById(caseStatDecRecorded.getApplicationId());
        caseApplication.setApplicationType(STAT_DEC);
        caseApplicationService.saveCaseApplication(caseApplication);
    }

    @Handles("sjp.events.case-application-for-reopening-recorded")
    public void handleReopeningRecorded(final Envelope<CaseApplicationForReopeningRecorded> envelope) {
        final CaseApplicationForReopeningRecorded reopeningRecorded = envelope.payload();
        final CaseApplication caseApplication = caseApplicationService.findById(reopeningRecorded.getApplicationId());
        caseApplication.setApplicationType(REOPENING);
        caseApplicationService.saveCaseApplication(caseApplication);
    }

    @Handles("sjp.events.application-status-changed")
    public void handleApplicationStatusChanged(final Envelope<ApplicationStatusChanged> envelope) {
        final ApplicationStatusChanged statusChanged = envelope.payload();
        final CaseApplication caseApplication = caseApplicationService.findById(statusChanged.getApplicationId());
        caseApplication.setApplicationStatus(ApplicationStatus.valueOf(statusChanged.getStatus().name()));
        caseApplicationService.saveCaseApplication(caseApplication);
    }

    @Handles("sjp.events.application-decision-set-aside")
    public void handleApplicationDecisionSetAside(final Envelope<ApplicationDecisionSetAside> envelope) {
        final ApplicationDecisionSetAside applicationSetAside = envelope.payload();
        final CaseDetail caseDetail = caseService.findById(applicationSetAside.getCaseId());
        caseDetail.setSetAside(true);
        caseDetail.setCompleted(false);
        caseDetail.getDefendant().getOffences().stream()
                .filter(offenceDetail -> ofNullable(offenceDetail.getCompleted()).orElse(false))
                .forEach(offenceDetail -> offenceDetail.setCompleted(false));
        caseService.saveCaseDetail(caseDetail);
    }

    private CaseApplication buildCaseApplication(final CaseApplicationRecorded caseApplicationRecorded, final CaseDetail caseDetail) {
        final CaseApplication caseApplication = new CaseApplication();

        caseApplication.setApplicationId(caseApplicationRecorded.getCourtApplication().getId());
        caseApplication.setCaseDetail(caseDetail);
        caseApplication.setTypeId(caseApplicationRecorded.getCourtApplication().getType().getId());
        caseApplication.setApplicationStatus(ApplicationStatus.valueOf(caseApplicationRecorded.getCourtApplication().getApplicationStatus().toString()));
        caseApplication.setTypeCode(caseApplicationRecorded.getCourtApplication().getType().getCode());
        caseApplication.setDateReceived(LocalDate.parse(caseApplicationRecorded.getCourtApplication().getApplicationReceivedDate()));
        caseApplication.setOutOfTimeReason(caseApplicationRecorded.getCourtApplication().getOutOfTimeReasons());
        caseApplication.setOutOfTime(isNullOrEmpty(caseApplicationRecorded.getCourtApplication().getOutOfTimeReasons()) ? false : true);
        caseApplication.setApplicationReference(caseApplicationRecorded.getCourtApplication().getApplicationReference());

        final CourtApplication courtApplication = caseApplicationRecorded.getCourtApplication();
        final JsonObject courtApplicationJsonObject = objectToJsonObjectConverter.convert(courtApplication);
        caseApplication.setInitiatedApplication(courtApplicationJsonObject);

        return caseApplication;
    }

}