package uk.gov.moj.cpp.sjp.query.view.response;

import static java.util.Optional.ofNullable;

import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.persistence.entity.ApplicationType;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseApplication;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseApplicationDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.Session;

import java.time.ZonedDateTime;
import java.util.UUID;

public class ApplicationDecisionView {

    private boolean granted;

    private String rejectionReason;

    private Boolean outOfTime;

    private String outOfTimeReason;

    private ApplicationType applicationType;

    private ZonedDateTime previousFinalDecision;

    private CaseDecisionView previousFinalDecisionObject;

    private UUID decisionId;

    private SessionView session;

    public ApplicationDecisionView(final CaseApplicationDecision caseApplicationDecision) {
        this.granted = caseApplicationDecision.isGranted();
        this.outOfTime = caseApplicationDecision.getOutOfTime();
        this.outOfTimeReason = caseApplicationDecision.getOutOfTimeReason();
        this.rejectionReason = caseApplicationDecision.getRejectionReason();
        this.applicationType = ofNullable(caseApplicationDecision.getCaseApplication())
                .map(CaseApplication::getApplicationType)
                .orElse(null);
        this.session = convertSessionEntity(caseApplicationDecision.getSession());
        this.decisionId = caseApplicationDecision.getDecisionId();
    }

    public boolean isGranted() {
        return granted;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public Boolean getOutOfTime() {
        return outOfTime;
    }

    public String getOutOfTimeReason() {
        return outOfTimeReason;
    }

    public ApplicationType getApplicationType() {
        return applicationType;
    }

    public CaseDecisionView getPreviousFinalDecisionObject() {
        return previousFinalDecisionObject;
    }

    public void setPreviousFinalDecision(final CaseDecisionView previousFinalDecision) {
        this.previousFinalDecision = previousFinalDecision.getSavedAt();
        this.previousFinalDecisionObject = previousFinalDecision;
    }

    public ZonedDateTime getPreviousFinalDecision() {
        return previousFinalDecision;
    }

    public SessionView getSession() {
        return session;
    }

    public UUID getDecisionId() {
        return decisionId;
    }

    private SessionView convertSessionEntity(final Session entity) {
        final SessionView view = new SessionView();

        if (entity == null) {
            // potentially session can be null (legacy RSJP)
            return view;
        }

        view.setSessionId(entity.getSessionId());
        view.setCourtHouseCode(entity.getCourtHouseCode());
        view.setCourtHouseName(entity.getCourtHouseName());
        view.setLocalJusticeAreaNationalCourtCode(entity.getLocalJusticeAreaNationalCourtCode());
        view.setMagistrate(entity.getMagistrate());
        view.setStartedAt(entity.getStartedAt());
        view.setEndedAt(entity.getEndedAt().orElse(null));
        view.setSessionType(entity.getType().name());

        if (SessionType.MAGISTRATE.name().equals(entity.getType().name()) && entity.getLegalAdviserUserId() != null) {
            view.setLegalAdviserUserId(entity.getLegalAdviserUserId());
        } else {
            view.setLegalAdviserUserId(entity.getUserId());
        }

        return view;
    }
}
