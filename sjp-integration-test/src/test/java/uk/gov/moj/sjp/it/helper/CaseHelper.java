package uk.gov.moj.sjp.it.helper;

import com.jayway.restassured.path.json.JsonPath;
import org.json.JSONObject;
import uk.gov.moj.sjp.it.EventSelector;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.EventSelector.EVENT_SELECTOR_CASE_CREATED;

/**
 * Helper to class to support Write / Read operations for assignment
 */
public class CaseHelper extends AbstractCaseHelper {

    private static final String WRITE_MEDIA_TYPE = "application/vnd.structure.command.create-case+json";

    private static final String TEMPLATE_CASE_CREATE_PAYLOAD = "raml/json/structure.command.create-case.json";

    public CaseHelper() {
        super();
    }
    public CaseHelper(String caseId) {
        super();
        this.caseId = caseId;
    }

    @Override
    protected void doAdditionalReadCallResponseVerification(JsonPath jsonRequest, JsonPath jsonResponse) {
        assertThat(jsonResponse.get("caseId"), equalTo(jsonRequest.get("caseId")));
        assertThat(jsonResponse.get("urn"), equalTo(jsonRequest.get("urn")));
        assertThat(jsonResponse.get("policeOfficerInCase.personId"), equalTo(jsonRequest.get("policeOfficerInCase.personId")));
        assertThat(jsonResponse.get("policeOfficerInCase.rank"), equalTo(jsonRequest.get("policeOfficerInCase.rank")));
        assertThat(jsonResponse.get("policeOfficerInCase.narrative"), equalTo(jsonRequest.get("policeOfficerInCase.narrative")));
        assertThat(jsonResponse.get("policeOfficerInCase.badgeNumber"), equalTo(jsonRequest.getString("policeOfficerInCase.badgeNumber")));
        assertThat(jsonResponse.get("policeSupervisor.personId"), equalTo(jsonRequest.get("policeSupervisor.personId")));
        assertThat(jsonResponse.get("policeSupervisor.rank"), equalTo(jsonRequest.get("policeSupervisor.rank")));
        assertThat(jsonResponse.get("policeSupervisor.narrative"), equalTo(jsonRequest.get("policeSupervisor.narrative")));
        assertThat(jsonResponse.get("policeSupervisor.badgeNumber"), equalTo(jsonRequest.getString("policeSupervisor.badgeNumber")));
    }

    @Override
    protected String getTemplatePayloadPath() {
        return TEMPLATE_CASE_CREATE_PAYLOAD;
    }

    @Override
    protected String getWriteMediaType() {
        return WRITE_MEDIA_TYPE;
    }

    @Override
    protected String getEventSelector() {
        return EVENT_SELECTOR_CASE_CREATED;
    }

    @Override
    protected void doAdditionalReplacementOfValues(JSONObject jsonObject) {
        // do nothing
    }

    @Override
    protected String getPublicEventSelector() {
        return EventSelector.PUBLIC_EVENT_SELECTOR_CASE_CREATED;
    }
}
