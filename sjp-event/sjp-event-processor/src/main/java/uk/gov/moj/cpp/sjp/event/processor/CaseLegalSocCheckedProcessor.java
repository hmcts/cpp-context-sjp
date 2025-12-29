package uk.gov.moj.cpp.sjp.event.processor;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;

import uk.gov.justice.json.schemas.domains.sjp.NoteType;
import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.processor.service.UsersGroupsService;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(EVENT_PROCESSOR)
public class CaseLegalSocCheckedProcessor {

    public static final String CASE_LEGAL_SOC_CHECKED_PUBLIC_EVENT_NAME = "public.sjp.case-legal-soc-checked";
    public static final String CHECKED_BY = "checkedBy";
    public static final String CHECKED_AT = "checkedAt";
    public static final String AUTHOR = "author";
    public static final String USER_ID = "userId";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    protected Sender sender;

    @Inject
    private UsersGroupsService usersGroupsService;

    @Inject
    private Enveloper enveloper;

    @Handles("sjp.events.marked-as-legal-soc-checked")
    public void handleCaseLegalSocChecked(final JsonEnvelope socCheckedEvent) {
        final JsonObject socCheckedEventPayload = socCheckedEvent.payloadAsJsonObject();
        final UUID caseId = UUID.fromString(socCheckedEventPayload.getString(CASE_ID));
        final UUID checkedBy = UUID.fromString(socCheckedEventPayload.getString(CHECKED_BY));
        final String checkedAt = socCheckedEventPayload.getString(CHECKED_AT);

        final JsonEnvelope emptyEnvelope = JsonEnvelope.envelopeFrom(Envelope.metadataFrom(socCheckedEvent.metadata()), NULL);

        addNoteCommand(socCheckedEvent, caseId, checkedBy, checkedAt, emptyEnvelope);

        raisePublicEvent(socCheckedEvent.metadata(), caseId, checkedBy, checkedAt);
    }

    @SuppressWarnings("squid:CallToDeprecatedMethod")
    private void addNoteCommand(final JsonEnvelope socCheckedEvent, final UUID caseId, final UUID checkedBy, final String checkedAt, final JsonEnvelope emptyEnvelope){
        final JsonObject checkedByUserDetails = usersGroupsService.getUserDetails(checkedBy, emptyEnvelope);
        final String socCheckedNoteText = getSocCheckedNoteText(checkedByUserDetails, checkedAt);

        final JsonObjectBuilder notePayload = createObjectBuilder()
                        .add(CASE_ID, caseId.toString())
                        .add(AUTHOR, createObjectBuilder()
                                .add(USER_ID, checkedBy.toString())
                                .add(FIRST_NAME, checkedByUserDetails.getString(FIRST_NAME))
                                .add(LAST_NAME, checkedByUserDetails.getString(LAST_NAME))
                        )
                        .add("note", createObjectBuilder()
                                .add("id", randomUUID().toString())
                                .add("text", socCheckedNoteText)
                                .add("type", NoteType.SOC_CHECK.toString())
                                .add("addedAt", checkedAt)
                        );

        sender.send(enveloper.withMetadataFrom(socCheckedEvent, "sjp.command.add-case-note").apply(notePayload.build()));
    }

    private void raisePublicEvent(final Metadata metadata, final UUID caseId, final UUID checkedBy, final String checkedAt) {
        final Metadata publicEventMetadata = metadataFrom(metadata)
                .withName(CASE_LEGAL_SOC_CHECKED_PUBLIC_EVENT_NAME)
                .build();

        final JsonObject publicEventPayload = createObjectBuilder()
                .add(CHECKED_BY, checkedBy.toString())
                .add(CASE_ID, caseId.toString())
                .add(CHECKED_AT, checkedAt)
                .build();

        sender.send(envelopeFrom(publicEventMetadata, publicEventPayload));
    }

    private String getSocCheckedNoteText(final JsonObject checkedByUserDetails, final String checkedAt) {
        /**
         * "Checked by legal advisor
         *
         * <dateOfSOCCheck> at <timeOfSOCCheck> by <legalAdvisorName>"
         */
        final ZonedDateTime zonedDateTime = ZonedDateTime.parse(checkedAt);
        final String advisorName = format("%s %s", checkedByUserDetails.getString(FIRST_NAME), checkedByUserDetails.getString(LAST_NAME));

        return format("Checked by legal advisor %n%s at %s by %s", zonedDateTime.toLocalDate(),
                zonedDateTime.toLocalDateTime().toLocalTime(), advisorName);
    }
}
