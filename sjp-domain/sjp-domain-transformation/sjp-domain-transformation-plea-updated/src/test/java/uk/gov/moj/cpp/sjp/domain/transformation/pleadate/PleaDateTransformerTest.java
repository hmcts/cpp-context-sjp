package uk.gov.moj.cpp.sjp.domain.transformation.pleadate;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.iterableWithSize;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PleaDateTransformerTest {

    private static final String PLEA_UPDATED_EVENT_NAME = "sjp.events.plea-updated";
    private static final String CASE_ID = UUID.randomUUID().toString();
    private static final String OFFENCE_ID = UUID.randomUUID().toString();
    private static final String PLEA = "NOT_GUILTY";
    private static final String PLEA_METHOD = "ONLINE";
    private static final String UPDATED_DATE = "2017-02-05T15:14:29.894Z";

    @InjectMocks
    private PleaDateTransformer transformer;

    @Test
    public void shouldIgnoreIrrelevantEvents() {
        String irrelevantEventName = "irrelevantEventName";
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(irrelevantEventName), JsonValue.NULL);

        final Action action = transformer.actionFor(envelope);
        assertThat(action, CoreMatchers.is(NO_ACTION));
    }

    @Test
    public void shouldIgnorePleaUpdatedEventsWithPleaUpdated() {

        final Action action = transformer.actionFor(buildPleaUpdatedEnvelope(true));
        assertThat(action, CoreMatchers.is(NO_ACTION));
    }

    @Test
    public void shouldTransformPleaUpdatedEventsWithoutPleaUpdated() {

        final Action action = transformer.actionFor(buildPleaUpdatedEnvelope(false));
        assertThat(action, CoreMatchers.is(TRANSFORM));
    }

    @Test
    public void shouldEnrichEventsWithoutPleaUpdated() {

        JsonEnvelope originalEventEnvelope = buildPleaUpdatedEnvelope(false);

        final Stream<JsonEnvelope> jsonEnvelopeStream = transformer.apply(
                originalEventEnvelope);

        final List<JsonEnvelope> events = jsonEnvelopeStream.collect(toList());

        assertThat(events, iterableWithSize(1));

        assertThat(events.get(0), jsonEnvelope(
                metadata().of(originalEventEnvelope.metadata()),
                payloadIsJson(allOf(
                        withJsonPath("caseId", is(CASE_ID)),
                        withJsonPath("offenceId", is(OFFENCE_ID)),
                        withJsonPath("plea", is(PLEA)),
                        withJsonPath("pleaMethod", is(PLEA_METHOD)),
                        withJsonPath("updatedDate", is(UPDATED_DATE))
                ))));
    }

    private JsonEnvelope buildPleaUpdatedEnvelope(final boolean hasUpdatedDate) {
        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add("caseId", CASE_ID)
                .add("offenceId", OFFENCE_ID)
                .add("plea", PLEA)
                .add("pleaMethod", PLEA_METHOD);

        if (hasUpdatedDate) {
            payloadBuilder.add("updatedDate", UPDATED_DATE);
        }

        return envelopeFrom(metadataWithRandomUUID(PLEA_UPDATED_EVENT_NAME)
                .createdAt(ZonedDateTimes.fromString(UPDATED_DATE)), payloadBuilder.build());
    }
}