package uk.gov.moj.sjp.it.helper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.sjp.it.EventSelector.EVENT_SELECTOR_CASE_RECEIVED;

import uk.gov.moj.sjp.it.EventSelector;

import java.time.LocalDate;

import com.jayway.restassured.path.json.JsonPath;
import org.json.JSONObject;

public class CaseSjpHelper extends AbstractCaseHelper {

    private static final String WRITE_MEDIA_TYPE = "application/vnd.sjp.create-sjp-case+json";

    private static final String TEMPLATE_CASE_SJP_CREATE_PAYLOAD = "raml/json/sjp.create-sjp-case.json";

    private final LocalDate postingDate;

    public CaseSjpHelper() {
        this(LocalDate.of(2015, 12, 2));
    }

    public CaseSjpHelper(final LocalDate postingDate) {
        this.postingDate = postingDate;
    }

    @Override
    protected void doAdditionalReadCallResponseVerification(JsonPath jsonRequest, JsonPath jsonResponse) {
        assertThat(jsonResponse.get("caseId"), equalTo(jsonRequest.get("caseId")));
        assertThat(jsonResponse.get("urn"), equalTo(jsonRequest.get("urn")));
        assertThat(jsonResponse.get("defendant.personalDetails.title"), equalTo(jsonRequest.get("defendant.title")));
        assertThat(jsonResponse.get("defendant.personalDetails.firstName"), equalTo(jsonRequest.get("defendant.firstName")));
        assertThat(jsonResponse.get("defendant.personalDetails.lastName"), equalTo(jsonRequest.get("defendant.lastName")));
        assertThat(jsonResponse.get("defendant.personalDetails.dateOfBirth"), equalTo(jsonRequest.get("defendant.dateOfBirth")));
        assertThat(jsonResponse.get("defendant.personalDetails.gender"), equalTo(jsonRequest.get("defendant.gender")));
        assertThat(jsonResponse.get("defendant.numPreviousConvictions"), equalTo(jsonRequest.get("defendant.numPreviousConvictions")));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address1"), equalTo(jsonRequest.get("defendant.address.address1")));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address2"), equalTo(jsonRequest.get("defendant.address.address2")));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address3"), equalTo(jsonRequest.get("defendant.address.address3")));
        assertThat(jsonResponse.get("defendant.personalDetails.address.address4"), equalTo(jsonRequest.get("defendant.address.address4")));
        assertThat(jsonResponse.get("defendant.personalDetails.address.postcode"), equalTo(jsonRequest.get("defendant.address.postcode")));
        assertThat(jsonResponse.get("defendant.offences[0].offenceSequenceNumber"), equalTo(jsonRequest.get("defendant.offences[0].offenceSequenceNo")));
        assertThat(jsonResponse.get("defendant.offences[0].wording"), equalTo(jsonRequest.get("defendant.offences[0].offenceWording")));
        assertThat(jsonResponse.get("defendant.offences[0].chargeDate"), equalTo(jsonRequest.get("defendant.offences[0].chargeDate")));
    }

    @Override
    protected String getTemplatePayloadPath() {
        return TEMPLATE_CASE_SJP_CREATE_PAYLOAD;
    }

    @Override
    protected String getWriteMediaType() {
        return WRITE_MEDIA_TYPE;
    }

    @Override
    protected String getEventSelector() {
        return EVENT_SELECTOR_CASE_RECEIVED;
    }

    @Override
    protected void doAdditionalReplacementOfValues(JSONObject jsonObject) {
        jsonObject.put("postingDate", postingDate);
        final JSONObject defendant = jsonObject.getJSONObject("defendant");
        defendant.getJSONArray("offences").getJSONObject(0).put("id", offenceId);
    }

    @Override
    protected String getPublicEventSelector() {
        return EventSelector.PUBLIC_EVENT_SELECTOR_SJP_CASE_CREATED;
    }

    public String getSingleDefendantId() {
        return jsonResponse.get("defendant.id");
    }

    public String getSingleOffenceId() {
        return jsonResponse.get("defendant.offences[0].id");
    }
}
