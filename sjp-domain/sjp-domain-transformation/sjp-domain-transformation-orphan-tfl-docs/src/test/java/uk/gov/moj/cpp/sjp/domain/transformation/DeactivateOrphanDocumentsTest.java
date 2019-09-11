package uk.gov.moj.cpp.sjp.domain.transformation;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.DEACTIVATE;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;

import javax.json.JsonValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DeactivateOrphanDocumentsTest {

    @InjectMocks
    private DeactivateOrphanDocuments transformer;

    @Test
    public void shouldNotProcessEventForNonRelatedEventName() {
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.case-document-deleted"),
                JsonValue.NULL);
        final Action action = whenTransformerActionIsCheckedFor(envelope);
        assertThat(action, is(NO_ACTION));
    }

    @Test
    public void shouldNotProcessEventForNotFirstPositionCaseDocumentAddedEvent() {
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.case-document-added").withPosition(2l),
                JsonValue.NULL);
        final Action action = whenTransformerActionIsCheckedFor(envelope);
        assertThat(action, is(NO_ACTION));
    }

    @Test
    public void shouldProcessEventForFirstPositionCaseDocumentAddedEvent() {
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.case-document-added").withPosition(1l),
                JsonValue.NULL);
        final Action action = whenTransformerActionIsCheckedFor(envelope);
        assertThat(action, is(DEACTIVATE));
    }

    private Action whenTransformerActionIsCheckedFor(final JsonEnvelope envelope) {
        return transformer.actionFor(envelope);
    }
}