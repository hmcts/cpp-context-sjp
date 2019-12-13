package uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.casereferredforcourthearing;

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

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.TransformationException;
import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.casereferredforcourthearing.service.ReferenceDecisionSavedResult;
import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.casereferredforcourthearing.service.ResultingEventStoreRepository;

import java.util.UUID;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseReferredForCourtHearingEventTransformerTest {

    private CaseReferredForCourtHearingEventTransformer caseReferredForCourtHearingEventTransformer;
    private int estimatedHearingDuration = 33;
    private String referredAt = "2019-09-05T14:03:11.105Z";

    @Mock
    private ResultingEventStoreRepository resultingEventStoreRepository;

    @Before
    public void setup() {
        caseReferredForCourtHearingEventTransformer = new CaseReferredForCourtHearingEventTransformer(resultingEventStoreRepository);
        caseReferredForCourtHearingEventTransformer.setEnveloper(EnveloperFactory.createEnveloper());
    }

    @Test
    public void shouldNotTransformEventThatIsNotCaseReferredForCourtHearingEvent() {
        final UUID caseId = randomUUID();
        final JsonEnvelope eventThatDoesNotExist = existingCaseReferredForCourtHearingEvent(
                                                        caseId,
                                                        randomUUID(),
                                                        "listing notes",
                                                        "sjp.event.that.does.not.exist");

        final Action action = caseReferredForCourtHearingEventTransformer.actionFor(eventThatDoesNotExist);

        assertThat("Expected action type is NO_ACTION for event that is not sjp.events.case-referred-for-court-hearing",
                action,
                Matchers.equalTo(Action.NO_ACTION));
    }

    @Test
    public void shouldNotTransformEventIfCaseReferredForCourtHearingEventHasAlreadyBeenTransformed() {

        final JsonEnvelope eventAlreadyTransformed = createEnvelope(
                CaseReferredForCourtHearingEventTransformer.CASE_REFERRED_FOR_COURT_HEARING_EVENT_NAME,
                transformedCaseReferredForCourtHearingEvent()
        );

        final Action action = caseReferredForCourtHearingEventTransformer.actionFor(eventAlreadyTransformed);

        assertThat("Expected action type is NO_ACTION for event that is sjp.events.case-referred-for-court-hearing but has already been transformed",
                action,
                Matchers.equalTo(Action.NO_ACTION));
    }

    @Test
    public void shouldTransformEventThatIsCaseReferredForCourtHearingIfHasNotAlreadyBeenTransformed() {
        final UUID caseId = randomUUID();
        final JsonEnvelope caseReferredForCourtHearingEvent = existingCaseReferredForCourtHearingEvent(
                                                                    caseId,
                                                                    randomUUID(),
                                                                    "listing notes",
                                                                    CaseReferredForCourtHearingEventTransformer.CASE_REFERRED_FOR_COURT_HEARING_EVENT_NAME);

        final Action action = caseReferredForCourtHearingEventTransformer.actionFor(caseReferredForCourtHearingEvent);

        assertThat("Expected action type is TRANSFORM for event that is flagged as being ready for migration",
                action,
                Matchers.equalTo(Action.TRANSFORM));
    }

    @Test(expected = TransformationException.class)
    public void shouldSkipMigrationOfEventIfNoResultingEventFound() {

        final UUID caseId = randomUUID();
        final JsonEnvelope caseReferredForCourtHearingEvent = existingCaseReferredForCourtHearingEvent(caseId, randomUUID(), "listing notes", CaseReferredForCourtHearingEventTransformer.CASE_REFERRED_FOR_COURT_HEARING_EVENT_NAME);

        when(resultingEventStoreRepository.getReferencedDecisionSavedEventByCaseId(caseId.toString())).thenReturn(null);

        caseReferredForCourtHearingEventTransformer.apply(caseReferredForCourtHearingEvent);
    }

    @Test
    public void shouldTransformCaseReferredForCourtHearingEvent() {
        shouldSuccessfullyTransformCaseReferredForCourtHearingEvent("FNG", VerdictType.FOUND_NOT_GUILTY);
        shouldSuccessfullyTransformCaseReferredForCourtHearingEvent("PSJ", VerdictType.PROVED_SJP);
        shouldSuccessfullyTransformCaseReferredForCourtHearingEvent("GSJ", VerdictType.FOUND_GUILTY);
        shouldSuccessfullyTransformCaseReferredForCourtHearingEvent("OTHER", VerdictType.NO_VERDICT);
    }

    private void shouldSuccessfullyTransformCaseReferredForCourtHearingEvent(String currentVerdict, VerdictType expectedVerdict) {

        final UUID caseId = randomUUID();
        final UUID decisionId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID referralReasonId = randomUUID();
        final String listingNotes = "listing notes";
        final JsonEnvelope caseReferredForCourtHearingEvent = existingCaseReferredForCourtHearingEvent(caseId, referralReasonId, listingNotes, CaseReferredForCourtHearingEventTransformer.CASE_REFERRED_FOR_COURT_HEARING_EVENT_NAME);

        final JsonObject payload = existingReferencedDecisionSavedEvent(caseId, decisionId, offenceId, currentVerdict);
        final ReferenceDecisionSavedResult referenceDecisionSavedResult = new ReferenceDecisionSavedResult(payload);

        when(resultingEventStoreRepository.getReferencedDecisionSavedEventByCaseId(caseId.toString())).thenReturn(referenceDecisionSavedResult);

        Stream<JsonEnvelope> transformedEventsStream =  caseReferredForCourtHearingEventTransformer.apply(caseReferredForCourtHearingEvent);

    assertThat("Expected outcome should be successfully transformed event",
                transformedEventsStream, streamContaining(
                        JsonEnvelopeMatcher.jsonEnvelope(
                                metadata().withName(CaseReferredForCourtHearingEventTransformer.CASE_REFERRED_FOR_COURT_HEARING_EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                                        withJsonPath("$.referredOffences[0].offenceId", equalTo(offenceId.toString())),
                                        withJsonPath("$.referredOffences[0].verdict", equalTo(expectedVerdict.name())),
                                        withJsonPath("$.estimatedHearingDuration", equalTo(estimatedHearingDuration)),
                                        withJsonPath("$.listingNotes", equalTo(listingNotes)),
                                        withJsonPath("$.referralReasonId", equalTo(referralReasonId.toString())),
                                        withJsonPath("$.referredAt", equalTo(referredAt)),
                                        withJsonPath("$.decisionId", equalTo(decisionId.toString()))
                                ))
                        )
                ));
    }

    private JsonEnvelope existingCaseReferredForCourtHearingEvent(final UUID caseId, final UUID referralReasonId, final String listingNotes, final String eventName) {
        JsonObject jsonObject = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("estimatedHearingDuration", estimatedHearingDuration)
                .add("listingNotes", listingNotes)
                .add("referralReasonId", referralReasonId.toString())
                .add("referredAt", referredAt)
                .add("sessionId", randomUUID().toString())
                .add("urn", "TVL9321582")
                .build();

        return createEnvelope(
                eventName,
                jsonObject
        );
    }

    private JsonObject existingReferencedDecisionSavedEvent(final UUID caseId, final UUID decisionId, final UUID offenceId, final String verdict) {

        final JsonArrayBuilder offencesBuilder = Json.createArrayBuilder();

        offencesBuilder.add(createObjectBuilder()
                .add("id", offenceId.toString()));

        return createObjectBuilder()
                .add("id", decisionId.toString())
                .add("caseId", caseId.toString())
                .add("sjpSessionId", randomUUID().toString())
                .add("created", "2019-09-05T14:08:16.630Z")
                .add("verdict", verdict)
                .add("offences", offencesBuilder)
                .add("accountDivisionCode", "77")
                .add("enforcingCourtCode", "828")
                .build();
    }

    private JsonObject transformedCaseReferredForCourtHearingEvent() {

        final JsonArrayBuilder referredOffencesBuilder = Json.createArrayBuilder();

        referredOffencesBuilder.add(createObjectBuilder()
                .add("offenceId", randomUUID().toString())
                .add("verdict", "FNG"));

        return createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("referredOffences", referredOffencesBuilder)
                .add("estimatedHearingDuration", "30")
                .add("referralReasonId", randomUUID().toString())
                .add("referredAt", referredAt)
                .add("decisionId", randomUUID().toString())
                .add("listingNotes", "listing notes")
                .build();
    }
}
