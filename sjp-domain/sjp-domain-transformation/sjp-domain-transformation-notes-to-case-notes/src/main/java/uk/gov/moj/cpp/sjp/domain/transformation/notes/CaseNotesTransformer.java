package uk.gov.moj.cpp.sjp.domain.transformation.notes;

import static java.util.UUID.randomUUID;
import static java.util.stream.Stream.of;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;
import uk.gov.moj.cpp.sjp.domain.transformation.connection.ResultingService;
import uk.gov.moj.cpp.sjp.domain.transformation.connection.UsersAndGroupService;

import java.util.Optional;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@Transformation
public class CaseNotesTransformer implements EventTransformation {

    public static final String CASE_COMPLETED_EVENT = "sjp.events.case-completed";
    public static final String CASE_NOTE_ADDED_EVENT = "sjp.events.case-note-added";

    private UsersAndGroupService usersAndGroupService = new UsersAndGroupService();
    private ResultingService resultingService = new ResultingService();


    @Override
    public Action actionFor(final JsonEnvelope event) {
        return CASE_COMPLETED_EVENT.equalsIgnoreCase(event.metadata().name()) ? TRANSFORM : NO_ACTION;
    }

    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope event) {
        final String caseId = event.payloadAsJsonObject().getString("caseId");
        final Optional<CaseDecisionDetails> caseDecisionDetails = resultingService.getCaseDecisionFor(caseId);
        final Optional<JsonEnvelope> caseNoteAddedEnvelope = caseDecisionDetails.map(caseDecision -> this.buildCaseNoteAddedEvent(caseDecision, caseId)).map(caseNote -> {
            final MetadataBuilder metadataBuilder = metadataFrom(event.metadata());
            metadataBuilder.withId(randomUUID());
            metadataBuilder.withName(CASE_NOTE_ADDED_EVENT);
            return envelopeFrom(metadataBuilder.build(), caseNote);
        });
        return caseNoteAddedEnvelope.isPresent() ? of(event, caseNoteAddedEnvelope.get()) : of(event);
    }

    private JsonObject buildCaseNoteAddedEvent(final CaseDecisionDetails caseDecisionDetails, final String caseId) {
        final UserDetails userDetails = usersAndGroupService.getUserDetails(caseDecisionDetails.getUserId());
        final JsonObjectBuilder caseNoteAddedEventBuilder = Json.createObjectBuilder();

        final JsonObjectBuilder noteBuilder = Json.createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("text", caseDecisionDetails.getDecisionNotes())
                .add("type", caseDecisionDetails.getNoteType())
                .add("addedAt", caseDecisionDetails.getCreatedAt());

        final JsonObjectBuilder authorBuilder = Json.createObjectBuilder()
                .add("userId", caseDecisionDetails.getUserId())
                .add("firstName", userDetails.getFirstName())
                .add("lastName", userDetails.getLastName());


        caseNoteAddedEventBuilder.add("caseId", caseId);
        caseNoteAddedEventBuilder.add("decisionId", caseDecisionDetails.getDecisionId().toString());
        caseNoteAddedEventBuilder.add("note", noteBuilder.build());
        caseNoteAddedEventBuilder.add("author", authorBuilder.build());

        final JsonObject jsonObject = caseNoteAddedEventBuilder.build();
        return jsonObject;
    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        //Not used
    }

}
