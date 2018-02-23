package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.allOf;
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
                        payload().isJson(allOf(withJsonPath("$.id"),
                                withJsonPath("$.urn", equalTo(urn)),
                                withJsonPath("$.completed", equalTo(expected.getBoolean("completed"))),
                                withJsonPath("$.assigned", equalTo(expected.getBoolean("assigned"))),
                                withJsonPath("$.defendant.personalDetails.firstName", equalTo(person(expected).getString("firstName"))),
                                withJsonPath("$.defendant.personalDetails.lastName", equalTo(person(expected).getString("lastName"))),
                                withJsonPath("$.defendant.personalDetails.dateOfBirth", equalTo(person(expected).getString("dateOfBirth"))),
                                withJsonPath("$.defendant.personalDetails.address.address1", equalTo(address(expected).getString("address1"))),
                                withJsonPath("$.defendant.personalDetails.address.address2", equalTo(address(expected).getString("address2"))),
                                withJsonPath("$.defendant.personalDetails.address.address3", equalTo(address(expected).getString("address3"))),
                                withJsonPath("$.defendant.personalDetails.address.address4", equalTo(address(expected).getString("address4"))),
                                withJsonPath("$.defendant.personalDetails.address.postcode", equalTo(postcode)),
                                withJsonPath("$.defendant.offences[0].title", equalTo(offence(expected).getString("title"))),
                                withJsonPath("$.defendant.offences[0].legislation", equalTo(offence(expected).getString("legislation"))),
                                withJsonPath("$.defendant.offences[0].wording", equalTo(offence(expected).getString("wording"))),
                                withJsonPath("$.defendant.offences[0].pendingWithdrawal", equalTo(offence(expected).getBoolean("pendingWithdrawal")))
                        )));
    }

    public void verifyCaseByPersonUrnWithoutPrefixAndPostcode(final JsonObject expected, final String urnWithoutPrefix, String urn, final String postcode) {
        poll(getCaseByUrnAndPostcode(urnWithoutPrefix, postcode))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.id"),
                                withJsonPath("$.urn", equalTo(urn)),
                                withJsonPath("$.defendant.personalDetails.address.postcode", equalTo(postcode))
                        )));
    }

    private JsonObject address(JsonObject expected) {
        return person(expected).getJsonObject("address");
    }

    private JsonObject person(JsonObject caseObject) {
        return caseObject.getJsonObject("defendant")
                .getJsonObject("personalDetails");
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
