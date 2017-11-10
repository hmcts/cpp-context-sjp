package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.cpp.sjp.event.processor.CaseUpdateRejectedProcessor;

import java.util.UUID;

import javax.json.Json;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseUpdateRejectedProcessorTest {

    private static final String CASE_ID = UUID.randomUUID().toString();

    @InjectMocks
    private CaseUpdateRejectedProcessor caseUpdateRejectedProcessor;
    @Mock
    private Sender sender;
    @Captor
    private ArgumentCaptor<JsonEnvelope> captor;
    @Spy
    private Enveloper envelopers = createEnveloper();

    @Test
    public void caseUpdateRejectedPublicEvent() throws Exception {

        final JsonEnvelope privateEvent = createEnvelope("structure.events.case-update-rejected",
                Json.createObjectBuilder()
                        .add("caseId", CASE_ID)
                        .add("reason", CaseUpdateRejected.RejectReason.CASE_COMPLETED.name())
                        .build());
        caseUpdateRejectedProcessor.caseUpdateRejected(privateEvent);

        verify(sender).send(captor.capture());

        final JsonEnvelope publicEvent = captor.getValue();

        assertThat(publicEvent, jsonEnvelope(
                metadata().withName("public.structure.case-update-rejected"),
                payloadIsJson(allOf(withJsonPath("$.caseId", equalTo(CASE_ID)),
                        withJsonPath("$.reason", equalTo(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name()))))
        ));
    }
}