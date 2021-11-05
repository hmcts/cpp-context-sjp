package uk.gov.moj.cpp.sjp.query.view;


import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.AccountNote;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseAccountNoteRepository;

import java.util.List;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.QUERY_VIEW)
public class AccountNoteQueryView {

    @Inject
    private Enveloper enveloper;

    @Inject
    private CaseAccountNoteRepository caseAccountNoteRepository;

    @Handles("sjp.query.account-note")
    public JsonEnvelope getAccountNote(final JsonEnvelope query) {
        final String caseUrn = query.payloadAsJsonObject().getString("caseUrn");

        final List<AccountNote> accountNotes = caseAccountNoteRepository.findByCaseUrn(caseUrn);

        return envelopeFrom(metadataFrom(query.metadata()).withName("sjp.query.account-note"),
                transformToResponseItem(accountNotes));
    }

    private static JsonObject transformToResponseItem(final List<AccountNote> accountNotes) {


        if (accountNotes != null && !accountNotes.isEmpty()) {
            return createObjectBuilder().add("id", accountNotes.get(0).getId().toString())
                    .add("caseId", accountNotes.get(0).getCaseId().toString())
                    .add("caseUrn", accountNotes.get(0).getCaseUrn())
                    .add("noteText", accountNotes.get(0).getNoteText())
                    .build();
        }

        return createObjectBuilder().build();
    }

}
