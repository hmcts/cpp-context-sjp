package uk.gov.moj.cpp.sjp.query.view;


import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.toJsonArray;

import uk.gov.justice.json.schemas.domains.sjp.NoteType;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseNote;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseNoteRepository;
import uk.gov.moj.cpp.sjp.query.view.service.UserAndGroupsService;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

@ServiceComponent(Component.QUERY_VIEW)
public class CaseNotesQueryView {

    @Inject
    private Enveloper enveloper;

    @Inject
    private CaseNoteRepository caseNotesRepository;

    @Inject
    private UserAndGroupsService userAndGroupsService;

    private static JsonValue transformToResponseItem(final CaseNote caseNote) {
        final JsonObjectBuilder builder = createObjectBuilder();
        builder.add("noteId", caseNote.getNoteId().toString())
                .add("noteType", caseNote.getNoteType().name())
                .add("noteText", caseNote.getNoteText())
                .add("addedAt", ZonedDateTimes.toString(caseNote.getAddedAt()))
                .add("authorFirstName", caseNote.getAuthorFirstName())
                .add("authorLastName", caseNote.getAuthorLastName());
        caseNote.getDecisionId().ifPresent(decisionId -> builder.add("decisionId", decisionId.toString()));
        return builder.build();
    }

    @Handles("sjp.query.case-notes")
    public JsonEnvelope getCaseNotes(final JsonEnvelope query) {
        final UUID caseId = UUID.fromString(query.payloadAsJsonObject().getString("caseId"));

        JsonArray caseNotes;

        if (userAndGroupsService.isUserProsecutor(query)) {
            caseNotes = toJsonArray(
                    caseNotesRepository.findByCaseIdAndNoteTypeOrderByAddedAtDesc(caseId, NoteType.CASE_MANAGEMENT),
                    CaseNotesQueryView::transformToResponseItem
            );
        } else {
            caseNotes = toJsonArray(
                    caseNotesRepository.findByCaseIdOrderByAddedAtDesc(caseId),
                    CaseNotesQueryView::transformToResponseItem
            );
        }


        return envelopeFrom(metadataFrom(query.metadata()).withName("sjp.query.case-notes"),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("notes", caseNotes).build());
    }
}
