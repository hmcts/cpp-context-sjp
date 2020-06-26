package uk.gov.moj.cpp.sjp.event.listener.converter;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.ADJOURN;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFERRED_FOR_FUTURE_SJP_SESSION;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFERRED_TO_OPEN_COURT;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFER_FOR_COURT_HEARING;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.SET_ASIDE;
import static uk.gov.moj.cpp.sjp.event.listener.converter.FinancialImpositionConverter.convertToFinancialImposition;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialImposition;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.Session;
import uk.gov.moj.cpp.sjp.persistence.repository.OffenceRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.EntityManager;

public class DecisionSavedToCaseDecision implements Converter<DecisionSaved, CaseDecision> {

    private static final Set<DecisionType> MULTIPLE_OFFENCE_DECISION_TYPES = newHashSet(ADJOURN,
            REFER_FOR_COURT_HEARING, REFERRED_FOR_FUTURE_SJP_SESSION, REFERRED_TO_OPEN_COURT, SET_ASIDE);

    @Inject
    private EntityManager em;

    @Inject
    private OffenceRepository offenceRepository;

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

    private List<uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision> convertToOffenceDecisionEntity(final UUID caseDecisionId, final OffenceDecision offenceDecision) {
        if (MULTIPLE_OFFENCE_DECISION_TYPES.contains(offenceDecision.getType()) && offenceDecision.hasPressRestriction()) {
            final List<OffenceDetail> offences = offenceRepository.findByIds(offenceDecision.getOffenceIds());
            return OffenceDecisionConverter.convert(caseDecisionId, offenceDecision, offences);
        } else {
            return OffenceDecisionConverter.convert(caseDecisionId, offenceDecision);
        }
    }
}
