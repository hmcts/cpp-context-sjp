package uk.gov.moj.cpp.sjp.query.controller;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.controller.service.SjpService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SjpQueryControllerTest {

    @Mock
    private Requester requester;

    @Mock
    private JsonEnvelope query;

    @Mock
    private JsonEnvelope response;

    @Mock
    private SjpService sjpService;

    @InjectMocks
    private SjpQueryController sjpQueryController;

    @Test
    public void shouldHandlesQueries() {
        assertThat(SjpQueryController.class, isHandlerClass(Component.QUERY_CONTROLLER)
                .with(allOf(
                        method("findCaseByUrn").thatHandles("sjp.query.case-by-urn").withRequesterPassThrough(),
                        method("findSjpCaseByUrn").thatHandles("sjp.query.sjp-case-by-urn").withRequesterPassThrough(),
                        method("findFinancialMeans").thatHandles("sjp.query.financial-means").withRequesterPassThrough(),
                        method("findEmployer").thatHandles("sjp.query.employer").withRequesterPassThrough(),
                        method("searchCasesByPersonId").thatHandles("sjp.query.cases-search").withRequesterPassThrough(),
                        method("findCaseSearchResults").thatHandles("sjp.query.case-search-results").withRequesterPassThrough(),
                        method("findCasesMissingSjpn").thatHandles("sjp.query.cases-missing-sjpn").withRequesterPassThrough(),
                        method("findCaseDefendants").thatHandles("sjp.query.case-defendants").withRequesterPassThrough(),
                        method("searchCaseByMaterialId").thatHandles("sjp.query.cases-search-by-material-id").withRequesterPassThrough(),
                        method("getAwaitingCases").thatHandles("sjp.query.awaiting-cases").withRequesterPassThrough(),
                        method("getCasesReferredToCourt").thatHandles("sjp.query.cases-referred-to-court").withRequesterPassThrough(),
                        method("getNotReadyCasesGroupedByAge").thatHandles("sjp.query.not-ready-cases-grouped-by-age").withRequesterPassThrough(),
                        method("getOldestCaseAge").thatHandles("sjp.query.oldest-case-age").withRequesterPassThrough(),
                        method("getResultOrders").thatHandles("sjp.query.result-orders").withRequesterPassThrough()
                )));
    }

    @Test
    public void shouldFindCaseByUrnPostcode() {

        setupExpectations();
        when(sjpService.getQueryEnvelope(Mockito.anyString(), Mockito.any(JsonEnvelope.class))).thenReturn(query);
        when(sjpService.findPersonByPostcode(Mockito.any(JsonEnvelope.class), Mockito.any(JsonEnvelope.class))).thenReturn(query);
        when(sjpService.buildCaseDetailsResponse(Mockito.anyString(), Mockito.any(JsonEnvelope.class),
                Mockito.any(JsonEnvelope.class))).thenReturn(response);

        JsonEnvelope actualResponse = sjpQueryController.findCaseByUrnPostcode(query);

        checkResult(actualResponse);
    }

    private void setupExpectations() {
        when(requester.request(query)).thenReturn(response);
    }

    private void checkResult(JsonEnvelope actualResponse) {
        verify(requester).request(query);
        assertThat(actualResponse, equalTo(response));
    }

}
