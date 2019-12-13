package uk.gov.moj.cpp.sjp.query.view.response;

import static java.util.Objects.nonNull;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialImposition;
import uk.gov.moj.cpp.sjp.persistence.entity.Session;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.ImmutableList;

public class CaseDecisionView {

    private UUID id;

    private SessionView session;

    private ZonedDateTime savedAt;

    private List<OffenceDecisionView> offenceDecisions = new ArrayList<>();

    private FinancialImpositionView financialImposition;

    public CaseDecisionView(final CaseDecision caseDecisionEntity) {
        this.id = caseDecisionEntity.getId();
        this.session = convertSessionEntity(caseDecisionEntity.getSession());
        this.savedAt = caseDecisionEntity.getSavedAt();
        caseDecisionEntity.getOffenceDecisions().forEach(offenceDecision -> this.offenceDecisions.add(new OffenceDecisionView(offenceDecision)));
        createFinancialImpositionView(caseDecisionEntity);
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

    private SessionView convertSessionEntity(final Session entity) {
        final SessionView view = new SessionView();

        if (entity == null) {
            // potentially session can be null (legacy RSJP)
            return view;
        }

        view.setSessionId(entity.getSessionId());
        view.setLegalAdviserUserId(entity.getUserId());
        view.setCourtHouseCode(entity.getCourtHouseCode());
        view.setCourtHouseName(entity.getCourtHouseName());
        view.setLocalJusticeAreaNationalCourtCode(entity.getLocalJusticeAreaNationalCourtCode());
        view.setMagistrate(entity.getMagistrate());
        view.setStartedAt(entity.getStartedAt());
        view.setEndedAt(entity.getEndedAt().orElse(null));
        view.setSessionType(entity.getType().name());

        return view;
    }
}
