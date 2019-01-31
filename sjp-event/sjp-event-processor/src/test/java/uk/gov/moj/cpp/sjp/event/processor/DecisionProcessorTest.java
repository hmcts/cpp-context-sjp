package uk.gov.moj.cpp.sjp.event.processor;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;

import java.util.UUID;

import javax.json.Json;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DecisionProcessorTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @Mock
    private CaseStateService caseStateService;

    @InjectMocks
    private DecisionProcessor caseDecisionListener;

    private UUID caseId = randomUUID();

    @Test
    public void shouldUpdateCaseState() {

        final JsonEnvelope event = createEnvelope("public.resulting.referenced-decisions-saved",
                Json.createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("resultedOn", now().toString())
                        .add("sjpSessionId", randomUUID().toString())
                        .build());
        caseDecisionListener.referencedDecisionsSaved(event);

        verify(caseStateService).caseCompleted(caseId, event.metadata());
    }

}