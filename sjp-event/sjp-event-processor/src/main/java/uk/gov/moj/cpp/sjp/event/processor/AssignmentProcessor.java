package uk.gov.moj.cpp.sjp.event.processor;


import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants.ASSIGNMENT_DOMAIN_OBJECT_ID;
import static uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants.ASSIGNMENT_NATURE_TYPE;
import static uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants.CASE_ASSIGNMENT_TYPE;
import static uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants.CASE_ID;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class AssignmentProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssignmentProcessor.class.getCanonicalName());

    static final String ASSIGNMENT_CONTEXT_ASSIGNMENT_CREATED = "assignment.assignment-created";
    static final String ASSIGNMENT_CONTEXT_ASSIGNMENT_DELETED = "assignment.assignment-deleted";

    static final String SJP_COMMAND_HANDLER_ASSIGNMENT_CREATED = "sjp.command.case-assignment-created";
    static final String SJP_COMMAND_HANDLER_ASSIGNMENT_DELETED = "sjp.command.case-assignment-deleted";

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Handles(ASSIGNMENT_CONTEXT_ASSIGNMENT_CREATED)
    public void handleAssignmentCreated(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final String id = payload.getString(ASSIGNMENT_DOMAIN_OBJECT_ID, "NULL");
        LOGGER.debug("Received assignment created message for id: {}", id);

        final String caseAssignmentTypeString = payload.getString(ASSIGNMENT_NATURE_TYPE, "NULL");
        final Optional<CaseAssignmentType> caseAssignmentType = CaseAssignmentType.from(caseAssignmentTypeString);

        if (!caseAssignmentType.isPresent()) {
            LOGGER.debug("Ignoring non-ATCM assignment creation type: {}", caseAssignmentTypeString);
            return;
        }

        sender.send(
                createEvent(SJP_COMMAND_HANDLER_ASSIGNMENT_CREATED, envelope, id, caseAssignmentType.get())
        );
    }

    @Handles(ASSIGNMENT_CONTEXT_ASSIGNMENT_DELETED)
    public void handleAssignmentDeleted(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final String id = payload.getString(ASSIGNMENT_DOMAIN_OBJECT_ID, "NULL");
        LOGGER.debug("Received assignment deleted message for id: {}", id);

        final String caseAssignmentTypeString = payload.getString(ASSIGNMENT_NATURE_TYPE, "NULL");
        final Optional<CaseAssignmentType> caseAssignmentType = CaseAssignmentType.from(caseAssignmentTypeString);

        if (!caseAssignmentType.isPresent()) {
            LOGGER.debug("Ignoring non-ATCM assignment deletion type: {}", caseAssignmentTypeString);
            return;
        }

        sender.send(
                createEvent(SJP_COMMAND_HANDLER_ASSIGNMENT_DELETED, envelope, id, caseAssignmentType.get())
        );
    }

    private JsonEnvelope createEvent(final String command, final JsonEnvelope envelope, final String id, final CaseAssignmentType caseAssignmentType) {
        final JsonObject publicEventPayload = Json.createObjectBuilder()
                .add(CASE_ID, id)
                .add(CASE_ASSIGNMENT_TYPE, caseAssignmentType.toString())
                .build();

        return enveloper.withMetadataFrom(envelope, command).apply(publicEventPayload);
    }
}
