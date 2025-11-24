package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionalOrganisations;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.matchers.RegionsMatcher.region;

import uk.gov.moj.sjp.it.helper.RegionsQueryApiClient;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

public class SjpQueryApiRegionsIT extends BaseIntegrationTest {

    @Test
    public void shouldFetchRegionsFromReferenceData() {
        stubForUserDetails(BaseIntegrationTest.USER_ID, "ALL");
        stubRegionalOrganisations();

        final JsonObject response = RegionsQueryApiClient.getRegions();

        assertThat(response.toString(), isJson(withJsonPath("regions[*]", allOf(
                region("40805c9f-42c3-3e8f-a1a8-c75165aff66b", "North West"),
                region("80a67650-25a4-337e-bda7-2fb2e7c689bc", "North East"),
                region("cb46ca55-2bcb-308d-876f-b61a8aaee4ce", "Midlands"),
                region("cecfdf28-ef5c-311a-a563-cfb8dc582cc6", "South West"),
                region("d3cb5d31-d18d-35c6-9318-d62f958a0fc6", "Wales"),
                region("aa597777-d263-3fb9-9c08-b632052db706", "South East"),
                region("05f6b08c-29df-3ad0-94d3-32529c555b82", "London")
        ))));
    }
}
