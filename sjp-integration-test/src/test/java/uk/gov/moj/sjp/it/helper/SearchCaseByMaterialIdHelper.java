package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.util.DefaultRequests.searchCasesByMaterialId;

public class SearchCaseByMaterialIdHelper extends AbstractTestHelper {

    public static final String CASE_ID_PROPERTY = "caseId";
    public static final String PROSECUTING_AUTHORITY_PROPERTY = "prosecutingAuthority";

    public static final String CASES_SEARCH_BY_MATERIAL_MEDIA_TYPE = "application/vnd.sjp.query.cases-search-by-material-id+json";

    private CaseHelper caseHelper;
    private String materialId;

    public SearchCaseByMaterialIdHelper(CaseHelper caseHelper, String materialId) {
        this.caseHelper = caseHelper;
        this.materialId = materialId;
    }

    public void assertCaseRetrieved(String prosecutingAuthority) {
        poll(searchCasesByMaterialId(materialId))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.caseId", is(caseHelper.getCaseId())),
                                withJsonPath("$.prosecutingAuthority", is(prosecutingAuthority)))
                        )
                );

    }
}
