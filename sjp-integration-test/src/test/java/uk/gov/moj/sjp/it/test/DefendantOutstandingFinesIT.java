package uk.gov.moj.sjp.it.test;

import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.ReadContext;
import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.stub.DefendantOutstandingFinesStub;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.withDefaults;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getReadUrl;
import static uk.gov.moj.sjp.it.util.JsonHelper.getJsonObject;
import static uk.gov.moj.sjp.it.util.JsonHelper.lenientCompare;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaults;

public class DefendantOutstandingFinesIT extends BaseIntegrationTest {

    private static final String DEFENDANT_OUTSTANDING_FINES_TYPE = "application/vnd.sjp.query.defendant-outstanding-fines+json";
    private final UUID caseIdOne = randomUUID();
    private final UUID defendantIdOne = randomUUID();

    @BeforeEach
    public void setUp() {
        stubProsecutorQuery(TFL.name(), TFL.getFullName(), randomUUID());
        new EventListener()
                .subscribe(CaseReceived.EVENT_NAME)
                .run(() -> createCaseForPayloadBuilder(withDefaults().withId(caseIdOne).withDefendantId(defendantIdOne)));
    }

    @Test
    public void shouldQueryOutstandingFines() throws IOException {
        final JsonPath jsonPath = CasePoller.pollUntilCaseByIdIsOk(caseIdOne);
        final UUID defendantId = UUID.fromString(jsonPath.getString("defendant.id"));
        final String firstName = jsonPath.getString("defendant.personalDetails.firstName");
        final String lastName = jsonPath.getString("defendant.personalDetails.lastName");
        final String dateOfBirth = jsonPath.getString("defendant.personalDetails.dateOfBirth");
        final String nationalInsuranceNumber = jsonPath.getString("defendant.personalDetails.nationalInsuranceNumber");
        final String fineResults = getPayload("stub-data/stagingenforcement.defendant.outstanding-fines.json");

        DefendantOutstandingFinesStub.stubStagingEnforcementOutstandingFines(firstName, lastName, dateOfBirth, nationalInsuranceNumber, fineResults);


        final RequestParamsBuilder requestParamsBuilder = getDefendantOutstandingFinesById(defendantId);
        final RequestParams build = requestParamsBuilder.build();

        final RestClient restClient = new RestClient();
        final Response response = restClient.query(build.getUrl(), build.getMediaType(), build.getHeaders());
        assertTrue(lenientCompare(getJsonObject(fineResults), getJsonObject(response.readEntity(String.class))));

    }


    @Test
    public void shouldQueryOutstandingFines_404() {
        CasePoller.pollUntilCaseByIdIsOk(caseIdOne);

        pollWithDefaults(getDefendantOutstandingFinesById(UUID.randomUUID()))
                .until(status().is(OK),
                        payload().isJson(allOf(ImmutableList.<Matcher<? super ReadContext>>builder()
                                .add(withJsonPath("$.outstandingFines.length()", equalTo(0))).build())));


    }

    public static RequestParamsBuilder getDefendantOutstandingFinesById(final UUID defendantId) {
        return getDefendantOutstandingFinesById(defendantId, USER_ID);
    }

    public static RequestParamsBuilder getDefendantOutstandingFinesById(final UUID defendantId, final UUID userId) {
        return requestParams(getReadUrl("/defendant/") + defendantId + "/outstanding-fines", DEFENDANT_OUTSTANDING_FINES_TYPE)
                .withHeader(HeaderConstants.USER_ID, userId);
    }


}
