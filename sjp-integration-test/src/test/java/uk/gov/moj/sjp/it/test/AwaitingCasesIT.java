package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.ws.rs.core.Response.Status.OK;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.util.DefaultRequests.findAwaitingCases;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaults;

import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseDocumentHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AwaitingCasesIT extends BaseIntegrationTest {

    public static final String AWAITING_CASES_MEDIA_TYPE = "application/vnd.sjp.query.awaiting-cases+json";

    private CaseDocumentHelper caseDocumentHelper;
    private String offenceCode;

    @Before
    public void setUp() {
        final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);

        caseDocumentHelper = new CaseDocumentHelper(createCasePayloadBuilder.getId());
        caseDocumentHelper.addDocumentAndVerifyAdded(); // add an SJPN document

        offenceCode = createCasePayloadBuilder.getOffenceBuilder().getLibraOffenceCode();
    }

    @After
    public void tearDown() {
        caseDocumentHelper.close();
    }

    @Test
    public void shouldFindAwaitingCases() {
        pollWithDefaults(findAwaitingCases())
                .until(status().is(OK),
                        payload().isJson(withJsonPath("$.awaitingCases[?]",
                                filter(where("firstName").is("David")
                                        .and("offenceCode").is(offenceCode)))));
    }
}
