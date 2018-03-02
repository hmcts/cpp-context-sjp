package uk.gov.moj.sjp.it.helper;

import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;

import java.util.UUID;

import javax.json.JsonObject;

public class DefendantDetailsHelper  {

    public void updateDefendantDetails(UUID caseId, String defendantId, JsonObject payload){
        String url = String.format("/cases/%s/defendant/%s", caseId, defendantId);
        makePostCall(url, "application/vnd.sjp.update-defendant-details+json", payload.toString());
    }
}
