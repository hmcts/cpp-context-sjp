package uk.gov.moj.cpp.sjp.event.processor.service.models;

import uk.gov.justice.json.schemas.domains.sjp.ApplicationType;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDecision;

import java.time.ZonedDateTime;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Because the classes used by the Sjp Query endpoints are auto-generated we cannot add any
 * behaviour that was supposed to be in these classes hence this wrapper/decorator class has been
 * introduced. Use this decorator to add behaviour that would otherwise have been added to the
 * original class
 */
public class ApplicationDecisionDecorator extends CaseDecision {

    private final CaseDetailsDecorator caseDetails;

    public ApplicationDecisionDecorator(final CaseDecision applicationDecision, final CaseDetailsDecorator caseDetails) {
        super(applicationDecision.getApplicationDecision(),
                applicationDecision.getFinancialImposition(),
                applicationDecision.getId(),
                applicationDecision.getOffenceDecisions(),
                applicationDecision.getSavedAt(),
                applicationDecision.getSession());
        this.caseDetails = caseDetails;
    }

    public CaseDetailsDecorator getCaseDetails() {
        return this.caseDetails;
    }

    public CaseDecisionDecorator getPreviousFinalDecision() {
        final ZonedDateTime savedAt = getApplicationDecision().getPreviousFinalDecision();
        return caseDetails.getCaseDecisionBySavedAt(savedAt).orElseThrow(IllegalStateException::new);
    }

    public ApplicationType getApplicationType() {
        return getApplicationDecision().getApplicationType();
    }

    public String getLocalJusticeAreaNationalCourtCode() {
        return getSession().getLocalJusticeAreaNationalCourtCode();
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(o, this);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
