package uk.gov.moj.cpp.sjp.event.listener.converter;

import static java.util.Objects.*;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.sjp.event.listener.converter.FinancialImpositionConverter.*;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialImposition;
import uk.gov.moj.cpp.sjp.persistence.entity.Session;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.EntityManager;

public class DecisionSavedToCaseDecision implements Converter<DecisionSaved, CaseDecision> {

    @Inject
    private EntityManager em;

    @Override
    public CaseDecision convert(final DecisionSaved event) {

        final CaseDecision caseDecisionEntity = new CaseDecision();

        caseDecisionEntity.setId(event.getDecisionId());
        caseDecisionEntity.setCaseId(event.getCaseId());
        caseDecisionEntity.setSavedAt(event.getSavedAt());

        final Session sessionEntity = em.getReference(Session.class, event.getSessionId());

        caseDecisionEntity.setSession(sessionEntity);
        caseDecisionEntity.setOffenceDecisions(convertToOffenceDecisionEntities(event.getDecisionId(), event.getOffenceDecisions()));
        if(nonNull(event.getFinancialImposition())) {
            final FinancialImposition financialImposition = convertToFinancialImposition(event.getFinancialImposition());
            financialImposition.setCaseDecision(caseDecisionEntity);
            caseDecisionEntity.setFinancialImposition(financialImposition);
        }
        return caseDecisionEntity;
    }

    private List<uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision> convertToOffenceDecisionEntities(final UUID caseDecisionId,
                                                                                                         final List<OffenceDecision> offenceDecisions) {

        return offenceDecisions.stream()
                .flatMap(offenceDecision -> convertToOffenceDecisionEntity(caseDecisionId, offenceDecision).stream())
                .collect(toList());
    }

    private List<uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision> convertToOffenceDecisionEntity(final UUID caseDecisionId,
                                                                                                       final OffenceDecision offenceDecision) {
        return OffenceDecisionConverter.convert(caseDecisionId, offenceDecision);
    }
}
