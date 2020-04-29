package uk.gov.moj.sjp.it.test;

import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.jayway.restassured.path.json.JsonPath;
import org.junit.Before;
import org.junit.Test;
import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.sjp.it.helper.CaseHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.jayway.awaitility.Awaitility.await;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.withDefaults;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;

@SuppressWarnings("unchecked")
public class AddRequestForOutstandingFinesIT extends BaseIntegrationTest {
    private final UUID caseIdOne = randomUUID();
    private final UUID defendantIdOne = randomUUID();

    public static void verifyStagingenforcementRequestOutstandingFines(final List<String> expectedValues) {

        final RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching("/stagingenforcement-service/command/api/rest/stagingenforcement/outstanding-fines"));
        expectedValues.forEach(
                expectedValue -> requestPatternBuilder.withRequestBody(containing(expectedValue))
        );
        verify(requestPatternBuilder);
    }

    public static void stubStagingenforcementOutstandingFines() {
        InternalEndpointMockUtils.stubPingFor("stagingenforcement-service");

        stubFor(post(urlPathEqualTo("/stagingenforcement-service/command/api/rest/stagingenforcement/outstanding-fines"))
                        .withHeader(CONTENT_TYPE, equalTo("application/vnd.stagingenforcement.request-outstanding-fine+json"))
                        .willReturn(aResponse().withStatus(SC_ACCEPTED)));

    }

    @Before
    public void setUp() {
        stubProsecutorQuery(TFL.name(), TFL.getFullName(), randomUUID());
        new EventListener()
                .subscribe(CaseReceived.EVENT_NAME)
                .run(() -> createCaseForPayloadBuilder(withDefaults().withId(caseIdOne).withDefendantId(defendantIdOne)));
        stubStagingenforcementOutstandingFines();
    }

    @Test
    public void shouldPostComputeOutstandingFines() throws Exception {

        CaseHelper.pollUntilCaseReady(caseIdOne);
        final JsonPath jsonPath = CasePoller.pollUntilCaseByIdIsOk(caseIdOne);
        final String defendantId = jsonPath.getString("defendant.id");
        System.out.println(jsonPath.toString());

        final String path = "/add-request-for-outstanding-fines";
        final String mediaType = "application/vnd.sjp.add-request-for-outstanding-fines+json";
        final String payload = createObjectBuilder().build().toString();

        makePostCall(USER_ID, path, mediaType, payload, ACCEPTED);

        await().atMost(30, TimeUnit.SECONDS).until(() -> verifyStagingenforcementRequestOutstandingFines(Arrays.asList(
                jsonPath.getString("defendant.personalDetails.firstName"),
                jsonPath.getString("defendant.personalDetails.lastName"),
                jsonPath.getString("defendant.personalDetails.nationalInsuranceNumber"),
                defendantId
        )));

    }

}