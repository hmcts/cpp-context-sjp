package uk.gov.moj.cpp.sjp.query.view;

import static java.util.UUID.fromString;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.view.service.CaseApplicationService;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.QUERY_VIEW)
public class CaseApplicationView {

    @Inject
    private CaseApplicationService caseApplicationService;

    @Inject
    private Enveloper enveloper;

    private static final String COMMON_CASE_RESULTS_QUERY = "sjp.query.common-case-application";

    private static final String CASE_ID = "caseId";

    @Handles(COMMON_CASE_RESULTS_QUERY)
    @SuppressWarnings("squid:CallToDeprecatedMethod")
    public JsonEnvelope getCaseResults(final JsonEnvelope envelope) {
        final UUID caseId = fromString(envelope.payloadAsJsonObject().getString(CASE_ID));

        final JsonObject commonCaseApplication = caseApplicationService.collateApplicationForResults(caseId);

        return enveloper.withMetadataFrom(envelope, COMMON_CASE_RESULTS_QUERY).apply(commonCaseApplication);
    }

}
