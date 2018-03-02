package uk.gov.moj.sjp.it.helper;

import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;

import java.util.UUID;

import javax.json.JsonObject;

public class FixDefendantDetailsHelper  {

    public void fixDefendantDetails(UUID caseId, JsonObject payload){
        String url = String.format("/cases/%s/fixdefendant", caseId);
        makePostCall(url, "application/vnd.sjp.fix-defendant-details+json", payload.toString());
    }
}
