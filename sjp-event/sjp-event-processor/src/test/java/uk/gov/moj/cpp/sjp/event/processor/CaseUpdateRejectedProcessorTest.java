package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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
    public void caseUpdateRejectedPublicEvent() {

        final JsonEnvelope privateEvent = createEnvelope("sjp.events.case-update-rejected",
                createObjectBuilder()
                        .add("caseId", CASE_ID)
                        .add("reason", CaseUpdateRejected.RejectReason.CASE_COMPLETED.name())
                        .build());
        caseUpdateRejectedProcessor.caseUpdateRejected(privateEvent);

        verify(sender).send(captor.capture());

        final JsonEnvelope publicEvent = captor.getValue();

        assertThat(publicEvent, jsonEnvelope(
                metadata().withName("public.sjp.case-update-rejected"),
                payloadIsJson(allOf(withJsonPath("$.caseId", equalTo(CASE_ID)),
                        withJsonPath("$.reason", equalTo(CaseUpdateRejected.RejectReason.CASE_COMPLETED.name()))))
        ));
    }
}