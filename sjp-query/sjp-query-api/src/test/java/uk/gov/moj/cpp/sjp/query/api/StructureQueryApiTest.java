package uk.gov.moj.cpp.sjp.query.api;

import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.core.annotation.Component;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StructureQueryApiTest {

    @Test
    public void shouldHandlesQueries() {
        assertThat(SjpQueryApi.class, isHandlerClass(Component.QUERY_API)
                .with(allOf(
                        method("findSjpCaseByUrn").thatHandles("sjp.query.sjp-case-by-urn").withRequesterPassThrough(),
                        method("getNotReadyCasesGroupedByAge").thatHandles("sjp.query.not-ready-cases-grouped-by-age").withRequesterPassThrough(),
                        method("getOldestCaseAge").thatHandles("sjp.query.oldest-case-age").withRequesterPassThrough(),
                        method("findCase").thatHandles("sjp.query.case").withRequesterPassThrough(),
                        method("findCaseByUrn").thatHandles("sjp.query.case-by-urn").withRequesterPassThrough(),
                        method("findSjpCaseByUrn").thatHandles("sjp.query.sjp-case-by-urn").withRequesterPassThrough(),
                        method("findCaseByUrnPostcode").thatHandles("sjp.query.case-by-urn-postcode").withRequesterPassThrough(),
                        method("findFinancialMeans").thatHandles("sjp.query.financial-means").withRequesterPassThrough(),
                        method("findEmployer").thatHandles("sjp.query.employer").withRequesterPassThrough(),
                        method("searchCasesByPersonId").thatHandles("sjp.query.cases-search").withRequesterPassThrough(),
                        method("findCaseSearchResults").thatHandles("sjp.query.case-search-results").withRequesterPassThrough(),
                        method("findCasesMissingSjpn").thatHandles("sjp.query.cases-missing-sjpn").withRequesterPassThrough(),
                        method("findCasesMissingSjpnWithDetails").thatHandles("sjp.query.cases-missing-sjpn-with-details").withRequesterPassThrough(),
                        method("findCaseDocuments").thatHandles("sjp.query.case-documents").withRequesterPassThrough(),
                        method("findCaseDefendants").thatHandles("sjp.query.case-defendants").withRequesterPassThrough(),
                        method("searchCaseByMaterialId").thatHandles("sjp.query.cases-search-by-material-id").withRequesterPassThrough(),
                        method("getAwaitingCases").thatHandles("sjp.query.awaiting-cases").withRequesterPassThrough(),
                        method("getCasesReferredToCourt").thatHandles("sjp.query.cases-referred-to-court").withRequesterPassThrough(),
                        method("getResultOrders").thatHandles("sjp.query.result-orders").withRequesterPassThrough()
                )));
    }
}
