package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.event.processor.DecisionListener;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DecisionListenerTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @InjectMocks
    private DecisionListener caseDecisionListener;

    private String caseId;

    @Before
    public void setup() {
        caseId = UUID.randomUUID().toString();
    }

    @Test
    public void shouldHandleReferencedDecisionSaved() {

        final JsonEnvelope event = envelope()
                .with(metadataWithRandomUUID("public.resulting.referenced-decisions-saved"))
                .withPayloadOf(caseId, "caseId")
                .withPayloadOf(LocalDate.now().toString(), "resultedOn")
                .withPayloadOf(UUID.randomUUID().toString(), "sjpSessionId")
                .build();

        caseDecisionListener.referencedDecisionsSaved(event);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(event).withName("sjp.command.complete-case"),
                payloadIsJson(withJsonPath("$.caseId", equalTo(caseId))))));
    }

    @Test
    public void shouldHandleCaseReferredToCourt() {

        final String hearingDate = LocalDate.now().plusWeeks(1).toString();

        final JsonEnvelope event = envelope()
                .with(metadataWithRandomUUID("public.resulting.case-referred-to-court"))
                .withPayloadOf(caseId, "caseId")
                .withPayloadOf(hearingDate, "hearingDate")
                .build();

        caseDecisionListener.caseReferredToCourt(event);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(event).withName("sjp.command.create-court-referral"),
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", equalTo(caseId)),
                        withJsonPath("$.hearingDate", equalTo(hearingDate))
                )))));
    }

}