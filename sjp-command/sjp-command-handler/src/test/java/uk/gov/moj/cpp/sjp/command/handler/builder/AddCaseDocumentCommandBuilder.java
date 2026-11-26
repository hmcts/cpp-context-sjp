package uk.gov.moj.cpp.sjp.command.handler.builder;


import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.CASE_DOCUMENT_ID;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.CASE_DOCUMENT_MATERIAL_ID;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.CASE_DOCUMENT_TYPE_SJPN;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.CASE_ID;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.JsonObjectBuilder;

public class AddCaseDocumentCommandBuilder {

    private UUID caseId = CASE_ID;
    private UUID id = CASE_DOCUMENT_ID;
    private UUID materialId = CASE_DOCUMENT_MATERIAL_ID;
    private String documentType;

    private AddCaseDocumentCommandBuilder() {
    }

    public static AddCaseDocumentCommandBuilder anAddCaseDocumentCommand() {
        return new AddCaseDocumentCommandBuilder().withDocumentType(CASE_DOCUMENT_TYPE_SJPN);
    }

    public static AddCaseDocumentCommandBuilder anMinimumAddCaseDocumentCommand() {
        return new AddCaseDocumentCommandBuilder();
    }

    public AddCaseDocumentCommandBuilder withDocumentType(String documentType) {
        this.documentType = documentType;
        return this;
    }

    public AddCaseDocumentCommandBuilder withId(UUID id) {
        this.id = id;
        return this;
    }


    public JsonEnvelope build() {
        if (caseId == null || id == null || materialId == null) {
            throw new RuntimeException("CaseId, id, materialId required by the AddCaseDocument command.");
        }

        JsonObjectBuilder victim = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("id", id.toString())
                .add("materialId", materialId.toString());

        if (this.documentType != null) {
            victim.add("documentType", documentType);
        }

        return envelopeFrom(
                metadataOf(UUID.randomUUID(), "sjp.command.add-case-document"),
                victim.build()
        );
    }
}
