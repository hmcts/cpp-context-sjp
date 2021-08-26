package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.time.ZonedDateTime.now;

import uk.gov.justice.json.schemas.domains.sjp.ApplicationType;
import uk.gov.moj.cpp.sjp.domain.EnforcementPendingApplicationRequiredNotification;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.Application;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.FinancialImpositionExportDetails;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationRequired;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

public class EnforcementCheckIfNotificationRequired {

    public static final EnforcementCheckIfNotificationRequired INSTANCE = new EnforcementCheckIfNotificationRequired();

    private EnforcementCheckIfNotificationRequired() {
    }

    public Stream<Object> checkIfPendingApplicationToNotified(CaseAggregateState state,
                                                              final EnforcementPendingApplicationRequiredNotification checkNotificationRequired) {
        final UUID defendantId = state.getDefendantId();
        final FinancialImpositionExportDetails financialImpositionExportDetails
                = state.getDefendantFinancialImpositionExportDetails().get(defendantId);

        if (state.hasPendingApplication()
                && null != financialImpositionExportDetails) {
            final String urn = state.getUrn();
            final String defendantName = state.getDefendantFirstName() + " " + state.getDefendantLastName();
            final Application currentApplication = state.getCurrentApplication();
            final UUID applicationId = currentApplication.getApplicationId();
            final ApplicationType applicationType = currentApplication.getType();

            final String gobAccountNumber = financialImpositionExportDetails.getAccountNumber();
            if (gobAccountNumber != null && isStatDecOrReOpeningApplication(applicationType)) {
                final int divisionCode = checkNotificationRequired.getDivisionCode();
                final LocalDate dateApplicationIsListed = LocalDate.parse(currentApplication.getCourtApplication().getApplicationReceivedDate());
                return Stream.of(new EnforcementPendingApplicationNotificationRequired(state.getCaseId(), applicationId,
                        now(), gobAccountNumber, defendantName, urn, divisionCode, dateApplicationIsListed));
            }
        }
        return Stream.empty();
    }

    private boolean isStatDecOrReOpeningApplication(final ApplicationType applicationType) {
        return applicationType.equals(ApplicationType.STAT_DEC) || applicationType.equals(ApplicationType.REOPENING);
    }
}
