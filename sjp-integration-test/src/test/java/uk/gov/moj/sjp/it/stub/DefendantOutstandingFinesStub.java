package uk.gov.moj.sjp.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_OK;

import uk.gov.justice.services.common.http.HeaderConstants;

import com.github.tomakehurst.wiremock.client.WireMock;

public class DefendantOutstandingFinesStub {
    public static void stubStagingEnforcementOutstandingFines(final String firstName,
                                                        final String lastName,
                                                        final String dateOfBirth,
                                                        final String nationalInsuranceNumber, final String jsonResponse) {

        final String urlPath = "/stagingenforcement-service/query/api/rest/stagingenforcement/defendant/outstanding-fines";
        stubFor(get(urlPathEqualTo(urlPath))
                .withQueryParam("firstname", WireMock.equalTo(firstName))
                .withQueryParam("lastname", WireMock.equalTo(lastName))
                .withQueryParam("dob", WireMock.equalTo(dateOfBirth))
                .withQueryParam("ninumber", WireMock.equalTo(nationalInsuranceNumber))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader(HeaderConstants.ID, randomUUID().toString())
                        .withHeader(CONTENT_TYPE, "application/vnd.stagingenforcement.defendant.outstanding-fines+json")
                        .withBody(jsonResponse)));
    }
}
