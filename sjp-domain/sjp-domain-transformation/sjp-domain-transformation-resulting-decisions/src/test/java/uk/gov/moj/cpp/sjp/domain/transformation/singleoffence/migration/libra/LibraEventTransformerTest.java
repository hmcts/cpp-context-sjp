package uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.libra;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.*;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.*;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.libra.LibraEventTransformer.SJP_EVENTS_CASE_REOPENED_IN_LIBRA;
import static uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.libra.LibraEventTransformer.SJP_EVENTS_CASE_REOPENED_IN_LIBRA_UNDONE;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.casemarkedready.CaseMarkedReadyForDecisionEventTransformer;
import uk.gov.moj.cpp.sjp.event.CaseStatusChanged;

import java.util.UUID;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LibraEventTransformerTest {

    private LibraEventTransformer libraEventTransformer = new LibraEventTransformer();

    @Before
    public void setup() {
        libraEventTransformer.setEnveloper(EnveloperFactory.createEnveloper());
    }

    @Test
    public void shouldTransformCaseReopenEvent() {
        final UUID caseId = randomUUID();
        final JsonEnvelope jsonEnvelope = jsonEnvelopeForReopen(caseId);

        final Action action = libraEventTransformer.actionFor(jsonEnvelope);

        assertThat(action, Matchers.equalTo(Action.TRANSFORM));
    }

    @Test
    public void shouldTransformCaseReOpenUndoneEvent() {
        final UUID caseId = randomUUID();
        final JsonEnvelope jsonEnvelope = jsonEnvelopeForReopenUndone(caseId);

        final Action action = libraEventTransformer.actionFor(jsonEnvelope);

        assertThat(action, Matchers.equalTo(Action.TRANSFORM));
    }

    @Test
    public void shouldCreateNewCaseStatusChangedEventForReopen() {
        final UUID caseId = randomUUID();
        final JsonEnvelope jsonEnvelope = jsonEnvelopeForReopen(caseId);

        final Stream<JsonEnvelope> transformedEventsStream = libraEventTransformer.apply(jsonEnvelope);

        assertThat(transformedEventsStream, streamContaining(
                jsonEnvelope(
                        metadata().withName(SJP_EVENTS_CASE_REOPENED_IN_LIBRA),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.reopenedDate", equalTo("2017-01-01")),
                                withJsonPath("$.libraCaseNumber", equalTo("LIBRA12345"))
                        ))

                ), jsonEnvelope(
                        metadata().withName(CaseStatusChanged.EVENT_NAME),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.caseStatus", equalTo(CaseStatus.REOPENED_IN_LIBRA.toString()))
                        ))
                )
        ));
    }

    @Test
    public void shouldCreateNewCaseStatusChangedEventForReopenUndone() {

        final UUID caseId = randomUUID();
        final JsonEnvelope jsonEnvelope = jsonEnvelopeForReopenUndone(caseId);

        final Stream<JsonEnvelope> transformedEventsStream = libraEventTransformer.apply(jsonEnvelope);

        assertThat(transformedEventsStream, streamContaining(
                jsonEnvelope(
                        metadata().withName(SJP_EVENTS_CASE_REOPENED_IN_LIBRA_UNDONE),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.oldReopenedDate", equalTo("2017-01-01"))
                        ))

                ), jsonEnvelope(
                        metadata().withName(CaseStatusChanged.EVENT_NAME),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.caseStatus", equalTo(CaseStatus.COMPLETED.toString()))
                        ))
                )
        ));
    }


    private JsonEnvelope jsonEnvelopeForReopen(final UUID caseId) {

        return createEnvelope(
                "sjp.events.case-reopened-in-libra",
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("reopenedDate", "2017-01-01")
                        .add("libraCaseNumber", "LIBRA12345")
                        .build()
        );
    }

    private JsonEnvelope jsonEnvelopeForReopenUndone(final UUID caseId) {
        return createEnvelope(
                "sjp.events.case-reopened-in-libra-undone",
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("oldReopenedDate", "2017-01-01")
                        .build()
        );
    }


}