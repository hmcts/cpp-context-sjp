package uk.gov.moj.cpp.sjp.event.listener;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.event.TrialRequestCancelled;
import uk.gov.moj.cpp.sjp.persistence.entity.PendingDatesToAvoid;
import uk.gov.moj.cpp.sjp.persistence.repository.PendingDatesToAvoidRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class TrialRequestCancelledListenerTest {

    @Mock
    private PendingDatesToAvoidRepository pendingDatesToAvoidRepository;

    @InjectMocks
    private TrialRequestCancelledListener trialRequestCancelledListener;

    @Captor
    private ArgumentCaptor<PendingDatesToAvoid> pendingDatesToAvoidCaptor;

    private Clock clock = new StoppedClock(ZonedDateTime.now());
    private ZonedDateTime now = clock.now();

    @Test
    public void shouldRemoveFromDatesToAvoidWhenTrialRequestCancelledEvent() {
        final UUID caseId = UUID.randomUUID();
        final JsonEnvelope event = envelope()
                .withPayloadOf(caseId.toString(), "caseId")
                .build();

        trialRequestCancelledListener.cancelTrial(event);

        verify(pendingDatesToAvoidRepository).removeByCaseId(caseId);
    }
}
