package uk.gov.moj.sjp.it.helper;

import javax.json.JsonObject;

public class DefendantDetailsHelper extends AbstractTestHelper {

    public void updateDefendantDetails(String caseId, String defendantId, JsonObject payload){
        String url = getWriteUrl(String.format("/cases/%s/defendant/%s", caseId, defendantId));
        makePostCall(url, "application/vnd.sjp.update-defendant-details+json", payload.toString());
    }
}
