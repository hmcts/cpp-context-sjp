package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseByUrnAndPostcode;

public class CitizenHelper {

    public static final String GET_CASE_BY_URN_AND_POSTCODE_MEDIA_TYPE = "application/vnd.sjp.query.case-by-urn-postcode+json";

    public void verifyCaseByPersonUrnAndPostcode(final String urn, final String postcode) {
        poll(getCaseByUrnAndPostcode(urn, postcode))
                .until(
                        status().is(OK),
                        payload().isJson(withJsonPath("$.defendants[0].person.address.postcode", equalTo(postcode)))
                );
    }

    public void verifyNoCaseByPersonUrnAndPostcode(final String urn, final String postcode) {
        poll(getCaseByUrnAndPostcode(urn, postcode)).until(status().is(NOT_FOUND));
    }
}
