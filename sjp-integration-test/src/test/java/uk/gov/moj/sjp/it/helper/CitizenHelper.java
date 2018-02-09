package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseByUrnAndPostcode;

import javax.json.JsonObject;

public class CitizenHelper {

    public static final String GET_CASE_BY_URN_AND_POSTCODE_MEDIA_TYPE = "application/vnd.sjp.query.case-by-urn-postcode+json";

    // TODO maybe this needs to remain as it was with the numbers
    public void verifyCaseByPersonUrnAndPostcode(final JsonObject expected, final String urn, final String postcode) {
        poll(getCaseByUrnAndPostcode(urn, postcode))
                .until(
                        status().is(OK),
                        payload().isJson(withJsonPath("$.id")),
                        payload().isJson(withJsonPath("$.urn", equalTo(urn))),
                        payload().isJson(withJsonPath("$.completed", equalTo(expected.getBoolean("completed")))),
                        payload().isJson(withJsonPath("$.assigned", equalTo(expected.getBoolean("assigned")))),
                        payload().isJson(withJsonPath("$.defendant.personalDetails.firstName", equalTo(person(expected).getString("firstName")))),
                        payload().isJson(withJsonPath("$.defendant.personalDetails.lastName", equalTo(person(expected).getString("lastName")))),
                        payload().isJson(withJsonPath("$.defendant.personalDetails.dateOfBirth", equalTo(person(expected).getString("dateOfBirth")))),
                        payload().isJson(withJsonPath("$.defendant.personalDetails.address.address1", equalTo(address(expected).getString("address1")))),
                        payload().isJson(withJsonPath("$.defendant.personalDetails.address.address2", equalTo(address(expected).getString("address2")))),
                        payload().isJson(withJsonPath("$.defendant.personalDetails.address.address3", equalTo(address(expected).getString("address3")))),
                        payload().isJson(withJsonPath("$.defendant.personalDetails.address.address4", equalTo(address(expected).getString("address4")))),
                        payload().isJson(withJsonPath("$.defendant.personalDetails.address.postcode", equalTo(postcode))),
                        payload().isJson(withJsonPath("$.defendant.offences[0].title", equalTo(offence(expected).getString("title")))),
                        payload().isJson(withJsonPath("$.defendant.offences[0].legislation", equalTo(offence(expected).getString("legislation")))),
                        payload().isJson(withJsonPath("$.defendant.offences[0].wording", equalTo(offence(expected).getString("wording")))),
                        payload().isJson(withJsonPath("$.defendant.offences[0].pendingWithdrawal", equalTo(offence(expected).getBoolean("pendingWithdrawal"))))
                );
    }

    private JsonObject address(JsonObject expected) {
        return person(expected).getJsonObject("address");
    }

    private JsonObject person(JsonObject caseObject) {
        return caseObject.getJsonObject("defendant")
                .getJsonObject("personalDetails");
    }

    private JsonObject contactDetails(JsonObject caseObject) {
        return caseObject.getJsonObject("defendant")
                .getJsonObject("personalDetails")
                .getJsonObject("contactDetails");
    }

    private JsonObject offence(JsonObject caseObject) {
        return caseObject.getJsonObject("defendant")
                .getJsonArray("offences")
                .getJsonObject(0);
    }

    public void verifyNoCaseByPersonUrnAndPostcode(final String urn, final String postcode) {
        poll(getCaseByUrnAndPostcode(urn, postcode)).until(status().is(NOT_FOUND));
    }
}
