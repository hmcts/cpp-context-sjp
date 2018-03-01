package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getReadUrl;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.moj.sjp.it.helper.CaseDocumentHelper;
import uk.gov.moj.sjp.it.helper.CaseSjpHelper;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import javax.json.Json;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ResultOrdersIT extends BaseIntegrationTest {

    private CaseSjpHelper case0;
    private CaseDocumentHelper resultOrder0;

    private CaseSjpHelper case1;
    private CaseDocumentHelper resultOrder1;

    @Before
    public void givenResultOrders() {
        case0 = createCase();
        resultOrder0 = createResultOrder(case0.getCaseId());

        case1 = createCase();
        resultOrder1 = createResultOrder(case1.getCaseId());
    }

    @After
    public void tearDown() {
        resultOrder0.close();
        resultOrder1.close();
    }

    @Test
    public void whenGetResultOrders() {
        //when
        final LocalDate FROM_DATE = LocalDate.now(ZoneOffset.UTC);
        final LocalDate TO_DATE = FROM_DATE.plusDays(1);

        RequestParamsBuilder resultOrders = requestParams(getReadUrl(format("/result-orders?fromDate=%s&toDate=%s", FROM_DATE, TO_DATE)),
                        "application/vnd.sjp.query.result-orders+json")
                        .withHeader(HeaderConstants.USER_ID, USER_ID);

        //then
        poll(resultOrders).until(status().is(OK), payload().isJson(allOf(
                        withJsonPath("$.resultOrders[0].caseId", is(case1.getCaseId())),
                        withJsonPath("$.resultOrders[0].urn", is(case1.getCaseUrn())),
//TODO results order fix
//                        withJsonPath("$.resultOrders[0].defendant.personId", is(case1.getDefendantPersonId())),
                        withJsonPath("$.resultOrders[0].order.materialId", is(resultOrder1.getMaterialId())),
                        withJsonPath("$.resultOrders[1].caseId", is(case0.getCaseId())),
                        withJsonPath("$.resultOrders[1].urn", is(case0.getCaseUrn())),
//                        withJsonPath("$.resultOrders[1].defendant.personId", is(case0.getDefendantPersonId())),
                        withJsonPath("$.resultOrders[1].order.materialId", is(resultOrder0.getMaterialId())))));
    }

    private CaseSjpHelper createCase() {
        CaseSjpHelper caseHelper = new CaseSjpHelper();
        caseHelper.createAndVerifyCase();
        return caseHelper;
    }

    private CaseDocumentHelper createResultOrder(String caseId) {
        CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(caseId);
        caseDocumentHelper.addCaseDocument(
                        Json.createObjectBuilder().add("id", UUID.randomUUID().toString())
                                        .add("materialId", UUID.randomUUID().toString())
                                        .add("documentType", "RESULT_ORDER").build().toString());
        return caseDocumentHelper;
    }
}
