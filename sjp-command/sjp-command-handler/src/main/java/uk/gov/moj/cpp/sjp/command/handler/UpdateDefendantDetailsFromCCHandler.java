package uk.gov.moj.cpp.sjp.command.handler;

import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;
import uk.gov.moj.cpp.sjp.domain.Person;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.domain.legalentity.LegalEntityDefendant;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

/**
 * Handler for updating defendant details from Criminal Courts (CC).
 * This handler bypasses checks for case completed and case referred for court hearing.
 */
@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateDefendantDetailsFromCCHandler extends BasePersonInfoHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private Enveloper enveloper;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Clock clock;

    @Handles("sjp.command.update-defendant-details-from-CC")
    public void updateDefendantDetailsFromCC(final JsonEnvelope command) throws EventStreamException {

        final ZonedDateTime createdAt = command.metadata().createdAt().orElseGet(clock::now);

        final JsonObject payload = command.payloadAsJsonObject();

        final UUID caseId = UUID.fromString(payload.getString("caseId"));
        final UUID defendantId = UUID.fromString(payload.getString("defendantId"));

        final String title = getStringOrNull(payload, "title");
        final String firstName = getStringOrNull(payload, "firstName");
        final String lastName = getStringOrNull(payload, "lastName");
        final String driverNumber = getStringOrNull(payload, "driverNumber");
        final String driverLicenceDetails = getStringOrNull(payload, "driverLicenceDetails");
        final Gender gender = Gender.valueFor(getStringOrNull(payload, "gender")).orElse(null);
        final String nationalInsuranceNumber = getStringOrNull(payload, "nationalInsuranceNumber");
        final String dateOfBirth = getStringOrNull(payload, "dateOfBirth");
        final String email = getStringOrNull(payload, "email");
        final String email2 = getStringOrNull(payload, "email2");
        String legalEntityName = getStringOrNull(payload, "legalEntityName");

        final JsonObject contactNumberPayload = payload.getJsonObject("contactNumber");
        final String homeNumber = contactNumberPayload != null ? getStringOrNull(contactNumberPayload, "home") : null;
        final String mobileNumber = contactNumberPayload != null ? getStringOrNull(contactNumberPayload, "mobile") : null;
        final String businessNumber = contactNumberPayload != null ? getStringOrNull(contactNumberPayload, "business") : null;
        final Address address = createAddressFrom(payload);
        final LocalDate birthDate = dateOfBirth == null ? null : LocalDate.parse(dateOfBirth);
        final String region = getStringOrNull(payload, "region");

        final EventStream eventStream = eventSource.getStreamById(caseId);

        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);

        final JsonObject legalEntityDefendantPayload = payload.getJsonObject("legalEntityDefendant");
        
        if (legalEntityDefendantPayload != null) {
            // Handle legal entity defendant
            legalEntityName = getStringOrNull(legalEntityDefendantPayload, "name");
            final Address legalEntityAddress = createAddressFrom(legalEntityDefendantPayload);
            final String incorporationNumber = getStringOrNull(legalEntityDefendantPayload, "incorporationNumber");
            final String position = getStringOrNull(legalEntityDefendantPayload, "position");
            
            final JsonObject legalEntityContactDetails = legalEntityDefendantPayload.getJsonObject("contactDetails");
            final String legalEntityHome = legalEntityContactDetails != null ? getStringOrNull(legalEntityContactDetails, "home") : null;
            final String legalEntityMobile = legalEntityContactDetails != null ? getStringOrNull(legalEntityContactDetails, "mobile") : null;
            final String legalEntityBusiness = legalEntityContactDetails != null ? getStringOrNull(legalEntityContactDetails, "business") : null;
            final String legalEntityEmail = legalEntityContactDetails != null ? getStringOrNull(legalEntityContactDetails, "email") : null;
            final String legalEntityEmail2 = legalEntityContactDetails != null ? getStringOrNull(legalEntityContactDetails, "email2") : null;
            
            final ContactDetails legalEntityContact = new ContactDetails(legalEntityHome, legalEntityMobile, legalEntityBusiness, legalEntityEmail, legalEntityEmail2);
            final LegalEntityDefendant legalEntityDefendant = LegalEntityDefendant.legalEntityDefendant()
                    .withName(legalEntityName)
                    .withAdddres(legalEntityAddress)
                    .withContactDetails(legalEntityContact)
                    .withIncorporationNumber(incorporationNumber)
                    .withPosition(position)
                    .build();
            
            final Stream<Object> events = caseAggregate.updateLegalEntityDefendantDetailsFromCC(caseId, defendantId, legalEntityDefendant, createdAt);
            eventStream.append(events.map(toEnvelopeWithMetadataFrom(command)));
        } else {
            // Handle person defendant
            final ContactDetails contactDetails = new ContactDetails(homeNumber, mobileNumber, businessNumber, email, email2);
            final Person person = new Person(title, firstName, lastName, birthDate, gender, nationalInsuranceNumber, driverNumber, driverLicenceDetails, address, contactDetails, region, legalEntityName);

            final Stream<Object> events = caseAggregate.updateDefendantDetailsFromCC(caseId, defendantId, person, createdAt);
            eventStream.append(events.map(toEnvelopeWithMetadataFrom(command)));
        }
    }
}

