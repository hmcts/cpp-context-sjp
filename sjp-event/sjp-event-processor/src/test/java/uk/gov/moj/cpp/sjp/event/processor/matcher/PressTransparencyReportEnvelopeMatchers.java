package uk.gov.moj.cpp.sjp.event.processor.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;

import uk.gov.justice.services.messaging.JsonEnvelope;

public class PressTransparencyReportEnvelopeMatchers {

    public static JsonEnvelope reportFailedCommand(final String pressTransparencyReportId) {
        return argThat(jsonEnvelope(
                metadata().withName("sjp.command.press-transparency-report-failed"),
                payload().isJson(allOf(
                        withJsonPath("$.pressTransparencyReportId", equalTo(pressTransparencyReportId))
                ))));
    }
}
