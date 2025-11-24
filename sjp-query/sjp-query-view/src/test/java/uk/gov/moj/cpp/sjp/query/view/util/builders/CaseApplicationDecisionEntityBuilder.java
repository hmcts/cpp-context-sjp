package uk.gov.moj.cpp.sjp.query.view.util.builders;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseApplicationDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.Session;

import java.time.ZonedDateTime;
import java.util.UUID;

public class CaseApplicationDecisionEntityBuilder {

    private UUID decisionId;
    private boolean granted;
    private boolean outOfTime;
    private String outOfTimeReason;
    private Session session;
    private ZonedDateTime savedAt;

    private CaseApplicationDecisionEntityBuilder() {
    }

    public static CaseApplicationDecisionEntityBuilder applicationDecision() {
        return new CaseApplicationDecisionEntityBuilder();
    }

    public CaseApplicationDecisionEntityBuilder withDecisionId(final UUID decisionId) {
        this.decisionId = decisionId;
        return this;
    }

    public CaseApplicationDecisionEntityBuilder withGranted(final boolean granted) {
        this.granted = granted;
        return this;
    }

    public CaseApplicationDecisionEntityBuilder withOutOfTime(final String outOfTimeReason) {
        this.outOfTime = true;
        this.outOfTimeReason = outOfTimeReason;
        return this;
    }

    public CaseApplicationDecisionEntityBuilder withSavedAt(final ZonedDateTime savedAt) {
        this.savedAt = savedAt;
        return this;
    }

    public CaseApplicationDecisionEntityBuilder withSession(final Session session) {
        this.session = session;
        return this;
    }

    public CaseApplicationDecision build() {
        final CaseApplicationDecision entity = new CaseApplicationDecision();
        entity.setDecisionId(decisionId);
        entity.setGranted(granted);
        entity.setOutOfTime(outOfTime);
        entity.setOutOfTimeReason(outOfTimeReason);
        entity.setSavedAt(savedAt);
        entity.setSession(session);
        return entity;
    }
}
