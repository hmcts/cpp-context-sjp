package uk.gov.moj.cpp.sjp.event.processor.service.models;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.QueryOffenceDecision;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Because the classes used by the Sjp Query endpoints are auto-generated we cannot add any
 * behaviour that was supposed to be in these classes hence this wrapper/decorator class has been
 * introduced. Use this decorator to add behaviour that would otherwise have been added to the
 * original class
 */
public class CaseDecisionDecorator extends CaseDecision {

    public CaseDecisionDecorator(final CaseDecision caseDecision) {
        super(caseDecision.getApplicationDecision(),
                caseDecision.getFinancialImposition(),
                caseDecision.getId(),
                caseDecision.getOffenceDecisions(),
                caseDecision.getSavedAt(),
                caseDecision.getSession());
    }

    public boolean hasEndorsementsOrDisqualification() {
        return getOffenceDecisions()
                .stream()
                .anyMatch(this::hasEndorsementsOrDisqualification);
    }

    public List<QueryOffenceDecision> getOffenceDecisionsWithEndorsementOrDisqualification() {
        return getOffenceDecisions().stream()
                .filter(this::hasEndorsementsOrDisqualification)
                .collect(Collectors.toList());
    }

    private boolean hasEndorsementsOrDisqualification(final QueryOffenceDecision offenceDecision) {
        final boolean licenceEndorsed = isTrue(offenceDecision.getLicenceEndorsement());
        final boolean disqualification = isTrue(offenceDecision.getDisqualification());
        return licenceEndorsed || disqualification;
    }
}
