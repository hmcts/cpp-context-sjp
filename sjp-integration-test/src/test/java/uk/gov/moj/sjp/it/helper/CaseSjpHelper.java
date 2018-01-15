package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.EventSelector.EVENT_SELECTOR_CASE_RECEIVED;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseById;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.ContactDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.sjp.it.EventSelector;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.json.JSONObject;

public class CaseSjpHelper extends AbstractCaseHelper {

    private static final String WRITE_MEDIA_TYPE = "application/vnd.sjp.create-sjp-case+json";

    private static final String TEMPLATE_CASE_SJP_CREATE_PAYLOAD = "raml/json/sjp.create-sjp-case.json";

    private final LocalDate postingDate;
    private String prosecutor;

    private final PersonalDetails personalDetails = new PersonalDetails("Mr","David", "LLOYD", LocalDates.from("1980-07-15"),
            "Male", "nationalInsuranceNumber",
            new Address("14 Tottenham Court Road", "London", "England", "UK", "W1T 1JY"),
            new ContactDetails(null, null, null)
    );


    public CaseSjpHelper() {
        this(LocalDate.of(2015, 12, 2), "TFL");
    }

    public CaseSjpHelper(final LocalDate postingDate) {
        this(postingDate, "TFL");
    }

    public CaseSjpHelper(final LocalDate postingDate, String prosecutor) {
        this.postingDate = postingDate;
        this.prosecutor = prosecutor;
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
        jsonObject.put("prosecutingAuthority", prosecutor);
    }

    public void verifyPersonInfo() {
        verifyPersonInfo(this.personalDetails, false);
    }

    public void verifyPersonInfo(final PersonalDetails personalDetails, final boolean includeContactsAndNiNumberFields) {
        List<Matcher> fieldMatchers = getCommonFieldMatchers(personalDetails);
        if (includeContactsAndNiNumberFields) {
            fieldMatchers = Stream.of(fieldMatchers, getContactsAndNiNumberMatchers(personalDetails))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        }
        poll(getCaseById(getCaseId()))
                .until(status().is(OK), payload().isJson(allOf(
                        fieldMatchers.toArray(new Matcher[fieldMatchers.size()])
                )));
    }
    
    private List<Matcher> getCommonFieldMatchers(final PersonalDetails personalDetails) {
        return Arrays.asList(
                withJsonPath("urn", equalTo(getCaseUrn())),
                withJsonPath("$.defendant.personalDetails.title", equalTo(personalDetails.getTitle())),
                withJsonPath("$.defendant.personalDetails.firstName", equalTo(personalDetails.getFirstName())),
                withJsonPath("$.defendant.personalDetails.lastName", equalTo(personalDetails.getLastName())),
                withJsonPath("$.defendant.personalDetails.gender", equalTo(personalDetails.getGender())),
                withJsonPath("$.defendant.personalDetails.dateOfBirth", equalTo(LocalDates.to(personalDetails.getDateOfBirth()))),
                withJsonPath("$.defendant.personalDetails.address.address1", equalTo(personalDetails.getAddress().getAddress1())),
                withJsonPath("$.defendant.personalDetails.address.address2", equalTo(personalDetails.getAddress().getAddress2())),
                withJsonPath("$.defendant.personalDetails.address.address3", equalTo(personalDetails.getAddress().getAddress3())),
                withJsonPath("$.defendant.personalDetails.address.address4", equalTo(personalDetails.getAddress().getAddress4())),
                withJsonPath("$.defendant.personalDetails.address.postcode", equalTo(personalDetails.getAddress().getPostcode()))
        );
    }

    private List<Matcher> getContactsAndNiNumberMatchers(final PersonalDetails personalDetails) {
        return Arrays.asList(
                withJsonPath("$.defendant.personalDetails.nationalInsuranceNumber", equalTo(personalDetails.getNationalInsuranceNumber())),
                withJsonPath("$.defendant.personalDetails.contactDetails.email", equalTo(personalDetails.getContactDetails().getEmail())),
                withJsonPath("$.defendant.personalDetails.contactDetails.home", equalTo(personalDetails.getContactDetails().getHome())),
                withJsonPath("$.defendant.personalDetails.contactDetails.mobile", equalTo(personalDetails.getContactDetails().getMobile()))
        );
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

    public PersonalDetails getPersonalDetails() {
        return personalDetails;
    }
}
