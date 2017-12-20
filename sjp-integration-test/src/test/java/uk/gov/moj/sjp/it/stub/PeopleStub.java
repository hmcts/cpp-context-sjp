package uk.gov.moj.sjp.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.sjp.it.util.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import javax.json.JsonObjectBuilder;

public class PeopleStub extends StubUtil {

    static {
        InternalEndpointMockUtils.stubPingFor("people-query-api");
    }

    public static void stubPerson(final String personId, final String postcode) {

        final JsonObjectBuilder objectBuilder = createObjectBuilder()
                .add("id", personId)
                .add("firstName", "firstName")
                .add("lastName", "lastName")
                .add("dateOfBirth", "1980-07-15")
                .add("homeTelephone", "02012345678")
                .add("mobile", "07777888999")
                .add("email", "email@email.com")
                .add("nationalInsuranceNumber", "AA123456C")
                .add("address", createObjectBuilder()
                        .add("address1", "address1")
                        .add("address2", "address2")
                        .add("address3", "address3")
                        .add("address4", "address4")
                        .add("postCode", postcode));

        String urlPath = "/people-query-api/query/api/rest/people/.*";
        stubFor(get(urlMatching(urlPath))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", DEFAULT_JSON_CONTENT_TYPE)
                        .withBody(objectBuilder.build().toString())));

        waitForStubToBeReady(urlPath, "application/vnd.people.query.person+json");
    }
}
