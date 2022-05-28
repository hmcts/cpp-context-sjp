package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAllReferenceData;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCourtCentreByDefendantPostCode;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaults;

import uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub;
import uk.gov.moj.sjp.it.stub.UsersGroupsStub;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class CourtCentreIT extends BaseIntegrationTest {
    private UUID defendantPostcode = UUID.randomUUID();
    private UUID prosecutionAuthorityCode = UUID.randomUUID();
    final UUID legalAdviserId = randomUUID();


    @Before
    public void setUp() {
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubAllReferenceData();
        UsersGroupsStub.stubGroupForUser(USER_ID, "Legal Advisers");
        UsersGroupsStub.stubGroupForUser(legalAdviserId, UsersGroupsStub.LEGAL_ADVISERS_GROUP);
        ReferenceDataServiceStub.stubQueryCourtsCodeData("/stub-data/referencedata.query.local-justice-area-court-prosecutor-mapping-courts.json");
    }

    @Test
    public void shoulReturnCourtcentre() {

        verifyCourtCentreByDefendantPostCode(defendantPostcode.toString(), prosecutionAuthorityCode.toString());
    }


    public void verifyCourtCentreByDefendantPostCode(final String defendantPostcode, final String prosecutionAuthorityCode) {
        pollWithDefaults(getCourtCentreByDefendantPostCode(defendantPostcode, prosecutionAuthorityCode, legalAdviserId))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.CourtCentre.id", equalTo("f8254db1-1683-483e-afb3-b87fde5a0a26")),
                                withJsonPath("$.CourtCentre.oucode", equalTo("B01LY00"))
                        )));
    }

}
