package uk.gov.moj.cpp.sjp.command.handler.builder;


import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.CASE_ID;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.REOPEN_DATE;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.REOPEN_LIBRA_NUMBER;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.REOPEN_REASON;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.REOPEN_UPDATE_DATE;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.REOPEN_UPDATE_LIBRA_NUMBER;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.REOPEN_UPDATE_REASON;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectMetadata;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;
import uk.gov.moj.cpp.sjp.domain.CaseReopenDetails;

import javax.json.JsonObject;

public class CaseReopenDetailsBuilder {

    private CaseReopenDetails caseReopenDetails;

    private CaseReopenDetailsBuilder() {
    }

    public static CaseReopenDetailsBuilder defaultCaseReopenDetails() {
        return new CaseReopenDetailsBuilder().withCaseReopenDetails(new CaseReopenDetails(
                CASE_ID, REOPEN_DATE, REOPEN_LIBRA_NUMBER, REOPEN_REASON
        ));
    }

    public static CaseReopenDetailsBuilder defaultCaseReopenUpdatedDetails() {
        return new CaseReopenDetailsBuilder().withCaseReopenDetails(new CaseReopenDetails(
                CASE_ID, REOPEN_UPDATE_DATE, REOPEN_UPDATE_LIBRA_NUMBER, REOPEN_UPDATE_REASON
        ));
    }

    public CaseReopenDetailsBuilder withCaseReopenDetails(CaseReopenDetails caseReopenDetails) {
        this.caseReopenDetails = caseReopenDetails;
        return this;
    }

    public CaseReopenDetails getCaseReopenDetails() {
        return caseReopenDetails;
    }

    public JsonObject buildJsobObject() {
        return createObjectBuilder()
                .add("caseId", caseReopenDetails.getCaseId().toString())
                .add("reopenedDate", caseReopenDetails.getReopenedDate().toString())
                .add("libraCaseNumber", caseReopenDetails.getLibraCaseNumber())
                .add("reason", caseReopenDetails.getReason()).build();
    }

    public JsonEnvelope buildJsonEnvelope(String actionName) {
        return JsonEnvelopeBuilder.envelopeFrom(
                JsonObjectMetadata.metadataOf(CASE_ID, actionName).build(),
                buildJsobObject());
    }
}
