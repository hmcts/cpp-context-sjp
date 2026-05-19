package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes sjp.events.defendant-details-update-request-accepted events and sends
 * sjp.command.accept-pending-defendant-changes commands to accept the pending defendant changes.
 */
@ServiceComponent(EVENT_PROCESSOR)
public class DefendantDetailsUpdateRequestAcceptedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantDetailsUpdateRequestAcceptedProcessor.class);
    private static final String SJP_COMMAND_ACCEPT_PENDING_DEFENDANT_CHANGES = "sjp.command.accept-pending-defendant-changes-from-CC";
    private static final String CASE_ID = "caseId";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String NEW_LEGAL_ENTITY_NAME = "newLegalEntityName";
    private static final String NEW_DATE_OF_BIRTH = "newDateOfBirth";
    public static final String NEW_PERSONAL_NAME = "newPersonalName";
    public static final String NEW_ADDRESS = "newAddress";

    @Inject
    private Sender sender;

    @Handles("sjp.events.defendant-details-update-request-accepted")
    public void handleDefendantDetailsUpdateRequestAccepted(final JsonEnvelope envelope) {
        LOGGER.info("Processing sjp.events.defendant-details-update-request-accepted event");

        try {
            final JsonObject payload = envelope.payloadAsJsonObject();
            final String caseId = payload.getString(CASE_ID, null);
            final String defendantId = payload.getString(DEFENDANT_ID, null);

            if (caseId == null || defendantId == null) {
                LOGGER.warn("Missing required fields: caseId={}, defendantId={}", caseId, defendantId);
                return;
            }

            final JsonObject commandPayload = buildCommandPayload(payload);

            final JsonEnvelope commandEnvelope = envelopeFrom(
                    metadataFrom(envelope.metadata()).withName(SJP_COMMAND_ACCEPT_PENDING_DEFENDANT_CHANGES).build(),
                    commandPayload);

            LOGGER.info("Sending command {} for caseId={}, defendantId={}",
                    SJP_COMMAND_ACCEPT_PENDING_DEFENDANT_CHANGES, caseId, defendantId);
            sender.send(commandEnvelope);

        } catch (RuntimeException e) {
            LOGGER.error("Error processing sjp.events.defendant-details-update-request-accepted event", e);
            throw e;
        }
    }

    private JsonObject buildCommandPayload(final JsonObject eventPayload) {
        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add(CASE_ID, eventPayload.getString(CASE_ID))
                .add(DEFENDANT_ID, eventPayload.getString(DEFENDANT_ID));

        addPersonalNameIfPresent(payloadBuilder, eventPayload);
        addLegalEntityNameIfPresent(payloadBuilder, eventPayload);
        addAddressIfPresent(payloadBuilder, eventPayload);
        addDateOfBirthIfPresent(payloadBuilder, eventPayload);

        return payloadBuilder.build();
    }

    private void addPersonalNameIfPresent(final JsonObjectBuilder payloadBuilder, final JsonObject eventPayload) {
        if (!isPresentAndNotNull(eventPayload, NEW_PERSONAL_NAME)) {
            return;
        }
        final JsonObject newPersonalName = eventPayload.getJsonObject(NEW_PERSONAL_NAME);
        if (newPersonalName != null) {
            addIfPresent(payloadBuilder, newPersonalName, "firstName", "firstName");
            addIfPresent(payloadBuilder, newPersonalName, "lastName", "lastName");
        }
    }

    private void addLegalEntityNameIfPresent(final JsonObjectBuilder payloadBuilder, final JsonObject eventPayload) {
        if (!isPresentAndNotNull(eventPayload, NEW_LEGAL_ENTITY_NAME)) {
            return;
        }
        final String legalEntityName = eventPayload.getString(NEW_LEGAL_ENTITY_NAME, null);
        if (legalEntityName != null) {
            payloadBuilder.add("legalEntityName", legalEntityName);
        }
    }

    private void addAddressIfPresent(final JsonObjectBuilder payloadBuilder, final JsonObject eventPayload) {
        if (!isPresentAndNotNull(eventPayload, NEW_ADDRESS)) {
            return;
        }
        final JsonObject newAddress = eventPayload.getJsonObject(NEW_ADDRESS);
        if (newAddress != null) {
            payloadBuilder.add("address", newAddress);
        }
    }

    private void addDateOfBirthIfPresent(final JsonObjectBuilder payloadBuilder, final JsonObject eventPayload) {
        if (!isPresentAndNotNull(eventPayload, NEW_DATE_OF_BIRTH)) {
            return;
        }
        final String dateOfBirth = eventPayload.getString(NEW_DATE_OF_BIRTH, null);
        if (dateOfBirth != null) {
            payloadBuilder.add("dateOfBirth", dateOfBirth);
        }
    }

    private boolean isPresentAndNotNull(final JsonObject jsonObject, final String key) {
        return jsonObject.containsKey(key) && !jsonObject.isNull(key);
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
}

