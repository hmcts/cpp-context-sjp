package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.HandlerUtils.createRejectionEvents;
import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.HandlerUtils.createRejectionEventsForDefendantUpdate;
import static uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated.DefendantDetailsUpdatedBuilder.defendantDetailsUpdated;
import static uk.gov.moj.cpp.sjp.event.DefendantPendingChangesAccepted.DefendantPendingChangesAcceptedBuilder.defendantPendingChangesAccepted;

import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Person;
import uk.gov.moj.cpp.sjp.domain.PersonalName;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.legalentity.LegalEntityDefendant;
import uk.gov.moj.cpp.sjp.event.CaseNotFound;
import uk.gov.moj.cpp.sjp.event.DefendantAddressUpdateRequested;
import uk.gov.moj.cpp.sjp.event.DefendantAddressUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDateOfBirthUpdateRequested;
import uk.gov.moj.cpp.sjp.event.DefendantDateOfBirthUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDetailUpdateRequested;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdateFailed;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdateRequestAccepted;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdatesAcknowledged;
import uk.gov.moj.cpp.sjp.event.DefendantNameUpdateRequested;
import uk.gov.moj.cpp.sjp.event.DefendantNameUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantNotFound;
import uk.gov.moj.cpp.sjp.event.DefendantPendingChangesAccepted;
import uk.gov.moj.cpp.sjp.event.DefendantPendingChangesRejected;
import uk.gov.moj.cpp.sjp.event.DefendantsNationalInsuranceNumberUpdated;
import uk.gov.moj.cpp.sjp.event.ProsecutionAuthorityAccessDenied;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
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
            final CaseAggregateState state,
            final String userProsecutingAuthority) {

        Object event;
        if (state.getCaseId() == null) {
            LOGGER.warn("Case not found: {}", state.getCaseId());
            event = new CaseNotFound(null, "Acknowledge defendant details updates");
        } else if (defendantId != null && !state.hasDefendant(defendantId)) {
            LOGGER.warn("Defendant not found: {}", defendantId);
            event = new DefendantNotFound(defendantId, "Acknowledge defendant details updates");
        } else if (!(state.getProsecutingAuthority().toUpperCase().startsWith(userProsecutingAuthority) || "ALL".equalsIgnoreCase(userProsecutingAuthority))) {
            event = new ProsecutionAuthorityAccessDenied(userProsecutingAuthority, state.getProsecutingAuthority());
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

        return updateDefendantDetails(userId,caseId,defendantId,person,updatedDate,state,false);
    }

    public Stream<Object> updateDefendantDetails(final UUID userId,
                                                 final UUID caseId,
                                                 final UUID defendantId,
                                                 final Person person,
                                                 final ZonedDateTime updatedDate,
                                                 final CaseAggregateState state, final boolean isAddressUpdateFromApplication) {

        return createRejectionEvents(
                userId,
                "Update defendant detail",
                defendantId,
                state,
                isAddressUpdateFromApplication
        ).orElse(createDefendantUpdateRequestedEvent(caseId, defendantId, person, updatedDate, state, isAddressUpdateFromApplication));
    }

    /**
     * Updates defendant details from Criminal Courts (CC) without checking for case completed
     * or case referred for court hearing status.
     * Returns empty stream if case is not found (no event raised).
     */
    public Stream<Object> updateDefendantDetailsFromCC(

                                                       final UUID caseId,
                                                       final UUID defendantId,
                                                       final Person person,
                                                       final ZonedDateTime updatedDate,
                                                       final CaseAggregateState state) {

        final Optional<Stream<Object>> rejectionEvents = createRejectionEventsForDefendantUpdate(
                "Update defendant detail from CC",
                defendantId,
                state
        );

        if (rejectionEvents.isPresent()) {
            return rejectionEvents.get();
        }

        // If Optional is empty (case not found), return empty stream (no event)
        if (state.getCaseId() == null) {
            return Stream.empty();
        }

        // Otherwise, proceed with creating the update event
        final Stream<Object> events = createDefendantUpdateRequestedEvent(caseId, defendantId, person, updatedDate, state, false);

        // Check if any of the three update-requested events were raised and add the new event
        final List<Object> eventList = events.collect(Collectors.toList());
        boolean hasUpdateRequestedEvent = eventList.stream().anyMatch(e ->
            e instanceof DefendantDateOfBirthUpdateRequested ||
            e instanceof DefendantAddressUpdateRequested ||
            e instanceof DefendantNameUpdateRequested
        );

        if (hasUpdateRequestedEvent) {
            // Extract changed fields from the events
            PersonalName newPersonalName = null;
            String newLegalEntityName = null;
            Address newAddress = null;
            LocalDate newDateOfBirth = null;

            for (Object event : eventList) {
                if (event instanceof DefendantDateOfBirthUpdateRequested defendantDateOfBirthUpdateRequested) {
                    newDateOfBirth = defendantDateOfBirthUpdateRequested.getNewDateOfBirth();
                } else if (event instanceof DefendantAddressUpdateRequested defendantAddressUpdateRequested) {
                    newAddress = defendantAddressUpdateRequested.getNewAddress();
                } else if (event instanceof DefendantNameUpdateRequested nameEvent) {
                    newPersonalName = nameEvent.getNewPersonalName();
                    newLegalEntityName = nameEvent.getNewLegalEntityName();
                }
            }

            // Add the new event to the stream
            return Stream.concat(
                eventList.stream(),
                Stream.of(new DefendantDetailsUpdateRequestAccepted(
                    caseId,
                    defendantId,
                    newPersonalName,
                    newLegalEntityName,
                    newAddress,
                    newDateOfBirth,
                    updatedDate))
            );
        }

        return eventList.stream();
    }

    /**
     * Updates legal entity defendant details from Criminal Courts (CC) without checking for case completed
     * or case referred for court hearing status.
     * Returns empty stream if case is not found (no event raised).
     */
    public Stream<Object> updateLegalEntityDefendantDetailsFromCC(
                                                                  final UUID caseId,
                                                                  final UUID defendantId,
                                                                  final LegalEntityDefendant legalEntityDefendant,
                                                                  final ZonedDateTime updatedDate,
                                                                  final CaseAggregateState state) {

        final Optional<Stream<Object>> rejectionEvents = createRejectionEventsForDefendantUpdate(
                "Update legal entity defendant detail from CC",
                defendantId,
                state
        );

        if (rejectionEvents.isPresent()) {
            return rejectionEvents.get();
        }

        // If Optional is empty (case not found), return empty stream (no event)
        if (state.getCaseId() == null) {
            return Stream.empty();
        }

        // Otherwise, proceed with creating the update event
        final Stream<Object> events = createLegalEntityDefendantUpdateRequestedEvent(caseId, defendantId, legalEntityDefendant, updatedDate, state);

        // Check if any update-requested events were raised and add DefendantDetailsUpdateRequestAccepted
        final List<Object> eventList = events.collect(Collectors.toList());
        boolean hasUpdateRequestedEvent = eventList.stream().anyMatch(e ->
            e instanceof DefendantAddressUpdateRequested ||
            e instanceof DefendantNameUpdateRequested
        );

        if (hasUpdateRequestedEvent) {
            // Extract changed fields from the events
            String newLegalEntityName = null;
            Address newAddress = null;

            for (Object event : eventList) {
                if (event instanceof DefendantAddressUpdateRequested defendantAddressUpdateRequested) {
                    newAddress = defendantAddressUpdateRequested.getNewAddress();
                } else if (event instanceof DefendantNameUpdateRequested nameEvent) {
                    newLegalEntityName = nameEvent.getNewLegalEntityName();
                }
            }

            // Add the new event to the stream
            return Stream.concat(
                eventList.stream(),
                Stream.of(new DefendantDetailsUpdateRequestAccepted(
                    caseId,
                    defendantId,
                    null, // PersonalName is null for legal entity
                    newLegalEntityName,
                    newAddress,
                    null, // DateOfBirth is null for legal entity
                    updatedDate))
            );
        }

        return eventList.stream();
    }

    public Stream<Object> acceptPendingDefendantChangesCC(final UUID caseId,
                                                          final UUID defendantId,
                                                          final Person person,
                                                          final ZonedDateTime updatedDate,
                                                          final CaseAggregateState state) {


        final Optional<Stream<Object>> rejectionEvents = createRejectionEventsForDefendantUpdate(
                "Accept pending defendant changes from CC",
                defendantId,
                state
        );

        if (rejectionEvents.isPresent()) {
            return rejectionEvents.get();
        }

        // If Optional is empty (case not found), return empty stream (no event)
        if (state.getCaseId() == null) {
            return Stream.empty();
        }

        return createDefendantUpdateEvent(caseId, defendantId, person, updatedDate, state);

    }

    public Stream<Object> acceptPendingDefendantChanges(final UUID userId,
                                                        final UUID caseId,
                                                        final UUID defendantId,
                                                        final Person person,
                                                        final ZonedDateTime updatedDate,
                                                        final CaseAggregateState state) {

        return createRejectionEvents(
                userId,
                "Accept pending defendant changes",
                defendantId,
                state
        ).orElse(createDefendantUpdateEvent(caseId, defendantId, person, updatedDate, state));
    }

    public Stream<Object> rejectPendingDefendantChanges(final UUID defendantId,
                                                        final ZonedDateTime updatedDate,
                                                        final CaseAggregateState state) {

        return Stream.of(new DefendantPendingChangesRejected(
                state.getCaseId(),
                defendantId,
                "Defendant pending changes rejected",
                updatedDate));
    }

    private Stream<Object> createDefendantUpdateEvent(final UUID caseId,
                                                      final UUID defendantId,
                                                      final Person person,
                                                      final ZonedDateTime updatedDate,
                                                      final CaseAggregateState state) {

        final Stream.Builder<Object> events = Stream.builder();

        try {
            validateDefendantAddress(person.getAddress(), state.getDefendantAddress());
        } catch (IllegalArgumentException | IllegalStateException e) {
            LOGGER.error("Defendant details update failed for ID: {} with message {} ", defendantId, e);
            return Stream.of(new DefendantDetailsUpdateFailed(caseId, defendantId, e.getMessage()));
        }

        getDefendantWarningEvents(person, updatedDate, state)
                .forEach(events::add);

        final DefendantDetailsUpdated defendantDetailsUpdated = defendantDetailsUpdated()
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .withFirstName(person.getFirstName())
                .withLastName(person.getLastName())
                .withDateOfBirth(person.getDateOfBirth())
                .withAddress(person.getAddress())
                .withUpdateByOnlinePlea(true)
                .withUpdatedDate(updatedDate)
                .withLegalEntityName(person.getLegalEntityName())
                .build();
        events.add(defendantDetailsUpdated);

        final DefendantPendingChangesAccepted defendantPendingChangesAccepted = defendantPendingChangesAccepted()
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .withAcceptedAt(updatedDate)
                .withFirstName(person.getFirstName())
                .withLastName(person.getLastName())
                .withDateOfBirth(person.getDateOfBirth())
                .withAddress(person.getAddress())
                .withLegalEntityName(person.getLegalEntityName())
                .build();
        events.add(defendantPendingChangesAccepted);

        return events.build();
    }

    private Stream<Object> createDefendantUpdateRequestedEvent(final UUID caseId,
                                                               final UUID defendantId,
                                                               final Person person,
                                                               final ZonedDateTime updatedDate,
                                                               final CaseAggregateState state, final boolean isAddressUpdateFromApplication) {

        final Stream.Builder<Object> events = Stream.builder();
        final boolean updatedByOnlinePlea = false;

        try {
            validateDefendantAddress(person.getAddress(), state.getDefendantAddress());
        } catch (IllegalArgumentException | IllegalStateException e) {
            LOGGER.error("Defendant details update failed for ID: {} with message {} ", defendantId, e);
            return Stream.of(new DefendantDetailsUpdateFailed(caseId, defendantId, e.getMessage()));
        }

        getDefendantUpdateRequestedEvents(person, updatedDate, updatedByOnlinePlea, state, isAddressUpdateFromApplication)
                .forEach(events::add);

        final DefendantDetailsUpdated.DefendantDetailsUpdatedBuilder defendantDetailsUpdated = defendantDetailsUpdated()
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .withTitle(person.getTitle())
                .withGender(person.getGender())
                .withNationalInsuranceNumber(person.getNationalInsuranceNumber())
                .withDriverNumber(person.getDriverNumber())
                .withDriverLicenceDetails(person.getDriverLicenceDetails())
                .withContactDetails(person.getContactDetails())
                .withUpdateByOnlinePlea(updatedByOnlinePlea)
                .withUpdatedDate(updatedDate)
                .withRegion(person.getRegion());
        if(isAddressUpdateFromApplication){
            defendantDetailsUpdated.withAddress(person.getAddress());
        }
        events.add(defendantDetailsUpdated.build());

        return events.build();
    }

    private void validateDefendantAddress(final Address address, final Address defendantAddress) {
        if (nonNull(defendantAddress) && nonNull(address)) {
            ensureFieldIsNotBlankIfWasDefined(defendantAddress.getAddress1(), address.getAddress1(),
                    "street (address1) can not be blank as previous value is: " + defendantAddress.getAddress1());
        }
    }

    private void ensureFieldIsNotBlankIfWasDefined(final String oldValue,
                                                   final String newValue,
                                                   final String errorMessage) {

        if (StringUtils.isNotBlank(oldValue) && StringUtils.isBlank(newValue)) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    @SuppressWarnings("squid:MethodCyclomaticComplexity")
    public Stream<Object> getDefendantUpdateRequestedEvents(final Person person,
                                                            final ZonedDateTime updatedDate,
                                                            final boolean isOnlinePlea,
                                                            final CaseAggregateState state) {
        return getDefendantUpdateRequestedEvents(person, updatedDate, isOnlinePlea, state, false);
    }

    @SuppressWarnings("squid:MethodCyclomaticComplexity")
    public Stream<Object> getDefendantUpdateRequestedEvents(final Person person,
                                                            final ZonedDateTime updatedDate,
                                                            final boolean isOnlinePlea,
                                                            final CaseAggregateState state, final boolean addressUpdateFromApplication) {

        final Stream.Builder<Object> events = Stream.builder();

        boolean isDateOfBirthChanged = false;
        boolean isAddressChanged = false;
        boolean isNameChanged = false;
        final LocalDate defendantDateOfBirth = state.getDefendantDateOfBirth();
        if ((defendantDateOfBirth != null && !defendantDateOfBirth.equals(person.getDateOfBirth()))
                || (defendantDateOfBirth == null && person.getDateOfBirth() != null)) {
            isDateOfBirthChanged = true;
            events.add(new DefendantDateOfBirthUpdateRequested(
                    state.getCaseId(),
                    person.getDateOfBirth(),
                    updatedDate));
        }

        final Address defendantAddress = state.getDefendantAddress();
        if (defendantAddress != null && !defendantAddress.equals(person.getAddress())) {
            isAddressChanged = true;
            events.add(new DefendantAddressUpdateRequested(
                    state.getCaseId(),
                    person.getAddress(),
                    updatedDate, addressUpdateFromApplication));
        }

        // Online plea doesn't update title
        final String defendantFirstName = state.getDefendantFirstName();
        final String defendantLastName = state.getDefendantLastName();
        if (!StringUtils.equalsIgnoreCase(defendantFirstName, person.getFirstName()) ||
                !StringUtils.equalsIgnoreCase(defendantLastName, person.getLastName())) {
            isNameChanged = true;
            events.add(new DefendantNameUpdateRequested(
                    state.getCaseId(),
                    new PersonalName(person.getTitle(), person.getFirstName(), person.getLastName()),
                    null,
                    updatedDate));
        } else if (isCompanyNameChanged(person.getLegalEntityName(), state)) {
            isNameChanged = true;
            events.add(new DefendantNameUpdateRequested(
                    state.getCaseId(),
                    null,
                    person.getLegalEntityName(),
                    updatedDate));
        }

        if (isNameChanged || isAddressChanged || isDateOfBirthChanged) {
            events.add(new DefendantDetailUpdateRequested(state.getCaseId(), isNameChanged, isAddressChanged, isDateOfBirthChanged));
        }

        return events.build();
    }

    @SuppressWarnings("squid:MethodCyclomaticComplexity")
    public Stream<Object> getDefendantWarningEvents(final Person person,
                                                    final ZonedDateTime updatedDate,
                                                    final CaseAggregateState state) {

        final Stream.Builder<Object> events = Stream.builder();

        final LocalDate defendantDateOfBirth = state.getDefendantDateOfBirth();
        if (nonNull(defendantDateOfBirth) && nonNull(person.getDateOfBirth()) && !defendantDateOfBirth.equals(person.getDateOfBirth())) {
            events.add(new DefendantDateOfBirthUpdated(
                    state.getCaseId(),
                    defendantDateOfBirth,
                    person.getDateOfBirth(),
                    updatedDate));
        }

        final Address defendantAddress = state.getDefendantAddress();
        if (nonNull(defendantAddress) && nonNull(person.getAddress()) && !defendantAddress.equals(person.getAddress())) {
            events.add(new DefendantAddressUpdated(
                    state.getCaseId(),
                    defendantAddress,
                    person.getAddress(),
                    updatedDate));
        }

        final String defendantFirstName = state.getDefendantFirstName();
        final String defendantLastName = state.getDefendantLastName();
        final String defendantLegalEntityName = state.getDefendantLegalEntityName();
        if (nonNull(person.getFirstName()) && nonNull(person.getLastName()) && (!StringUtils.equalsIgnoreCase(defendantFirstName, person.getFirstName()) ||
                !StringUtils.equalsIgnoreCase(defendantLastName, person.getLastName()))) {

            events.add(new DefendantNameUpdated(
                    state.getCaseId(),
                    new PersonalName(state.getDefendantTitle(), defendantFirstName, defendantLastName),
                    new PersonalName(person.getTitle(), person.getFirstName(), person.getLastName()),
                    null,
                    null,
                    updatedDate));
        } else if (nonNull(person.getLegalEntityName()) && isCompanyNameChanged(person.getLegalEntityName(), state)) {
            events.add(new DefendantNameUpdated(
                    state.getCaseId(),
                    null,
                    null,
                    person.getLegalEntityName(),
                    defendantLegalEntityName,
                    updatedDate));
        }

        return events.build();
    }

    public Stream<Object> getLegalEntityDefendantUpdateRequestedEvents(final LegalEntityDefendant legalEntityDefendant,
                                                                       final ZonedDateTime updatedDate,
                                                                       final CaseAggregateState state) {

        final Stream.Builder<Object> events = Stream.builder();

        boolean isAddressChanged = false;
        boolean isNameChanged = false;

        final Address defendantAddress = state.getDefendantAddress();
        if (defendantAddress != null && !defendantAddress.equals(legalEntityDefendant.getAddress())) {
            isAddressChanged = true;
            events.add(new DefendantAddressUpdateRequested(
                    state.getCaseId(),
                    legalEntityDefendant.getAddress(),
                    updatedDate,false));
        }

        // Online plea doesn't update title
        // For legal entity defendants, check if name changed or if state has no name but new name is provided
        final String defendantLegalEntityName = state.getDefendantLegalEntityName();
        if (isCompanyNameChanged(legalEntityDefendant.getName(), state) ||
                (defendantLegalEntityName == null && legalEntityDefendant.getName() != null)) {
            isNameChanged = true;
            events.add(new DefendantNameUpdateRequested(
                    state.getCaseId(),
                    null,
                    legalEntityDefendant.getName(),
                    updatedDate));
        }

        if (isNameChanged || isAddressChanged) {
            events.add(new DefendantDetailUpdateRequested(state.getCaseId(), isNameChanged, isAddressChanged, false));
        }

        return events.build();
    }

    private Stream<Object> createLegalEntityDefendantUpdateRequestedEvent(final UUID caseId,
                                                                          final UUID defendantId,
                                                                          final LegalEntityDefendant legalEntityDefendant,
                                                                          final ZonedDateTime updatedDate,
                                                                          final CaseAggregateState state) {

        final Stream.Builder<Object> events = Stream.builder();

        try {
            validateDefendantAddress(legalEntityDefendant.getAddress(), state.getDefendantAddress());
        } catch (IllegalArgumentException | IllegalStateException e) {
            LOGGER.error("Legal entity defendant details update failed for ID: {} with message {} ", defendantId, e);
            return Stream.of(new DefendantDetailsUpdateFailed(caseId, defendantId, e.getMessage()));
        }

        getLegalEntityDefendantUpdateRequestedEvents(legalEntityDefendant, updatedDate, state)
                .forEach(events::add);

        final DefendantDetailsUpdated.DefendantDetailsUpdatedBuilder defendantDetailsUpdated = defendantDetailsUpdated()
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .withLegalEntityName(legalEntityDefendant.getName())
                .withAddress(legalEntityDefendant.getAddress())
                .withContactDetails(legalEntityDefendant.getContactDetails())
                .withUpdateByOnlinePlea(false)
                .withUpdatedDate(updatedDate);

        events.add(defendantDetailsUpdated.build());

        return events.build();
    }

    private boolean isCompanyNameChanged(final String legalEntityName, final CaseAggregateState state) {

        final String defendantLegalEntityName = state.getDefendantLegalEntityName();

        return defendantLegalEntityName != null && !defendantLegalEntityName.equals(legalEntityName);
    }
}
