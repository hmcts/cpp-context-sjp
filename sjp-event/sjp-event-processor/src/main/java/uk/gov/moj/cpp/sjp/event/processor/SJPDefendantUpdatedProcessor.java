package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes public.progression.case-defendant-changed events and sends
 * sjp.command.update-defendant-details commands to update defendant details in SJP.
 */
@ServiceComponent(EVENT_PROCESSOR)
public class SJPDefendantUpdatedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SJPDefendantUpdatedProcessor.class);
    private static final String SJP_COMMAND_UPDATE_DEFENDANT_DETAILS_FROM_CC = "sjp.command.update-defendant-details-from-CC";
    public static final String ADDRESS = "address";


    @Inject
    private Sender sender;

    @Handles("public.progression.case-defendant-changed")
    public void handleCaseDefendantChanged(final JsonEnvelope jsonEnvelope) {
        LOGGER.info("Processing public.progression.case-defendant-changed event");

        try {
            final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
            final JsonObject defendant = payload.getJsonObject("defendant");

            if (defendant == null) {
                LOGGER.warn("Defendant object is null in public.progression.case-defendant-changed event");
                return;
            }

            final String defendantId = defendant.getString("id", null);
            final String prosecutionCaseId = defendant.getString("prosecutionCaseId", null);

            if (defendantId == null || prosecutionCaseId == null) {
                LOGGER.warn("Missing required fields: defendantId={}, prosecutionCaseId={}", defendantId, prosecutionCaseId);
                return;
            }

            final JsonObject personDefendant = defendant.getJsonObject("personDefendant");
            final JsonObject legalEntityDefendant = defendant.getJsonObject("legalEntityDefendant");
            
            if (personDefendant == null && legalEntityDefendant == null) {
                LOGGER.info("No personDefendant or legalEntityDefendant found in event, skipping update");
                return;
            }

            final JsonObject commandPayload = buildCommandPayload(defendant, prosecutionCaseId, personDefendant, legalEntityDefendant);

            final JsonEnvelope commandEnvelope = envelopeFrom(
                    metadataFrom(jsonEnvelope.metadata()).withName(SJP_COMMAND_UPDATE_DEFENDANT_DETAILS_FROM_CC),
                    commandPayload);

            LOGGER.info("Sending command {} for caseId={}, defendantId={}",
                    SJP_COMMAND_UPDATE_DEFENDANT_DETAILS_FROM_CC, prosecutionCaseId, defendantId);
            sender.send(commandEnvelope);

        } catch (RuntimeException e) {
            LOGGER.error("Error processing public.progression.case-defendant-changed event", e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("Unexpected error processing public.progression.case-defendant-changed event", e);
            throw new RuntimeException("Failed to process case-defendant-changed event", e);
        }
    }

    private JsonObject buildCommandPayload(final JsonObject defendant, final String prosecutionCaseId, 
                                          final JsonObject personDefendant, final JsonObject legalEntityDefendant) {
        final JsonObjectBuilder payloadBuilder = Json.createObjectBuilder()
                .add("caseId", prosecutionCaseId)
                .add("defendantId", defendant.getString("id"));

        if (personDefendant != null) {
            addPersonDefendantData(payloadBuilder, personDefendant, defendant);
        }

        if (legalEntityDefendant != null) {
            addLegalEntityDefendantData(payloadBuilder, legalEntityDefendant);
        }

        return payloadBuilder.build();
    }

    private void addPersonDefendantData(final JsonObjectBuilder payloadBuilder, final JsonObject personDefendant, 
                                       final JsonObject defendant) {
        final JsonObject personDetails = personDefendant.getJsonObject("personDetails");
        if (personDetails != null) {
            addPersonDetails(payloadBuilder, personDetails);
        }

        addIfPresent(payloadBuilder, personDefendant, "driverNumber", "driverNumber");
        addDefendantAddressIfNeeded(payloadBuilder, personDefendant, defendant);
    }

    private void addPersonDetails(final JsonObjectBuilder payloadBuilder, final JsonObject personDetails) {
        addIfPresent(payloadBuilder, personDetails, "title", "title");
        addIfPresent(payloadBuilder, personDetails, "firstName", "firstName");
        addIfPresent(payloadBuilder, personDetails, "lastName", "lastName");
        addIfPresent(payloadBuilder, personDetails, "dateOfBirth", "dateOfBirth");
        addGenderIfPresent(payloadBuilder, personDetails);
        addIfPresent(payloadBuilder, personDetails, "nationalInsuranceNumber", "nationalInsuranceNumber");
        addIfPresent(payloadBuilder, personDetails, "region", "region");

        addAddressIfPresent(payloadBuilder, personDetails, ADDRESS);
        addContactDetails(payloadBuilder, personDetails);
    }

    private void addContactDetails(final JsonObjectBuilder payloadBuilder, final JsonObject personDetails) {
        final JsonObject contact = personDetails.getJsonObject("contact");
        if (contact == null) {
            return;
        }

        final JsonObjectBuilder contactNumberBuilder = Json.createObjectBuilder();
        addIfPresent(contactNumberBuilder, contact, "home", "home");
        addIfPresent(contactNumberBuilder, contact, "mobile", "mobile");
        addIfPresent(contactNumberBuilder, contact, "work", "business");
        final JsonObject contactNumber = contactNumberBuilder.build();
        if (!contactNumber.isEmpty()) {
            payloadBuilder.add("contactNumber", contactNumber);
        }

        addIfPresent(payloadBuilder, contact, "primaryEmail", "email");
        addIfPresent(payloadBuilder, contact, "secondaryEmail", "email2");
    }

    private void addAddressIfPresent(final JsonObjectBuilder payloadBuilder, final JsonObject source, final String key) {
        final JsonObject address = source.getJsonObject(key);
        if (address != null) {
            payloadBuilder.add(ADDRESS, address);
        }
    }

    private void addDefendantAddressIfNeeded(final JsonObjectBuilder payloadBuilder, final JsonObject personDefendant,
                                            final JsonObject defendant) {
        final JsonObject personDetailsForAddress = personDefendant.getJsonObject("personDetails");
        if (personDetailsForAddress == null || personDetailsForAddress.getJsonObject(ADDRESS) == null) {
            addAddressIfPresent(payloadBuilder, defendant, ADDRESS);
        }
    }

    private void addLegalEntityDefendantData(final JsonObjectBuilder payloadBuilder, final JsonObject legalEntityDefendant) {
        final JsonObjectBuilder legalEntityBuilder = Json.createObjectBuilder();
        
        addIfPresent(legalEntityBuilder, legalEntityDefendant, "name", "name");
        addAddressIfPresent(legalEntityBuilder, legalEntityDefendant, ADDRESS);
        
        final JsonObject contactDetails = legalEntityDefendant.getJsonObject("contactDetails");
        if (contactDetails != null) {
            legalEntityBuilder.add("contactDetails", contactDetails);
        }
        
        addIfPresent(legalEntityBuilder, legalEntityDefendant, "incorporationNumber", "incorporationNumber");
        addIfPresent(legalEntityBuilder, legalEntityDefendant, "position", "position");
        
        final JsonObject legalEntity = legalEntityBuilder.build();
        if (!legalEntity.isEmpty()) {
            payloadBuilder.add("legalEntityDefendant", legalEntity);
        }
    }

    private void addIfPresent(final JsonObjectBuilder builder, final JsonObject source, 
                             final String sourceKey, final String targetKey) {
        if (source.containsKey(sourceKey) && !source.isNull(sourceKey)) {
            try {
                final String value = source.getString(sourceKey, null);
                if (value != null) {
                    builder.add(targetKey, value);
                }
            } catch (ClassCastException e) {
                LOGGER.debug("Field {} is not a string, skipping", sourceKey);
            }
        }
    }

    /**
     * Converts gender from courtsGender format (MALE, FEMALE, etc.) to gender format (Male, Female, etc.)
     */
    private void addGenderIfPresent(final JsonObjectBuilder builder, final JsonObject personDetails) {
        if (personDetails.containsKey("gender") && !personDetails.isNull("gender")) {
            try {
                final String genderValue = personDetails.getString("gender", null);
                if (genderValue != null) {
                    final String convertedGender = convertGenderFromCourtsFormat(genderValue);
                    if (convertedGender != null) {
                        builder.add("gender", convertedGender);
                    }
                }
            } catch (ClassCastException e) {
                LOGGER.debug("Field gender is not a string, skipping");
            }
        }
    }

    /**
     * Converts gender from courtsGender.json enum values to gender.json enum values.
     * courtsGender: MALE, FEMALE, NOT_KNOWN, NOT_SPECIFIED
     * gender: Male, Female, Not Specified
     */
    private String convertGenderFromCourtsFormat(final String courtsGender) {
        if (courtsGender == null) {
            return null;
        }
        switch (courtsGender.toUpperCase()) {
            case "MALE":
                return "Male";
            case "FEMALE":
                return "Female";
            case "NOT_KNOWN":
            case "NOT_SPECIFIED":
                return "Not Specified";
            default:
                LOGGER.warn("Unknown gender value from courts format: {}, using as-is", courtsGender);
                return courtsGender;
        }
    }
}

