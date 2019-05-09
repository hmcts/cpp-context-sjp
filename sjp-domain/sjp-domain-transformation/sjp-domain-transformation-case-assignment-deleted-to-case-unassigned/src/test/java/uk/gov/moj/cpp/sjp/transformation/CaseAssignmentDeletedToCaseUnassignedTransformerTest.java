package uk.gov.moj.cpp.sjp.transformation;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;
import static uk.gov.moj.cpp.sjp.transformation.CaseAssignmentDeletedToCaseUnassignedTransformer.*;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.time.ZonedDateTime;
import java.util.List;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;

public class CaseAssignmentDeletedToCaseUnassignedTransformerTest {

    private CaseAssignmentDeletedToCaseUnassignedTransformer transformer = new CaseAssignmentDeletedToCaseUnassignedTransformer();

    private String caseId = randomUUID().toString();

    private JsonEnvelope oldJsonEnvelope;

    @Before
    public void onceBeforeEachTest() {
        oldJsonEnvelope = envelopeFrom(metadataWithRandomUUID(CASE_ASSIGNMENT_DELETED_EVENT)
                        .withUserId(randomUUID().toString())
                        .createdAt(ZonedDateTime.now())
                        .withClientCorrelationId(randomUUID().toString())
                        .withCausation(randomUUID(), randomUUID())
                        .build(),
                createObjectBuilder().add("caseId", caseId).build());
    }

    @Test
    public void shouldRaiseTransformActionForOnlyCaseAssignmentDeletedEvent() {
        assertThat(transformer.actionFor(oldJsonEnvelope), is(TRANSFORM));
    }

    @Test
    public void shouldNotRaiseTransformActionForAnyOtherEvent() {
        JsonEnvelope caseAssignmentDeletedEventEnvelope = envelopeFrom(metadataWithRandomUUID("AN_EVENT_SHOULD_NOT_HANDLED").build(),
                createObjectBuilder().add("caseId", caseId).build());
        assertThat(transformer.actionFor(caseAssignmentDeletedEventEnvelope), is(NO_ACTION));
    }

    @Test
    public void shouldCreateNewCaseUnassignedEven() {
        final List<JsonEnvelope> envelopeList = whenTheTransformationIsApplied();
        thenOutputStreamHasOneEvent(envelopeList);
        thenCaseUnassignedEventHasAllTheFieldsSetCorrectly(envelopeList.get(0));
    }

    private void thenOutputStreamHasOneEvent(final List<JsonEnvelope> envelopeList) {
        assertThat(envelopeList, hasSize(1));
    }

    protected void thenCaseUnassignedEventHasAllTheFieldsSetCorrectly(final JsonEnvelope actualJsonEnvelope) {

        final Metadata metadata = actualJsonEnvelope.metadata();
        assertThat(metadata.name(), is(CASE_UNASSIGNED_EVENT));
        assertThat(metadata.userId(), is(oldJsonEnvelope.metadata().userId()));
        assertThat(metadata.createdAt(), is(oldJsonEnvelope.metadata().createdAt()));
        assertThat(metadata.causation(), is(oldJsonEnvelope.metadata().causation()));
        assertThat(metadata.streamId(), is(oldJsonEnvelope.metadata().streamId()));

        final JsonObject caseUnassignedEvent = actualJsonEnvelope.payloadAsJsonObject();
        assertThat(caseUnassignedEvent.getString("caseId"), is(oldJsonEnvelope.payloadAsJsonObject().getString("caseId")));
    }

    protected List<JsonEnvelope> whenTheTransformationIsApplied() {
        return transformer.apply(oldJsonEnvelope).collect(toList());
    }
}