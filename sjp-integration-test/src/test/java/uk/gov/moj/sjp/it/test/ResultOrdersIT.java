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

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseDocumentHelper;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import javax.json.Json;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ResultOrdersIT extends BaseIntegrationTest {

    private CreateCase.CreateCasePayloadBuilder case0;
    private CaseDocumentHelper resultOrder0;

    private CreateCase.CreateCasePayloadBuilder case1;
    private CaseDocumentHelper resultOrder1;

    @Before
    public void givenResultOrders() {
        case0 = createCase();
        resultOrder0 = createResultOrder(case0.getId());

        case1 = createCase();
        resultOrder1 = createResultOrder(case1.getId());
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
        //the order of case0 and case1 is missed as the sorting is descending by the creationg date, so newest are first
        poll(resultOrders).until(status().is(OK), payload().isJson(allOf(
                withJsonPath("$.resultOrders[0].caseId", is(case1.getId().toString())),
                withJsonPath("$.resultOrders[0].urn", is(case1.getUrn())),
                withJsonPath("$.resultOrders[0].defendant.title", is(case1.getDefendantBuilder().getTitle())),
                withJsonPath("$.resultOrders[0].defendant.firstName", is(case1.getDefendantBuilder().getFirstName())),
                withJsonPath("$.resultOrders[0].defendant.lastName", is(case1.getDefendantBuilder().getLastName())),
                withJsonPath("$.resultOrders[0].defendant.dateOfBirth", is(LocalDates.to(case1.getDefendantBuilder().getDateOfBirth()))),
                withJsonPath("$.resultOrders[0].defendant.address.address1", is(case1.getDefendantBuilder().getAddressBuilder().getAddress1())),
                withJsonPath("$.resultOrders[0].defendant.address.address2", is(case1.getDefendantBuilder().getAddressBuilder().getAddress2())),
                withJsonPath("$.resultOrders[0].defendant.address.address3", is(case1.getDefendantBuilder().getAddressBuilder().getAddress3())),
                withJsonPath("$.resultOrders[0].defendant.address.address4", is(case1.getDefendantBuilder().getAddressBuilder().getAddress4())),
                withJsonPath("$.resultOrders[0].defendant.address.postCode", is(case1.getDefendantBuilder().getAddressBuilder().getPostcode())),
                withJsonPath("$.resultOrders[0].order.materialId", is(resultOrder1.getMaterialId())),
                withJsonPath("$.resultOrders[1].caseId", is(case0.getId().toString())),
                withJsonPath("$.resultOrders[1].urn", is(case0.getUrn())),
                withJsonPath("$.resultOrders[1].defendant.title", is(case0.getDefendantBuilder().getTitle())),
                withJsonPath("$.resultOrders[1].defendant.firstName", is(case0.getDefendantBuilder().getFirstName())),
                withJsonPath("$.resultOrders[1].defendant.lastName", is(case0.getDefendantBuilder().getLastName())),
                withJsonPath("$.resultOrders[1].defendant.dateOfBirth", is(LocalDates.to(case0.getDefendantBuilder().getDateOfBirth()))),
                withJsonPath("$.resultOrders[1].defendant.address.address1", is(case0.getDefendantBuilder().getAddressBuilder().getAddress1())),
                withJsonPath("$.resultOrders[1].defendant.address.address2", is(case0.getDefendantBuilder().getAddressBuilder().getAddress2())),
                withJsonPath("$.resultOrders[1].defendant.address.address3", is(case0.getDefendantBuilder().getAddressBuilder().getAddress3())),
                withJsonPath("$.resultOrders[1].defendant.address.address4", is(case0.getDefendantBuilder().getAddressBuilder().getAddress4())),
                withJsonPath("$.resultOrders[1].defendant.address.postCode", is(case0.getDefendantBuilder().getAddressBuilder().getPostcode())),
                withJsonPath("$.resultOrders[1].order.materialId", is(resultOrder0.getMaterialId())))));
    }

    private CreateCase.CreateCasePayloadBuilder createCase() {
        CreateCase.CreateCasePayloadBuilder casePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        CreateCase.createCaseForPayloadBuilder(casePayloadBuilder);
        return casePayloadBuilder;
    }

    private static CaseDocumentHelper createResultOrder(UUID caseId) {
        CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(caseId);
        caseDocumentHelper.addCaseDocument(
                Json.createObjectBuilder().add("id", UUID.randomUUID().toString())
                        .add("materialId", UUID.randomUUID().toString())
                        .add("documentType", "RESULT_ORDER").build().toString());
        return caseDocumentHelper;
    }
}
