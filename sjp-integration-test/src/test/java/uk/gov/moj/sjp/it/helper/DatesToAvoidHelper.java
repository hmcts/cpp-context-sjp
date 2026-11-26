package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonValueIsJsonMatcher.isJson;
import static uk.gov.moj.sjp.it.util.QueueUtil.sendToQueue;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidTimerExpired;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;

public class DatesToAvoidHelper {

    public static void makeDatesToAvoidExpired(final EventListener eventListener, final UUID caseId) {
        eventListener.subscribe(DatesToAvoidTimerExpired.EVENT_NAME)
                .run(() -> sendToQueue("sjp.handler.command", createEnvelope("sjp.command.expire-dates-to-avoid-timer",
                        createObjectBuilder().add("caseId", caseId.toString()).build())));
    }

    public static void verifyDatesToAvoidExpiredEventEmitted(final EventListener eventListener, final UUID caseId) {
        final List<Matcher<? super ReadContext>> matchers = new ArrayList<>();
        matchers.add(withJsonPath("caseId", is(caseId.toString())));
        final Optional<JsonEnvelope> jsonEnvelope = eventListener.popEvent(DatesToAvoidTimerExpired.EVENT_NAME);

        assertThat(jsonEnvelope.get(), jsonEnvelope(
                metadata().withName(DatesToAvoidTimerExpired.EVENT_NAME),
                payload(isJson(allOf(matchers)))
        ));
    }
}
