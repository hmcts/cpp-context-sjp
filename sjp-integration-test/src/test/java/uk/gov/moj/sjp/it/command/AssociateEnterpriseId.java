package uk.gov.moj.sjp.it.command;

import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;

import java.util.Objects;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

public class AssociateEnterpriseId {
    private static final String WRITE_MEDIA_TYPE = "application/vnd.enterprise-id+json";

    private final String enterpriseId;
    private final String commandUrl; 

    public AssociateEnterpriseId(String enterpriseId, UUID caseId) {
        Objects.requireNonNull(enterpriseId, "enterpriseId is mandatory");
        Objects.requireNonNull(caseId, "caseId is mandatory");
        this.enterpriseId = enterpriseId;
        this.commandUrl = "/cases/" + caseId.toString();
    }

    public void associateEnterpriseIdWIthCase() {
        final JsonObject payload = toJsonObjectRepresentingPayload(enterpriseId);
        makePostCall(commandUrl, WRITE_MEDIA_TYPE, payload.toString());
    }

    private JsonObject toJsonObjectRepresentingPayload(final String enterpriseId) {
        return Json.createObjectBuilder()
                .add("enterpriseId", enterpriseId)
                .build();
    }
}
