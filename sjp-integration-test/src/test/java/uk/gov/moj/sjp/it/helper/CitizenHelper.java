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

    public void verifyCaseByPersonUrnAndPostcode(final JsonObject expected, final String urn, final String postcode) {
        poll(getCaseByUrnAndPostcode(urn, postcode))
                .until(
                        status().is(OK),
                        payload().isJson(withJsonPath("$.completed", equalTo(expected.getBoolean("completed")))),
                        payload().isJson(withJsonPath("$.assigned", equalTo(expected.getBoolean("assigned")))),
                        payload().isJson(withJsonPath("$.defendants[0].person.firstName", equalTo(person(expected).getString("firstName")))),
                        payload().isJson(withJsonPath("$.defendants[0].person.lastName", equalTo(person(expected).getString("lastName")))),
                        payload().isJson(withJsonPath("$.defendants[0].person.dateOfBirth", equalTo(person(expected).getString("dateOfBirth")))),
                        payload().isJson(withJsonPath("$.defendants[0].person.homeTelephone", equalTo(person(expected).getString("homeTelephone")))),
                        payload().isJson(withJsonPath("$.defendants[0].person.mobile", equalTo(person(expected).getString("mobile")))),
                        payload().isJson(withJsonPath("$.defendants[0].person.email", equalTo(person(expected).getString("email")))),
                        payload().isJson(withJsonPath("$.defendants[0].person.nationalInsuranceNumber", equalTo(person(expected).getString("nationalInsuranceNumber")))),
                        payload().isJson(withJsonPath("$.defendants[0].person.address.address1", equalTo(address(expected).getString("address1")))),
                        payload().isJson(withJsonPath("$.defendants[0].person.address.address2", equalTo(address(expected).getString("address2")))),
                        payload().isJson(withJsonPath("$.defendants[0].person.address.address3", equalTo(address(expected).getString("address3")))),
                        payload().isJson(withJsonPath("$.defendants[0].person.address.address4", equalTo(address(expected).getString("address4")))),
                        payload().isJson(withJsonPath("$.defendants[0].person.address.postCode", equalTo(postcode))),
                        payload().isJson(withJsonPath("$.defendants[0].offences[0].title", equalTo(offence(expected).getString("title")))),
                        payload().isJson(withJsonPath("$.defendants[0].offences[0].wording", equalTo(offence(expected).getString("wording")))),
                        payload().isJson(withJsonPath("$.defendants[0].offences[0].pendingWithdrawal", equalTo(offence(expected).getBoolean("pendingWithdrawal"))))
                );
    }

    private JsonObject address(JsonObject expected) {
        return person(expected).getJsonObject("address");
    }

    private JsonObject person(JsonObject caseObject) {
        return caseObject.getJsonArray("defendants").getJsonObject(0).getJsonObject("person");
    }

    private JsonObject offence(JsonObject caseObject) {
        return caseObject.getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0);
    }

    public void verifyNoCaseByPersonUrnAndPostcode(final String urn, final String postcode) {
        poll(getCaseByUrnAndPostcode(urn, postcode)).until(status().is(NOT_FOUND));
    }
}
