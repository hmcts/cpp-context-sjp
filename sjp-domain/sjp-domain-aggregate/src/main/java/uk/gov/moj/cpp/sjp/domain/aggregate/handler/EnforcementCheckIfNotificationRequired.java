package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.time.ZonedDateTime.now;
import static java.util.Objects.nonNull;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationType;
import uk.gov.moj.cpp.sjp.domain.EnforcementPendingApplicationRequiredNotification;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.Application;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.FinancialImpositionExportDetails;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationRequired;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EnforcementCheckIfNotificationRequired {

    public static final EnforcementCheckIfNotificationRequired INSTANCE = new EnforcementCheckIfNotificationRequired();
    public static final String EMPTY_STRING = "";

    private EnforcementCheckIfNotificationRequired() {
    }

    public Stream<Object> checkIfPendingApplicationToNotified(CaseAggregateState state,
                                                              final EnforcementPendingApplicationRequiredNotification checkNotificationRequired) {
        final UUID defendantId = state.getDefendantId();
        final FinancialImpositionExportDetails financialImpositionExportDetails
                = state.getDefendantFinancialImpositionExportDetails().get(defendantId);

        if (state.hasPendingApplication() && null != financialImpositionExportDetails) {
            final Application currentApplication = state.getCurrentApplication();
            final ApplicationType applicationType = currentApplication.getType();
            final String gobAccountNumber = financialImpositionExportDetails.getAccountNumber();
            if (gobAccountNumber != null && isStatDecOrReOpeningApplication(applicationType)) {
                final int divisionCode = checkNotificationRequired.getDivisionCode();
                final EnforcementPendingApplicationNotificationRequired event =
                        enrichEnforcementPendingApplicationNotificationRequired(gobAccountNumber, divisionCode, state);
                return Stream.of(event);
            }
        }
        return Stream.empty();
    }

    private EnforcementPendingApplicationNotificationRequired enrichEnforcementPendingApplicationNotificationRequired(final String gobAccountNumber,
                                                                                                                      final int divisionCode,
                                                                                                                      final CaseAggregateState state) {
        String dateOfBirth = EMPTY_STRING;
        final String defendantAddress;
        final String defendantEmail;
        final String defendantContactNumber;
        if (isOrganisationDefendant(state.getCurrentApplication().getCourtApplication())) {
            final Organisation org = state.getCurrentApplication().getCourtApplication().getSubject().getMasterDefendant()
                    .getLegalEntityDefendant().getOrganisation();
            defendantAddress = getFullAddressAsString(org.getAddress());
            defendantEmail = getDefendantEmail(org.getContact());
            defendantContactNumber = getDefendantContactNumber(org.getContact());
        } else {
            final Person person = state.getCurrentApplication().getCourtApplication().getSubject().getMasterDefendant()
                    .getPersonDefendant().getPersonDetails();
            defendantAddress = getFullAddressAsString(person.getAddress());
            dateOfBirth = person.getDateOfBirth();
            defendantEmail = getDefendantEmail(person.getContact());
            defendantContactNumber = getDefendantContactNumber(person.getContact());
        }
        final LocalDate dateApplicationIsListed = LocalDate.parse(state.getCurrentApplication().getCourtApplication().getApplicationReceivedDate());
        final String defendantName = state.getDefendantFirstName() + " " + state.getDefendantLastName();
        return new EnforcementPendingApplicationNotificationRequired(
                state.getCaseId(),
                state.getCurrentApplication().getApplicationId(), now(),
                gobAccountNumber, defendantName,
                state.getUrn(), divisionCode,
                dateApplicationIsListed, defendantAddress,
                dateOfBirth, defendantEmail, getOriginalDateOfSentence(state), defendantContactNumber);

    }

    private static String getOriginalDateOfSentence(final CaseAggregateState state) {
        String originalDateOfSentence = EMPTY_STRING;
        if (nonNull(state.getSavedAt())) {
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            originalDateOfSentence = state.getSavedAt().format(formatter);
        }
        return originalDateOfSentence;
    }


    private static String getDefendantContactNumber(final ContactNumber contactNumber) {
        if (nonNull(contactNumber)) {
            if (nonNull(contactNumber.getHome())) {
                return contactNumber.getHome();
            }
            if (nonNull(contactNumber.getMobile())) {
                return contactNumber.getMobile();
            }
        }
        return EMPTY_STRING;
    }

    private static String getDefendantEmail(final ContactNumber contactNumber) {
        return nonNull(contactNumber) && nonNull(contactNumber.getPrimaryEmail()) ? contactNumber.getPrimaryEmail() : EMPTY_STRING;
    }

    public boolean isOrganisationDefendant(final CourtApplication courtApplication) {
        return nonNull(courtApplication.getSubject())
                && nonNull(courtApplication.getSubject().getMasterDefendant())
                && nonNull(courtApplication.getSubject().getMasterDefendant().getLegalEntityDefendant());
    }

    public String getFullAddressAsString(final Address address) {
        if (nonNull(address)) {
            final List<String> addressLines = Arrays.asList(address.getAddress1(), address.getAddress2(), address.getAddress3(),
                    address.getAddress4(), address.getAddress5(), address.getPostcode());
            return addressLines.stream()
                    .filter(line -> line != null && !line.isEmpty())
                    .collect(Collectors.joining(" "));
        } else {
            return EMPTY_STRING;
        }
    }

    private boolean isStatDecOrReOpeningApplication(final ApplicationType applicationType) {
        return applicationType.equals(ApplicationType.STAT_DEC) || applicationType.equals(ApplicationType.REOPENING);
    }
}
