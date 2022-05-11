package uk.gov.moj.cpp.sjp.event.listener;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.SocCheck;
import uk.gov.moj.cpp.sjp.persistence.repository.SocCheckRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@SuppressWarnings("WeakerAccess")
@RunWith(MockitoJUnitRunner.class)
public class SocCheckListenerTest {

    @Mock
    private SocCheckRepository socCheckRepository;

    @InjectMocks
    private SocCheckListener listener;

    @Mock
    private JsonEnvelope envelope;

    @Captor
    private ArgumentCaptor<SocCheck> socCheckCaptor;

    @Test
    public void shouldHandleSocCheckEvent() {
        assertThat(SocCheckListener.class, isHandlerClass(EVENT_LISTENER)
                .with(method("handleCaseLegalSocChecked").thatHandles("sjp.events.marked-as-legal-soc-checked")));
    }

    @Test
    public void shouldAddSocCheck() {
        final UUID caseId = UUID.randomUUID();
        final UUID checkedBy = UUID.randomUUID();
        final ZonedDateTime checkedAt = ZonedDateTime.now();

        JsonObject payload = getPayload(checkedBy, caseId, checkedAt);

        when(envelope.payloadAsJsonObject()).thenReturn(payload);

        listener.handleCaseLegalSocChecked(envelope);

        verify(socCheckRepository).save(socCheckCaptor.capture());

        final SocCheck socCheck = socCheckCaptor.getValue();

        assertThat(socCheck.getCaseId(), equalTo(caseId));

    }

    private JsonObject getPayload(final UUID checkedBy, final UUID caseId, final ZonedDateTime now) {
        return createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("checkedBy", checkedBy.toString())
                .add("checkedAt", now.toString()).build();
    }
}
