package uk.gov.moj.cpp.sjp.query.api;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;

/**
 * Dummy endpoint ncessary for interceptor chain processor to work.
 */
@SuppressWarnings({"unused"})
@ServiceComponent(Component.QUERY_API)
public class CourtExtractContentApi {

    @Handles("sjp.query.case-court-extract")
    public JsonEnvelope dummyCourtExtract(final JsonEnvelope envelope) {
        return envelope;
    }

}
