package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.util.DefaultRequests.searchCasesByPersonId;

public class SearchCasesByPersonIdHelper extends AbstractTestHelper {

    public static final String CASES_SEARCH_MEDIA_TYPE = "application/vnd.sjp.query.cases-search+json";

    public void verifySearchCaseByPersonId(final String personId, final int expectedHits) {
        poll(searchCasesByPersonId(personId))
                .until(
                        status().is(OK),
                        payload().isJson(withJsonPath("$.hits", hasSize(expectedHits)))
                );
    }

}
