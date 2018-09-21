package uk.gov.moj.cpp.sjp.transformation;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.moj.cpp.sjp.transformation.data.CourtHouseDataSource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MagistrateSessionStartedEventTransformerTest {

    private static final String EVENT_MAGISTRATE_SESSION_STARTED = "sjp.events.magistrate-session-started";
    private static final String COURT_HOUSE_NAME = "Willesden Magistrates' Court";
    private static final String SESSION_ID = randomUUID().toString();
    private static final String USER_ID = randomUUID().toString();
    private static final String LOCAL_JUSTICE_AREA_NATIONAL_COURT_CODE = "testLocalJusticeAreaNationalCourtCode";
    private static final String STARTED_AT = DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now());
    private static final String MAGISTRATE = "test magistrate";

    @InjectMocks
    private MagistrateSessionStartedEventTransformer magistrateSessionStartedEventTransformer;

    @Mock
    private CourtHouseDataSource courtHouseDataSource;

    @Before
    public void setUp() {
        magistrateSessionStartedEventTransformer.setEnveloper(createEnveloper());
    }

    @Test
    public void shouldIgnoreIrrelevantEvents() {
        String irrelevantEventName = "irrelevantEventName";
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(irrelevantEventName), JsonValue.NULL);

        final Action action = magistrateSessionStartedEventTransformer.actionFor(envelope);
        assertThat(action, CoreMatchers.is(NO_ACTION));
    }

    @Test
    public void shouldTransformMagistrateSessionStartedEvent() {
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(EVENT_MAGISTRATE_SESSION_STARTED), JsonValue.NULL);

        final Action action = magistrateSessionStartedEventTransformer.actionFor(envelope);
        assertThat(action, CoreMatchers.is(TRANSFORM));
    }

    @Test
    public void shouldEnrichEventWithCourtHouseCode() {
        final String courtHouseCode = "B01OK";

        when(courtHouseDataSource.getCourtCodeForName(COURT_HOUSE_NAME)).thenReturn(courtHouseCode);
        final Stream<JsonEnvelope> jsonEnvelopeStream = magistrateSessionStartedEventTransformer.apply(buildEnvelope());

        final List<JsonEnvelope> events = jsonEnvelopeStream.collect(toList());

        assertThat(events, iterableWithSize(1));

        assertThat(events.get(0), jsonEnvelope(
                metadata().withName(EVENT_MAGISTRATE_SESSION_STARTED),
                payloadIsJson(allOf(
                        withJsonPath("sessionId", is(SESSION_ID)),
                        withJsonPath("magistrate", is(MAGISTRATE)),
                        withJsonPath("courtHouseCode", is(courtHouseCode)),
                        withJsonPath("userId", is(USER_ID)),
                        withJsonPath("courtHouseName", is(COURT_HOUSE_NAME)),
                        withJsonPath("localJusticeAreaNationalCourtCode", is(LOCAL_JUSTICE_AREA_NATIONAL_COURT_CODE)),
                        withJsonPath("startedAt", is(STARTED_AT))
                ))));
    }

    @Test
    public void shouldIgnoreEventWithCourtCodePresent() {
        final JsonObject payload = createObjectBuilder().add("courtHouseCode", "code").build();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(EVENT_MAGISTRATE_SESSION_STARTED), payload);

        final Stream<JsonEnvelope> jsonEnvelopeStream = magistrateSessionStartedEventTransformer.apply(envelope);

        Optional<JsonEnvelope> firstEvent = jsonEnvelopeStream.findFirst();

        assertTrue(firstEvent.isPresent());
        assertThat(firstEvent.get().payloadAsJsonObject(), is(payload));

        verify(courtHouseDataSource, never()).getCourtCodeForName(anyString());
    }

    private JsonEnvelope buildEnvelope() {
        final JsonObject payload = createObjectBuilder()
                .add("sessionId", SESSION_ID)
                .add("magistrate", MAGISTRATE)
                .add("userId", USER_ID)
                .add("courtHouseName", COURT_HOUSE_NAME)
                .add("localJusticeAreaNationalCourtCode", LOCAL_JUSTICE_AREA_NATIONAL_COURT_CODE)
                .add("startedAt", STARTED_AT)
                .build();

        return envelopeFrom(metadataWithRandomUUID(EVENT_MAGISTRATE_SESSION_STARTED), payload);
    }

}
