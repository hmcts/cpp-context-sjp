package uk.gov.moj.cpp.sjp.query.view.response;

import static java.util.Objects.nonNull;

import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseApplicationDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialImposition;
import uk.gov.moj.cpp.sjp.persistence.entity.Session;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static java.util.Objects.nonNull;

import com.google.common.collect.ImmutableList;

public class CaseDecisionView implements Comparable<CaseDecisionView> {

    private UUID id;

    private SessionView session;

    private ZonedDateTime savedAt;

    private List<OffenceDecisionView> offenceDecisions = new ArrayList<>();

    private ApplicationDecisionView applicationDecision;

    private FinancialImpositionView financialImposition;

    public CaseDecisionView(final CaseDecision caseDecisionEntity) {
        this.id = caseDecisionEntity.getId();
        this.session = convertSessionEntity(caseDecisionEntity.getSession());
        this.savedAt = caseDecisionEntity.getSavedAt();
        caseDecisionEntity.getOffenceDecisions().forEach(offenceDecision -> this.offenceDecisions.add(new OffenceDecisionView(offenceDecision)));
        createFinancialImpositionView(caseDecisionEntity);
    }

    public CaseDecisionView(final CaseApplicationDecision applicationDecisionEntity) {
        this.id = applicationDecisionEntity.getDecisionId();
        this.session = convertSessionEntity(applicationDecisionEntity.getSession());
        this.savedAt = applicationDecisionEntity.getSavedAt();
        this.applicationDecision = new ApplicationDecisionView(applicationDecisionEntity);
    }

    private void createFinancialImpositionView(CaseDecision caseDecisionEntity) {
        final FinancialImposition financialImpositionEntity = caseDecisionEntity.getFinancialImposition();
        if (nonNull(financialImpositionEntity)) {
            this.financialImposition = new FinancialImpositionView(financialImpositionEntity.getCostsAndSurcharge(),
                    financialImpositionEntity.getPayment());
        }
    }

    public UUID getId() {
        return id;
    }

    public SessionView getSession() {
        return session;
    }

    public ZonedDateTime getSavedAt() {
        return savedAt;
    }

    public List<OffenceDecisionView> getOffenceDecisions() {
        return ImmutableList.copyOf(offenceDecisions);
    }

    public FinancialImpositionView getFinancialImposition() {
        return financialImposition;
    }

    public ApplicationDecisionView getApplicationDecision() {
        return applicationDecision;
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

    @Override
    public int compareTo(final CaseDecisionView o) {
        return savedAt.compareTo(o.getSavedAt());
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CaseDecisionView)) {
            return false;
        }
        final CaseDecisionView that = (CaseDecisionView) o;
        return id.equals(that.id) &&
                Objects.equals(session, that.session) &&
                savedAt.equals(that.savedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, session, savedAt);
    }
}
