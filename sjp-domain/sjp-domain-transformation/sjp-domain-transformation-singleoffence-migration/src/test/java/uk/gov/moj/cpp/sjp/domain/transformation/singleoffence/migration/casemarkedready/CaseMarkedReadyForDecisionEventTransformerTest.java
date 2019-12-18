package uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.casemarkedready;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.TransformationException;
import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.service.SjpViewStoreService;

import java.util.UUID;
import java.util.stream.Stream;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

@RunWith(MockitoJUnitRunner.class)
public class CaseMarkedReadyForDecisionEventTransformerTest {

    private CaseMarkedReadyForDecisionEventTransformer caseMarkedReadyForDecisionEventTransformer;
    private static final String MEDIUM_PRIORITY = "MEDIUM";

    @Mock
    private SjpViewStoreService sjpViewStoreService;

    @Before
    public void setup() {
        caseMarkedReadyForDecisionEventTransformer =
                new CaseMarkedReadyForDecisionEventTransformer(sjpViewStoreService);
        caseMarkedReadyForDecisionEventTransformer.setEnveloper(EnveloperFactory.createEnveloper());
    }

    @Test
    public void shouldNotTransformEventThatIsNotCaseMarkedReadyForDecision() {

        final UUID caseId = randomUUID();
        final JsonEnvelope caseCompleted = jsonEnvelope(caseId, "PIA", "sjp.event.that.does.not.exist");

        final Action action = caseMarkedReadyForDecisionEventTransformer.actionFor(caseCompleted);

        assertThat("Expected action type is NO_ACTION for event that is not sjp.events.case-marked-ready-for-decision",
                action,
                Matchers.equalTo(Action.NO_ACTION));
    }

    @Test
    public void shouldNotTransformEventThatIsNotReadyForMigration() {

        final UUID caseId = randomUUID();
        final JsonEnvelope caseCompleted = jsonEnvelope(caseId, "PIA", CaseMarkedReadyForDecisionEventTransformer.CASE_MARKED_READY_FOR_DECISION_EVENT_NAME);
        when(sjpViewStoreService.getWhetherCaseIsCandidateForMigration(caseId.toString())).thenReturn(false);

        final Action action = caseMarkedReadyForDecisionEventTransformer.actionFor(caseCompleted);

        assertThat("Expected action type is NO_ACTION for event that is not flagged as being ready for migration ",
                action,
                Matchers.equalTo(Action.NO_ACTION));
    }

    @Test
    public void shouldTransformEventThatIsReadyForMigration() {

        final UUID caseId = randomUUID();
        final JsonEnvelope caseCompleted = jsonEnvelope(caseId, "PIA", CaseMarkedReadyForDecisionEventTransformer.CASE_MARKED_READY_FOR_DECISION_EVENT_NAME);
        when(sjpViewStoreService.getWhetherCaseIsCandidateForMigration(caseId.toString())).thenReturn(true);

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

    @Test(expected = TransformationException.class)
    public void shouldSkipMigrationOfEventIfNoReasonFound() {

        final UUID caseId = randomUUID();
        final JsonEnvelope caseMarkedReadyForDecision = jsonEnvelope(caseId, "", CaseMarkedReadyForDecisionEventTransformer.CASE_MARKED_READY_FOR_DECISION_EVENT_NAME);

        caseMarkedReadyForDecisionEventTransformer.apply(caseMarkedReadyForDecision);
    }

    private void shouldTransformEventThatIsReadyForMigration(final String reason, final SessionType sessionType) {

        final UUID caseId = randomUUID();
        final JsonEnvelope caseMarkedReadyForDecision = jsonEnvelope(caseId, reason, CaseMarkedReadyForDecisionEventTransformer.CASE_MARKED_READY_FOR_DECISION_EVENT_NAME);

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

    private JsonEnvelope jsonEnvelope(final UUID caseId, final String reason, final String eventName) {
        return createEnvelope(
                eventName,
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("reason", reason)
                        .add("markedAt", "2019-09-05T13:24:27.839Z")
                        .build()
        );
    }
}
