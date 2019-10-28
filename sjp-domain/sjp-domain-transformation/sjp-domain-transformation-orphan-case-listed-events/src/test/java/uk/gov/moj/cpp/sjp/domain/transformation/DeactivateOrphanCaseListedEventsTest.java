package uk.gov.moj.cpp.sjp.domain.transformation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;

import javax.json.JsonValue;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.DEACTIVATE;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;

@RunWith(MockitoJUnitRunner.class)
public class DeactivateOrphanCaseListedEventsTest {

    private static final String EVENT_CASE_LISTED_IN_CRIMINAL_COURTS = "sjp.events.case-listed-in-criminal-courts";

    @InjectMocks
    private DeactivateOrphanCaseListedEvents transformer;

    @Test
    public void shouldNotProcessEventForNonRelatedEventName() {
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("unrelated-event"),
                JsonValue.NULL);
        final Action action = whenTransformerActionIsCheckedFor(envelope);
        assertThat(action, is(NO_ACTION));
    }

    @Test
    public void shouldNotProcessEventForOtherThanFirstPosition() {
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID(EVENT_CASE_LISTED_IN_CRIMINAL_COURTS).withPosition(2L),
                JsonValue.NULL);
        final Action action = whenTransformerActionIsCheckedFor(envelope);
        assertThat(action, is(NO_ACTION));
    }

    @Test
    public void shouldProcessEventForFirstPositionCaseDocumentAddedEvent() {
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID(EVENT_CASE_LISTED_IN_CRIMINAL_COURTS).withPosition(1L),
                JsonValue.NULL);
        final Action action = whenTransformerActionIsCheckedFor(envelope);
        assertThat(action, is(DEACTIVATE));
    }

    private Action whenTransformerActionIsCheckedFor(final JsonEnvelope envelope) {
        return transformer.actionFor(envelope);
    }
}