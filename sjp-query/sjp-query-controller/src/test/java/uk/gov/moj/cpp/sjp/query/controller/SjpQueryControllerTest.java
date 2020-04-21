package uk.gov.moj.cpp.sjp.query.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.annotation.Component;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

@RunWith(MockitoJUnitRunner.class)
public class SjpQueryControllerTest {

    @Test
    public void shouldHandlesQueries() {
        assertThat(SjpQueryController.class, isHandlerClass(Component.QUERY_CONTROLLER)
                .with(allOf(
                        method("findCaseByUrn").thatHandles("sjp.query.case-by-urn").withRequesterPassThrough(),
                        method("findFinancialMeans").thatHandles("sjp.query.financial-means").withRequesterPassThrough(),
                        method("findEmployer").thatHandles("sjp.query.employer").withRequesterPassThrough(),
                        method("findCaseSearchResults").thatHandles("sjp.query.case-search-results").withRequesterPassThrough(),
                        method("findCasesMissingSjpn").thatHandles("sjp.query.cases-missing-sjpn").withRequesterPassThrough(),
                        method("searchCaseByMaterialId").thatHandles("sjp.query.cases-search-by-material-id").withRequesterPassThrough(),
                        method("getResultOrders").thatHandles("sjp.query.result-orders").withRequesterPassThrough(),
                        method("getReadyCases").thatHandles("sjp.query.ready-cases").withRequesterPassThrough(),
                        method("getCaseAssignment").thatHandles("sjp.query.case-assignment").withRequesterPassThrough(),
                        method("getProsecutingAuthority").thatHandles("sjp.query.case-prosecuting-authority").withRequesterPassThrough(),
                        method("getDefendantDetailsUpdates").thatHandles("sjp.query.defendant-details-updates").withRequesterPassThrough(),
                        method("getCaseNotes").thatHandles("sjp.query.case-notes").withRequesterPassThrough(),
                        method("getTransparencyReportMetadata").thatHandles("sjp.query.transparency-report-metadata").withRequesterPassThrough(),
                        method("getCourtExtract").thatHandles("sjp.query.case-court-extract").withRequesterPassThrough()
                )));
    }
}
