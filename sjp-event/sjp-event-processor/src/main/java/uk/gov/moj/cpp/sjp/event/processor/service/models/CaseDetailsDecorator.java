package uk.gov.moj.cpp.sjp.event.processor.service.models;

import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;

import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Because the classes used by the Sjp Query endpoints are auto-generated we cannot add any
 * behaviour that was supposed to be in these classes hence this wrapper/decorator class has been
 * introduced. Use this decorator to add behaviour that would otherwise have been added to the
 * original class
 */
public class CaseDetailsDecorator extends CaseDetails {

    public CaseDetailsDecorator(final CaseDetails caseDetails) {
        super(
                caseDetails.getAdjournedTo(),
                caseDetails.getAssigned(),
                caseDetails.getCaseApplication(),
                caseDetails.getCaseDecisions(),
                caseDetails.getCaseDocuments(),
                caseDetails.getCompleted(),
                caseDetails.getCosts(),
                caseDetails.getDateTimeCreated(),
                caseDetails.getDatesToAvoid(),
                caseDetails.getDefendant(),
                caseDetails.getEnterpriseId(),
                caseDetails.getId(),
                caseDetails.getLibraCaseNumber(),
                caseDetails.getListedInCriminalCourts(),
                caseDetails.getManagedByATCM(),
                caseDetails.getOnlinePleaReceived(),
                caseDetails.getPoliceFlag(),
                caseDetails.getPostConviction(),
                caseDetails.getPostingDate(),
                caseDetails.getProsecutingAuthority(),
                caseDetails.getProsecutingAuthorityName(),
                caseDetails.getReopenedDate(),
                caseDetails.getReopenedInLibraReason(),
                caseDetails.getSetAside(),
                caseDetails.getStatus(),
                caseDetails.getUrn()
        );
    }

    public Optional<ApplicationDecisionDecorator> getCurrentApplicationDecision() {
        return getCaseDecisions()
                .stream()
                .filter(caseDecision -> nonNull(caseDecision.getApplicationDecision()))
                .max(comparing(CaseDecision::getSavedAt))
                .map(applicationDecision -> new ApplicationDecisionDecorator(applicationDecision, this));
    }

    public Optional<ApplicationDecisionDecorator> getApplicationDecisionByDecisionId(final UUID decisionId) {
        return getCaseDecisions()
                .stream()
                .filter(caseDecision -> caseDecision.getId().equals(decisionId))
                .findFirst()
                .map(applicationDecision -> new ApplicationDecisionDecorator(applicationDecision, this));
    }

    public Optional<CaseDecisionDecorator> getCaseDecisionBySavedAt(final ZonedDateTime savedAt) {
        return getCaseDecisions()
                .stream()
                .filter(caseDecision -> caseDecision.getSavedAt().equals(savedAt))
                .findFirst()
                .map(CaseDecisionDecorator::new);
    }

    public Offence getOffenceById(final UUID offenceId) {
        return getDefendant().getOffences()
                .stream()
                .filter(offence -> offence.getId().equals(offenceId))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    public String getDefendantFirstName() {
        return getDefendant().getPersonalDetails().getFirstName();
    }

    public String getDefendantLastName() {
        return getDefendant().getPersonalDetails().getLastName();
    }

    public Optional<LocalDate> getDefendantDateOfBirth() {
        return Optional.ofNullable(getDefendant().getPersonalDetails())
                .map(PersonalDetails::getDateOfBirth);
    }
}
