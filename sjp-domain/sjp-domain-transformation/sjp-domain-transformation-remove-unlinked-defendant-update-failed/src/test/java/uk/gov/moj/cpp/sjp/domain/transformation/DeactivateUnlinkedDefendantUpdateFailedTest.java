package uk.gov.moj.cpp.sjp.domain.transformation;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.DEACTIVATE;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.moj.cpp.sjp.domain.transformation.service.SjpEventStoreService;

import java.util.UUID;

import javax.json.JsonValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DeactivateUnlinkedDefendantUpdateFailedTest {

    @Mock
    private SjpEventStoreService sjpEventStoreService;

    @InjectMocks
    private DeactivateUnlinkedDefendantUpdateFailed transformer;

    @Test
    public void shouldNotProcessEventForNonRelatedEventName() {
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.case-document-deleted"),
                JsonValue.NULL);
        final Action action = whenTransformerActionIsCheckedFor(envelope);
        assertThat(action, is(NO_ACTION));
    }

    @Test
    public void shouldNotProcessEventForNonUnlinkedEvent() {
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.defendant-update-failed").withPosition(76l)
                .withStreamId(UUID.randomUUID()),
                JsonValue.NULL);
        when(sjpEventStoreService.hasInitialEventInStream(anyString())).thenReturn(true);
        final Action action = whenTransformerActionIsCheckedFor(envelope);
        assertThat(action, is(NO_ACTION));
    }

    @Test
    public void shouldDeactivateEventForUnlinkedEvent() {
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.defendant-update-failed")
                        .withPosition(76l)
                        .withStreamId(UUID.randomUUID()),
                        JsonValue.NULL);
        when(sjpEventStoreService.hasInitialEventInStream(anyString())).thenReturn(false);
        final Action action = whenTransformerActionIsCheckedFor(envelope);
        assertThat(action, is(DEACTIVATE));
    }

    private Action whenTransformerActionIsCheckedFor(final JsonEnvelope envelope) {
        return transformer.actionFor(envelope);
    }
}
