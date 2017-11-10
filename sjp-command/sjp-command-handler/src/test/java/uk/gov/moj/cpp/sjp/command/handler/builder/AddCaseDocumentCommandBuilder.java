package uk.gov.moj.cpp.sjp.command.handler.builder;


import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataOf;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.CASE_DOCUMENT_ID;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.CASE_DOCUMENT_MATERIAL_ID;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.CASE_DOCUMENT_POLICE_MATERIAL_ID;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.CASE_DOCUMENT_POLICE_NAME;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.CASE_DOCUMENT_TYPE_SJPN;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.CASE_ID;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.JsonObjectBuilder;

public class AddCaseDocumentCommandBuilder {

    private UUID caseId = CASE_ID;
    private UUID id = CASE_DOCUMENT_ID;
    private String materialId = CASE_DOCUMENT_MATERIAL_ID;
    private String policeName = CASE_DOCUMENT_POLICE_NAME;
    private String policeMaterialId = CASE_DOCUMENT_POLICE_MATERIAL_ID;
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

    public AddCaseDocumentCommandBuilder withPoliceMaterialId(String policeMaterialId) {
        this.policeMaterialId = policeMaterialId;
        return this;
    }

    public JsonEnvelope build() {
        if (caseId == null || id == null || materialId == null || policeName == null || policeMaterialId == null) {
            throw new RuntimeException("CaseId, id, materialId, policeName and policeMaterialId are required by the AddCaseDocument command.");
        }

        JsonObjectBuilder victim = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("id", id.toString())
                .add("materialId", materialId)
                .add("policeName", policeName)
                .add("policeMaterialId", policeMaterialId);

        if (this.documentType != null) {
            victim.add("documentType", documentType);
        }

        return envelopeFrom(
                metadataOf(UUID.randomUUID(), "structure.command.add-case-document"),
                victim.build()
        );
    }
}
