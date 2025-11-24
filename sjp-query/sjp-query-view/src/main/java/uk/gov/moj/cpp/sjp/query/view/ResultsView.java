package uk.gov.moj.cpp.sjp.query.view;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.view.service.ResultsService;

import javax.inject.Inject;
import javax.json.JsonObject;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.QUERY_VIEW)
public class ResultsView {

    @Inject
    private ResultsService resultsService;

    @Inject
    private Enveloper enveloper;

    private static final String CASE_RESULTS_EVENT = "sjp.query.case-results";

    @Handles(CASE_RESULTS_EVENT)
    public JsonEnvelope getCaseResults(final JsonEnvelope envelope) {
        final JsonObject caseResults = resultsService.findCaseResults(envelope);

        return enveloper.withMetadataFrom(envelope, CASE_RESULTS_EVENT).apply(caseResults);
    }
}
