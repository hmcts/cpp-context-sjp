package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;

import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class CaseNoteAddedProcessor {

    public static final String CASE_NOTE_ADDED_PUBLIC_EVENT_NAME = "public.sjp.case-note-added";

    private static final String AUTHOR = "author";
    private static final String NOTE = "note";

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    protected Sender sender;

    @Handles("sjp.events.case-note-added")
    public void handleCaseNoteAdded(final JsonEnvelope event) {
        final JsonObject eventPayload = event.payloadAsJsonObject();
        final UUID caseId = UUID.fromString(eventPayload.getString(CASE_ID));
        final String authorId = eventPayload.getJsonObject(AUTHOR).getString("userId");
        final JsonObject notePayload = eventPayload.getJsonObject(NOTE);
        final UUID noteId = UUID.fromString(notePayload.getString("id"));
        final String noteType = notePayload.getString("type");
        final String addedAt = notePayload.getString("addedAt");

        raisePublicEvent(event.metadata(), caseId, authorId, noteId, noteType, addedAt);
    }

    private void raisePublicEvent(final Metadata metadata, final UUID caseId, final String authorId,
                                  final UUID noteId, final String noteType, final String addedAt) {
        final Metadata publicEventMetadata = metadataFrom(metadata)
                .withName(CASE_NOTE_ADDED_PUBLIC_EVENT_NAME)
                .build();

        final JsonObject publicEventPayload = createObjectBuilder()
                .add("id", caseId.toString())
                .add("authorId", authorId)
                .add("noteId", noteId.toString())
                .add("noteType", noteType)
                .add("addedAt", addedAt)
                .build();

        sender.send(envelopeFrom(publicEventMetadata, publicEventPayload));
    }
}
