package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.HandlerUtils.createRejectionEvents;
import static uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated.DefendantDetailsUpdatedBuilder.defendantDetailsUpdated;

import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Person;
import uk.gov.moj.cpp.sjp.domain.PersonalName;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseNotFound;
import uk.gov.moj.cpp.sjp.event.DefendantAddressUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDateOfBirthUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdateFailed;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdatesAcknowledged;
import uk.gov.moj.cpp.sjp.event.DefendantNotFound;
import uk.gov.moj.cpp.sjp.event.DefendantPersonalNameUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantsNationalInsuranceNumberUpdated;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaseDefendantHandler {

    public static final CaseDefendantHandler INSTANCE = new CaseDefendantHandler();

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseDefendantHandler.class);

    private CaseDefendantHandler() {
    }

    public Stream<Object> updateDefendantNationalInsuranceNumber(final UUID userId,
                                                                 final UUID defendantId,
                                                                 final String newNationalInsuranceNumber,
                                                                 final CaseAggregateState state) {

        return createRejectionEvents(
                userId,
                "Update national insurance number",
                defendantId,
                state
        ).orElse(Stream.of(new DefendantsNationalInsuranceNumberUpdated(state.getCaseId(), defendantId, newNationalInsuranceNumber)));
    }

    public Stream<Object> acknowledgeDefendantDetailsUpdates(
            final UUID defendantId,
            final ZonedDateTime acknowledgedAt,
            final CaseAggregateState state) {

        Object event;
        if (state.getCaseId() == null) {
            LOGGER.warn("Case not found: {}", state.getCaseId());
            event = new CaseNotFound(null, "Acknowledge defendant details updates");
        } else if (defendantId != null && !state.hasDefendant(defendantId)) {
            LOGGER.warn("Defendant not found: {}", defendantId);
            event = new DefendantNotFound(defendantId, "Acknowledge defendant details updates");
        } else {
            event = new DefendantDetailsUpdatesAcknowledged(state.getCaseId(), defendantId, acknowledgedAt);
        }

        return Stream.of(event);
    }

    public Stream<Object> updateDefendantDetails(final UUID userId,
                                                 final UUID caseId,
                                                 final UUID defendantId,
                                                 final Person person,
                                                 final ZonedDateTime updatedDate,
                                                 final CaseAggregateState state) {

        return createRejectionEvents(
                userId,
                "Update defendant detail",
                defendantId,
                state
        ).orElse(createDefendantUpdateEvent(caseId, defendantId, person, updatedDate, state));
    }

    private Stream<Object> createDefendantUpdateEvent(final UUID caseId,
                                                      final UUID defendantId,
                                                      final Person person,
                                                      final ZonedDateTime updatedDate,
                                                      final CaseAggregateState state) {

        final Stream.Builder<Object> events = Stream.builder();
        final boolean updatedByOnlinePlea = false;

        try {
            validateDefendantAddress(person.getAddress(), state.getDefendantAddress());
        } catch (IllegalArgumentException | IllegalStateException e) {
            LOGGER.error("Defendant details update failed for ID: {} with message {} ", defendantId, e);
            return Stream.of(new DefendantDetailsUpdateFailed(caseId, defendantId, e.getMessage()));
        }

        getDefendantWarningEvents(person, updatedDate, updatedByOnlinePlea, state)
                .forEach(events::add);

        final DefendantDetailsUpdated defendantDetailsUpdated = defendantDetailsUpdated()
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .withTitle(person.getTitle())
                .withFirstName(person.getFirstName())
                .withLastName(person.getLastName())
                .withDateOfBirth(person.getDateOfBirth())
                .withGender(person.getGender())
                .withNationalInsuranceNumber(person.getNationalInsuranceNumber())
                .withDriverNumber(person.getDriverNumber())
                .withDriverLicenceDetails(person.getDriverLicenceDetails())
                .withContactDetails(person.getContactDetails())
                .withAddress(person.getAddress())
                .withUpdateByOnlinePlea(updatedByOnlinePlea)
                .withUpdatedDate(updatedDate)
                .withRegion(person.getRegion())
                .build();
        events.add(defendantDetailsUpdated);

        return events.build();
    }

    private void validateDefendantAddress(final Address address, final Address defendantAddress) {
        if (defendantAddress != null) {
            ensureFieldIsNotBlankIfWasDefined(defendantAddress.getAddress1(), address.getAddress1(),
                    "street (address1) can not be blank as previous value is: " + defendantAddress.getAddress1());

            ensureFieldIsNotBlankIfWasDefined(defendantAddress.getAddress3(), address.getAddress3(),
                    "town (address3) can not be blank as previous value is: " + defendantAddress.getAddress3());
        }
    }

    private void ensureFieldIsNotBlankIfWasDefined(final String oldValue,
                                                   final String newValue,
                                                   final String errorMessage) {

        if (StringUtils.isNotBlank(oldValue) && StringUtils.isBlank(newValue)) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public Stream<Object> getDefendantWarningEvents(final Person person,
                                                    final ZonedDateTime updatedDate,
                                                    final boolean isOnlinePlea,
                                                    final CaseAggregateState state) {

        final Stream.Builder<Object> events = Stream.builder();

        final LocalDate defendantDateOfBirth = state.getDefendantDateOfBirth();
        if (defendantDateOfBirth != null && !defendantDateOfBirth.equals(person.getDateOfBirth())) {
            events.add(new DefendantDateOfBirthUpdated(
                    state.getCaseId(),
                    defendantDateOfBirth,
                    person.getDateOfBirth(),
                    updatedDate));
        }

        final Address defendantAddress = state.getDefendantAddress();
        if (defendantAddress != null && !defendantAddress.equals(person.getAddress())) {
            events.add(new DefendantAddressUpdated(
                    state.getCaseId(),
                    defendantAddress,
                    person.getAddress(),
                    updatedDate));
        }

        // Online plea doesn't update title
        final String defendantFirstName = state.getDefendantFirstName();
        final String defendantLastName = state.getDefendantLastName();

        if (isTitleChanged(isOnlinePlea, person.getTitle(), state) ||
                !StringUtils.equalsIgnoreCase(defendantFirstName, person.getFirstName()) ||
                !StringUtils.equalsIgnoreCase(defendantLastName, person.getLastName())) {

            events.add(new DefendantPersonalNameUpdated(
                    state.getCaseId(),
                    new PersonalName(state.getDefendantTitle(), defendantFirstName, defendantLastName),
                    new PersonalName(person.getTitle(), person.getFirstName(), person.getLastName()),
                    updatedDate));
        }

        return events.build();
    }

    private boolean isTitleChanged(final boolean isOnlinePlea,
                                   final String title,
                                   final CaseAggregateState state) {

        final String defendantTitle = state.getDefendantTitle();

        return !isOnlinePlea && defendantTitle != null && !defendantTitle.equalsIgnoreCase(title);
    }
}
