package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.ws.rs.core.Response.Status.OK;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.util.DefaultRequests.findAwaitingCases;

import uk.gov.moj.sjp.it.helper.CaseDocumentHelper;
import uk.gov.moj.sjp.it.helper.CaseSjpHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AwaitingCasesIT extends BaseIntegrationTest {

    public static final String AWAITING_CASES_MEDIA_TYPE = "application/vnd.sjp.query.awaiting-cases+json";

    private CaseSjpHelper caseSjpHelper;
    private CaseDocumentHelper caseDocumentHelper;
    private String offenceCode;

    @Before
    public void setUp() throws Exception {

        caseSjpHelper = new CaseSjpHelper();
        caseSjpHelper.createCase();
        caseSjpHelper.verifyCaseCreatedUsingId();

        caseDocumentHelper = new CaseDocumentHelper(caseSjpHelper.getCaseId());
        caseDocumentHelper.addDocumentAndVerifyAdded(); // add an SJPN document

        offenceCode = caseSjpHelper.getCaseResponseUsingId().get("defendant.offences[0].offenceCode");
    }

    @After
    public void tearDown() {
        caseSjpHelper.close();
        caseDocumentHelper.close();
    }

    @Test
    public void shouldFindAwaitingCases() {
        poll(findAwaitingCases()).until(status().is(OK),
                payload().isJson(withJsonPath("$.awaitingCases[?]",
                        filter(where("firstName").is("David")
                                .and("offenceCode").is(offenceCode)))));
    }
}
