package uk.gov.moj.cpp.sjp.query.view;


import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.toJsonArray;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseNoteRepository;
import uk.gov.moj.cpp.sjp.query.view.service.CaseService;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObjectBuilder;

@ServiceComponent(Component.QUERY_VIEW)
public class CaseNotesQueryView {

    @Inject
    private Enveloper enveloper;

    @Inject
    private CaseService caseService;

    @Inject
    private CaseNoteRepository caseNotesRepository;

    @Handles("sjp.query.case-notes")
    public JsonEnvelope getCaseNotes(final JsonEnvelope query) {
        final UUID caseId = UUID.fromString(query.payloadAsJsonObject().getString("caseId"));

        final JsonArray caseNotes = toJsonArray(caseNotesRepository.findByCaseIdOrderByAddedAtDesc(caseId),
                caseNote -> {
                    final JsonObjectBuilder builder = createObjectBuilder();
                    builder.add("noteId", caseNote.getNoteId().toString())
                            .add("noteType", caseNote.getNoteType().name())
                            .add("noteText", caseNote.getNoteText())
                            .add("addedAt", caseNote.getAddedAt().toString())
                            .add("authorFirstName", caseNote.getAuthorFirstName())
                            .add("authorLastName", caseNote.getAuthorLastName());
                    caseNote.getDecisionId().ifPresent(decisionId -> builder.add("decisionId", decisionId.toString()));
                    return builder.build();
                });

        return enveloper.withMetadataFrom(query, "sjp.query.case-notes").apply(createObjectBuilder().add("caseId", caseId.toString()).add("notes", caseNotes).build());
    }
}
