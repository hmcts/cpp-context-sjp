package uk.gov.moj.cpp.sjp.query.api;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;

@SuppressWarnings({"unused", "WeakerAccess"})
@ServiceComponent(Component.QUERY_API)
public class TransparencyReportContentApi {

    @Handles("sjp.query.transparency-report-content")
    public JsonEnvelope getTransparencyReportContent(final JsonEnvelope envelope) {
        return envelope;
    }
}
