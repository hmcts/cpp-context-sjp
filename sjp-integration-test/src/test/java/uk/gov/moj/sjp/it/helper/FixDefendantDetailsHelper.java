package uk.gov.moj.sjp.it.helper;

import javax.json.JsonObject;

public class FixDefendantDetailsHelper extends AbstractTestHelper {

    public void fixDefendantDetails(String caseId, JsonObject payload){
        String url = getWriteUrl(String.format("/cases/%s/fixdefendant", caseId));
        makePostCall(url, "application/vnd.sjp.fix-defendant-details+json", payload.toString());
    }
}
