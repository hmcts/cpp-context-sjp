package uk.gov.moj.sjp.it.helper;

import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;

import javax.json.JsonObject;

public class DefendantDetailsHelper  {

    public void updateDefendantDetails(String caseId, String defendantId, JsonObject payload){
        String url = String.format("/cases/%s/defendant/%s", caseId, defendantId);
        makePostCall(url, "application/vnd.sjp.update-defendant-details+json", payload.toString());
    }
}
