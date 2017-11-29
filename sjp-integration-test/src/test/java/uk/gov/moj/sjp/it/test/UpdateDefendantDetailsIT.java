package uk.gov.moj.sjp.it.test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.TEN_SECONDS;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.sjp.it.util.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.sjp.it.helper.CaseSjpHelper;
import uk.gov.moj.sjp.it.helper.DefendantDetailsHelper;
import uk.gov.moj.sjp.it.util.FileUtil;

import java.io.IOException;

import javax.json.JsonObject;

import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.client.UrlMatchingStrategy;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UpdateDefendantDetailsIT extends BaseIntegrationTest {

    private CaseSjpHelper caseSjpHelper;
    private DefendantDetailsHelper defendantDetailsHelper;

    @Before
    public void createACaseAfterAnother() {
        caseSjpHelper = new CaseSjpHelper();
        caseSjpHelper.createCase();
        caseSjpHelper.verifyCaseCreatedUsingId();
        defendantDetailsHelper = new DefendantDetailsHelper();
    }

    @After
    public void tearDown() {
        caseSjpHelper.close();
        defendantDetailsHelper.close();
    }

    @Test
    public void updateDefendantDetails() throws IOException {

        JsonObject payloadTemplate = FileUtil.givenPayload("/payload/sjp.update-defendant-details.json");

        JsonObject payload = JsonObjects
                .createObjectBuilderWithFilter(payloadTemplate, propertyName -> !propertyName.equals("personId"))
                .add("personId", caseSjpHelper.getDefendantPersonId())
                .build();
        stubPeopleCommand(caseSjpHelper.getDefendantPersonId());

        defendantDetailsHelper.updateDefendantDetails(caseSjpHelper.getCaseId(),
                caseSjpHelper.getSingleDefendantId(), payload);

        assertDetailsExistence(caseSjpHelper.getDefendantPersonId());
    }

    private void stubPeopleCommand(String peopleId) {
        String COMMAND_URL = "/people-command-api/command/api/rest/people/people/" + peopleId;
        String COMMAND_MEDIA_TYPE = "application/vnd.people.command.update-personal-details+json";
        String DEFAULT_JSON_CONTENT_TYPE = "application/json";

        InternalEndpointMockUtils.stubPingFor("people-command-api");

        stubFor(post(urlPathEqualTo(COMMAND_URL))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("Content-Type", DEFAULT_JSON_CONTENT_TYPE)));

        stubFor(get(urlPathEqualTo(COMMAND_URL))
                .willReturn(aResponse().withStatus(SC_OK)));

        waitForStubToBeReady(COMMAND_URL, COMMAND_MEDIA_TYPE);
    }


    private void assertDetailsExistence(String personId) {
        UrlMatchingStrategy url = new UrlMatchingStrategy();
        url.setUrlPath("/people-command-api/command/api/rest/people/people/" + personId);

        await().atMost(TEN_SECONDS).until(() ->
                WireMock.findAll(new RequestPatternBuilder(RequestMethod.POST, url)
                        .withHeader("Content-Type", equalTo("application/vnd.people.command.update-personal-details+json"))
                        .withRequestBody(containing("\"title\":\"Mr\""))
                        .withRequestBody(containing("\"firstName\":\"David\""))
                        .withRequestBody(containing("\"lastName\":\"SMITH\""))
                        .withRequestBody(containing("\"dateOfBirth\":\"1980-07-15\""))
                        .withRequestBody(containing("\"gender\":\"Male\""))
                        .withRequestBody(containing("\"email\":\"email@email.com\""))
                        .withRequestBody(containing("\"address1\":\"14 Shaftesbury Road\""))
                        .withRequestBody(containing("\"address2\":\"London\""))
                        .withRequestBody(containing("\"address3\":\"England\""))
                        .withRequestBody(containing("\"address4\":\"UK\""))
                        .withRequestBody(containing("\"postCode\":\"W1T 1JY\""))
                        .withRequestBody(containing("\"home\":\"02087654321\""))
                        .withRequestBody(containing("\"mobile\":\"07123456789\""))
                        .withRequestBody(containing("\"nationalInsuranceNumber\":\"QQ 12 34 56 C\""))
                ).size() > 0
        );
    }

}
