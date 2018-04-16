package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.Json;

import org.junit.Before;
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

    private UUID caseId;

    @Before
    public void setup() {
        caseId = UUID.randomUUID();
    }

    @Test
    public void shouldUpdateCaseState() {

        final JsonEnvelope event = createEnvelope("public.resulting.referenced-decisions-saved",
                Json.createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("resultedOn", ZonedDateTime.now().toString())
                        .add("sjpSessionId", UUID.randomUUID().toString())
                        .build());
        caseDecisionListener.referencedDecisionsSaved(event);

        verify(caseStateService).caseCompleted(caseId, event.metadata());
    }


    @Test
    public void shouldHandleCaseReferredToCourt() {

        final String hearingDate = LocalDate.now().plusWeeks(1).toString();

        final JsonEnvelope event = createEnvelope("public.resulting.case-referred-to-court",
                Json.createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("hearingDate", hearingDate)
                        .build());

        caseDecisionListener.caseReferredToCourt(event);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(event).withName("sjp.command.create-court-referral"),
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.hearingDate", equalTo(hearingDate))
                )))));
    }

}