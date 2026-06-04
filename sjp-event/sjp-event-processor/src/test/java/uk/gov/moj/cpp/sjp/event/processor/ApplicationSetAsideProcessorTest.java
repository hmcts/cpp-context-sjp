package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.decision.ApplicationDecisionSetAside;
import uk.gov.moj.cpp.sjp.event.processor.utils.builders.ApplicationDecisionSetAsideEnvelope;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationSetAsideProcessorTest {

    @Mock
    private Sender sender;
    @InjectMocks
    private ApplicationSetAsideProcessor processor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    private UUID caseId;
    private UUID applicationId;
    private UUID applicationDecisionId;
    private JsonEnvelope envelope;

    @BeforeEach
    public void setUp() {
        caseId = randomUUID();
        applicationId = randomUUID();
        applicationDecisionId = randomUUID();
        final ApplicationDecisionSetAside applicationDecisionSetAside = new ApplicationDecisionSetAside(applicationId, caseId, "TVLXYZ01");
        envelope = ApplicationDecisionSetAsideEnvelope.of(applicationDecisionSetAside);
    }

    @Test
    public void shouldPublishPublicEventOnApplicationSetAside() throws FileServiceException {
        final JsonObject payload = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("applicationID", applicationDecisionId.toString())
                .add("caseUrn", "TVLXYZ01")
                .build();
        envelope = ApplicationDecisionSetAsideEnvelope.of(payload);
        processor.handleApplicationDecisionSetAside(envelope);

        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is("public.sjp.application-decision-set-aside"));
        assertThat(envelopeCaptor.getValue().payloadAsJsonObject(), is(payload));
        assertThat(envelopeCaptor.getValue().payloadAsJsonObject().getString("caseUrn"), is("TVLXYZ01"));
    }
}
