package uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.casemarkedready;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.moj.cpp.sjp.domain.transformation.exception.TransformationException;
import uk.gov.moj.cpp.sjp.domain.transformation.service.SjpEventStoreService;
import uk.gov.moj.cpp.sjp.domain.transformation.service.SjpViewStoreService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseMarkedReadyForDecisionEventTransformerTest {

    private CaseMarkedReadyForDecisionEventTransformer caseMarkedReadyForDecisionEventTransformer;
    private static final String MEDIUM_PRIORITY = "MEDIUM";
    private SjpViewStoreService sjpViewStoreService = mock(SjpViewStoreService.class);
    private SjpEventStoreService sjpEventStoreService = mock(SjpEventStoreService.class);

    @Before
    public void setup() {
        caseMarkedReadyForDecisionEventTransformer = new CaseMarkedReadyForDecisionEventTransformer(sjpViewStoreService, sjpEventStoreService);
        caseMarkedReadyForDecisionEventTransformer.setEnveloper(EnveloperFactory.createEnveloper());
    }

    @Test
    public void shouldNotTransformEventThatIsNotCaseMarkedReadyForDecision() {

        final UUID caseId = randomUUID();
        final JsonEnvelope caseCompleted = envelope(caseId, "PIA", "sjp.event.that.does.not.exist");

        final Action action = caseMarkedReadyForDecisionEventTransformer.actionFor(caseCompleted);

        assertThat("Expected action type is NO_ACTION for event that is not sjp.events.case-marked-ready-for-decision",
                action,
                Matchers.equalTo(Action.NO_ACTION));
    }


    @Test
    public void shouldTransformEventThatIsReadyForMigration() {

        final UUID caseId = randomUUID();
        final JsonEnvelope caseCompleted = envelope(caseId, "PIA", CaseMarkedReadyForDecisionEventTransformer.CASE_MARKED_READY_FOR_DECISION_EVENT_NAME);

        final Action action = caseMarkedReadyForDecisionEventTransformer.actionFor(caseCompleted);

        assertThat("Expected action type is TRANSFORM for event that is flagged as being ready for migration ",
                action,
                Matchers.equalTo(Action.TRANSFORM));
    }

    @Test
    public void shouldTransformEventToCorrectSessionTypeBasedOnCaseReadinessReason() {
        shouldTransformEventThatIsReadyForMigration(CaseReadinessReason.PIA.name(), SessionType.MAGISTRATE);
        shouldTransformEventThatIsReadyForMigration(CaseReadinessReason.PLEADED_GUILTY.name(), SessionType.MAGISTRATE);
        shouldTransformEventThatIsReadyForMigration(CaseReadinessReason.PLEADED_NOT_GUILTY.name(), SessionType.DELEGATED_POWERS);
        shouldTransformEventThatIsReadyForMigration(CaseReadinessReason.PLEADED_GUILTY_REQUEST_HEARING.name(), SessionType.DELEGATED_POWERS);
        shouldTransformEventThatIsReadyForMigration(CaseReadinessReason.WITHDRAWAL_REQUESTED.name(), SessionType.DELEGATED_POWERS);
        shouldTransformEventThatIsReadyForMigration(CaseReadinessReason.UNKNOWN.name(), SessionType.DELEGATED_POWERS);
    }

    @Test
    public void shouldCreateExpiredEventWhenTheResponseTimeIsPast() {

        final UUID caseId = randomUUID();
        final String reason = CaseReadinessReason.PIA.name();
        final SessionType sessionType = SessionType.MAGISTRATE;
        final LocalDate postingDate = LocalDate.now().minusDays(30);

        when(sjpViewStoreService.getPostingDate(caseId.toString())).thenReturn(Optional.of(postingDate));

        final JsonEnvelope caseMarkedReadyForDecision = envelope(caseId, reason, CaseMarkedReadyForDecisionEventTransformer.CASE_MARKED_READY_FOR_DECISION_EVENT_NAME);

        final CaseMarkedReadyReadyMetaData caseMarkedReadyReadyMetaData =
                new CaseMarkedReadyReadyMetaData(caseMarkedReadyForDecision.metadata().id().toString(), LocalDate.now());
        final List<CaseMarkedReadyReadyMetaData> caseMarkedReadyReadyMetaDataList = new ArrayList<>();
        caseMarkedReadyReadyMetaDataList.add(caseMarkedReadyReadyMetaData);

        when(sjpEventStoreService.getMarkedReadyForDecisionMetadata(caseId.toString())).thenReturn(caseMarkedReadyReadyMetaDataList);

        final Stream<JsonEnvelope> transformedEventsStream = caseMarkedReadyForDecisionEventTransformer.apply(caseMarkedReadyForDecision);

        final List<JsonEnvelope> events = transformedEventsStream.collect(Collectors.toList());

        assertThat(events.size(), is(2));
        assertThat(events.get(0).metadata().name(), is("sjp.events.defendant-response-timer-expired"));
        assertThat(events.get(0).metadata().id(), is(notNullValue()));
        assertThat(events.get(0).payloadAsJsonObject().getString("caseId"), is(caseId.toString()));
    }

    @Test(expected = TransformationException.class)
    public void shouldSkipMigrationOfEventIfNoReasonFound() {

        final UUID caseId = randomUUID();
        final JsonEnvelope caseMarkedReadyForDecision = envelope(caseId, "", CaseMarkedReadyForDecisionEventTransformer.CASE_MARKED_READY_FOR_DECISION_EVENT_NAME);

        caseMarkedReadyForDecisionEventTransformer.apply(caseMarkedReadyForDecision);
    }

    private void shouldTransformEventThatIsReadyForMigration(final String reason, final SessionType sessionType) {

        final UUID caseId = randomUUID();
        when(sjpViewStoreService.getPostingDate(caseId.toString())).thenReturn(Optional.empty());
        final JsonEnvelope caseMarkedReadyForDecision = envelope(caseId, reason, CaseMarkedReadyForDecisionEventTransformer.CASE_MARKED_READY_FOR_DECISION_EVENT_NAME);

        Stream<JsonEnvelope> transformedEventsStream =  caseMarkedReadyForDecisionEventTransformer.apply(caseMarkedReadyForDecision);

        assertThat(String.format("Expected outcome for reason %s is to set sessionType to %s", reason, sessionType.name()),
                transformedEventsStream, streamContaining(
                JsonEnvelopeMatcher.jsonEnvelope(
                        metadata().withName(CaseMarkedReadyForDecisionEventTransformer.CASE_MARKED_READY_FOR_DECISION_EVENT_NAME),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.reason", equalTo(reason)),
                                withJsonPath("$.markedAt", equalTo("2019-09-05T13:24:27.839Z")),
                                withJsonPath("$.sessionType", equalTo(sessionType.name())),
                                withJsonPath("$.priority", equalTo(MEDIUM_PRIORITY))
                        ))
                )
        ));
    }

    private JsonEnvelope envelope(final UUID caseId, final String reason, final String eventName) {
        return createEnvelope(
                eventName,
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("reason", reason)
                        .add("markedAt", "2019-09-05T13:24:27.839Z")
                        .build()
        );
    }

    @Test
    public void shouldCreate28DaysTimerExpiredOnFirstReadyForDecisionEventWhenTheReadyForDecisionIsFiredForThe28DaysTimerExpiredReason() {

        final UUID caseId = randomUUID();
        final String reason = CaseReadinessReason.PIA.name();
        final SessionType sessionType = SessionType.MAGISTRATE;
        final LocalDate postingDate = LocalDate.now().minusDays(30);

        when(sjpViewStoreService.getPostingDate(caseId.toString())).thenReturn(Optional.of(postingDate));

        JsonEnvelope caseMarkedReadyForDecision = envelope(caseId, reason, CaseMarkedReadyForDecisionEventTransformer.CASE_MARKED_READY_FOR_DECISION_EVENT_NAME);

        final List<CaseMarkedReadyReadyMetaData> caseMarkedReadyReadyMetaDataList = new ArrayList<>();
        CaseMarkedReadyReadyMetaData caseMarkedReadyReadyMetaData =
                new CaseMarkedReadyReadyMetaData(caseMarkedReadyForDecision.metadata().id().toString(), LocalDate.now());
        caseMarkedReadyReadyMetaDataList.add(caseMarkedReadyReadyMetaData);
        caseMarkedReadyReadyMetaData =
                new CaseMarkedReadyReadyMetaData(UUID.randomUUID().toString(), LocalDate.now());
        caseMarkedReadyReadyMetaDataList.add(caseMarkedReadyReadyMetaData);

        when(sjpEventStoreService.getMarkedReadyForDecisionMetadata(caseId.toString())).thenReturn(caseMarkedReadyReadyMetaDataList);

        Stream<JsonEnvelope> transformedEventsStream = caseMarkedReadyForDecisionEventTransformer.apply(caseMarkedReadyForDecision);
        List<JsonEnvelope> events = transformedEventsStream.collect(Collectors.toList());

        assertThat(events.size(), is(2));
        assertThat(events.get(0).metadata().name(), is("sjp.events.defendant-response-timer-expired"));
        assertThat(events.get(0).metadata().id(), is(notNullValue()));
        assertThat(events.get(0).payloadAsJsonObject().getString("caseId"), is(caseId.toString()));


        assertThat(events.get(1).metadata().name(), is(CaseMarkedReadyForDecisionEventTransformer.CASE_MARKED_READY_FOR_DECISION_EVENT_NAME));
        assertThat(events.get(1).metadata().id().toString(), is(caseMarkedReadyForDecision.metadata().id().toString()));
    }

    @Test
    public void shouldCreate28DaysTimerExpiredOnSecondReadyForDecisionEventWhenTheReadyForDecisionIsFiredForThe28DaysTimerExpiredReason() {

        final UUID caseId = randomUUID();
        final String reason = CaseReadinessReason.PIA.name();
        final SessionType sessionType = SessionType.MAGISTRATE;
        final LocalDate postingDate = LocalDate.now().minusDays(30);

        when(sjpViewStoreService.getPostingDate(caseId.toString())).thenReturn(Optional.of(postingDate));

        JsonEnvelope caseMarkedReadyForDecision = envelope(caseId, reason, CaseMarkedReadyForDecisionEventTransformer.CASE_MARKED_READY_FOR_DECISION_EVENT_NAME);

        final List<CaseMarkedReadyReadyMetaData> caseMarkedReadyReadyMetaDataList = new ArrayList<>();
        CaseMarkedReadyReadyMetaData caseMarkedReadyReadyMetaData =
                new CaseMarkedReadyReadyMetaData(UUID.randomUUID().toString(), LocalDate.now().minusDays(10));
        caseMarkedReadyReadyMetaDataList.add(caseMarkedReadyReadyMetaData);
        caseMarkedReadyReadyMetaData =
                new CaseMarkedReadyReadyMetaData(caseMarkedReadyForDecision.metadata().id().toString(), LocalDate.now());
        caseMarkedReadyReadyMetaDataList.add(caseMarkedReadyReadyMetaData);

        when(sjpEventStoreService.getMarkedReadyForDecisionMetadata(caseId.toString())).thenReturn(caseMarkedReadyReadyMetaDataList);

        Stream<JsonEnvelope> transformedEventsStream = caseMarkedReadyForDecisionEventTransformer.apply(caseMarkedReadyForDecision);
        List<JsonEnvelope> events = transformedEventsStream.collect(Collectors.toList());

        assertThat(events.size(), is(2));
        assertThat(events.get(0).metadata().name(), is("sjp.events.defendant-response-timer-expired"));
        assertThat(events.get(0).metadata().id(), is(notNullValue()));
        assertThat(events.get(0).payloadAsJsonObject().getString("caseId"), is(caseId.toString()));


        assertThat(events.get(1).metadata().name(), is(CaseMarkedReadyForDecisionEventTransformer.CASE_MARKED_READY_FOR_DECISION_EVENT_NAME));
        assertThat(events.get(1).metadata().id().toString(), is(caseMarkedReadyForDecision.metadata().id().toString()));
    }

    @Test
    public void shouldNotCreate28DaysTimerExpiredOnSecondReadyForDecisionEventWhenTheReadyForDecisionIsFiredForThe28DaysTimerExpiredReason() {

        final UUID caseId = randomUUID();
        final String reason = CaseReadinessReason.PIA.name();
        final SessionType sessionType = SessionType.MAGISTRATE;
        final LocalDate postingDate = LocalDate.now().minusDays(30);

        when(sjpViewStoreService.getPostingDate(caseId.toString())).thenReturn(Optional.of(postingDate));

        JsonEnvelope caseMarkedReadyForDecision = envelope(caseId, reason, CaseMarkedReadyForDecisionEventTransformer.CASE_MARKED_READY_FOR_DECISION_EVENT_NAME);

        final List<CaseMarkedReadyReadyMetaData> caseMarkedReadyReadyMetaDataList = new ArrayList<>();
        CaseMarkedReadyReadyMetaData caseMarkedReadyReadyMetaData =
                new CaseMarkedReadyReadyMetaData(UUID.randomUUID().toString(), LocalDate.now());
        caseMarkedReadyReadyMetaDataList.add(caseMarkedReadyReadyMetaData);
        caseMarkedReadyReadyMetaData =
                new CaseMarkedReadyReadyMetaData(caseMarkedReadyForDecision.metadata().id().toString(), LocalDate.now());
        caseMarkedReadyReadyMetaDataList.add(caseMarkedReadyReadyMetaData);

        when(sjpEventStoreService.getMarkedReadyForDecisionMetadata(caseId.toString())).thenReturn(caseMarkedReadyReadyMetaDataList);

        Stream<JsonEnvelope> transformedEventsStream = caseMarkedReadyForDecisionEventTransformer.apply(caseMarkedReadyForDecision);
        List<JsonEnvelope> events = transformedEventsStream.collect(Collectors.toList());

        assertThat(events.size(), is(1));
        assertThat(events.get(0).metadata().name(), is(CaseMarkedReadyForDecisionEventTransformer.CASE_MARKED_READY_FOR_DECISION_EVENT_NAME));
        assertThat(events.get(0).metadata().id().toString(), is(caseMarkedReadyForDecision.metadata().id().toString()));
    }

}
