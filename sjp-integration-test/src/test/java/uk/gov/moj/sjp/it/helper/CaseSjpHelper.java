package uk.gov.moj.sjp.it.helper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.sjp.it.EventSelector.EVENT_SELECTOR_SJP_CASE_CREATED;

import uk.gov.moj.sjp.it.EventSelector;

import java.time.LocalDate;
import java.util.UUID;

import com.jayway.restassured.path.json.JsonPath;
import org.json.JSONObject;

public class CaseSjpHelper extends AbstractCaseHelper {

    private static final String WRITE_MEDIA_TYPE = "application/vnd.sjp.create-sjp-case+json";

    public static final String GET_SJP_CASE_BY_URN_MEDIA_TYPE = "application/vnd.sjp.query.sjp-case-by-urn+json";

    private static final String TEMPLATE_CASE_SJP_CREATE_PAYLOAD = "raml/json/sjp.create-sjp-case.json";

    private final LocalDate postingDate;
    private String personId;

    public CaseSjpHelper() {
        this(LocalDate.of(2015, 12, 2), UUID.randomUUID().toString());
    }

    public CaseSjpHelper(final LocalDate postingDate) {
        this(postingDate, UUID.randomUUID().toString());
    }

    public CaseSjpHelper(final LocalDate postingDate, final String personId) {
        this.postingDate = postingDate;
        this.personId = personId;
    }

    @Override
    protected void doAdditionalReadCallResponseVerification(JsonPath jsonRequest, JsonPath jsonResponse) {
        assertThat(jsonResponse.get("caseId"), equalTo(jsonRequest.get("caseId")));
        assertThat(jsonResponse.get("urn"), equalTo(jsonRequest.get("urn")));
        assertThat(jsonResponse.get("defendants[0].personId"), equalTo(jsonRequest.get("personId")));
        assertThat(jsonResponse.get("defendants[0].offences[0].offenceCode"), equalTo(jsonRequest.get("offences[0].libraOffenceCode")));
        assertThat(jsonResponse.get("defendants[0].offences[0].offenceSequenceNumber"), equalTo(jsonRequest.get("offences[0].offenceSequenceNo")));
        assertThat(jsonResponse.get("defendants[0].offences[0].wording"), equalTo(jsonRequest.get("offences[0].offenceWording")));
        assertThat(jsonResponse.get("defendants[0].offences[0].chargeDate"), equalTo(jsonRequest.get("offences[0].chargeDate")));
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
        return EVENT_SELECTOR_SJP_CASE_CREATED;
    }

    @Override
    protected void doAdditionalReplacementOfValues(JSONObject jsonObject) {
        jsonObject.put("postingDate", postingDate);
        jsonObject.getJSONArray("offences").getJSONObject(0).put("id", offenceId);
        jsonObject.put("personId", personId);
    }

    @Override
    protected String getPublicEventSelector() {
        return EventSelector.PUBLIC_EVENT_SELECTOR_SJP_CASE_CREATED;
    }

    public String getSingleDefendantId() {
        return jsonResponse.get("defendants[0].id");
    }

    public String getDefendantPersonId() {
        return personId;
    }

    public String getSingleOffenceId() {
        return jsonResponse.get("defendants[0].offences[0].id");
    }
}
