package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import javax.json.Json;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseAdjournmentProcessorTest {

    private static final UUID CASE_ID = randomUUID();
    private static final LocalDate ADJOURNED_TO = LocalDate.now();
    @Mock
    private CaseStateService caseStateService;

    @Mock
    private Sender sender;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    private CaseAdjournmentProcessor caseAdjournmentProcessor;

    @Test
    public void shouldRecordCaseAdjournedToLaterSjpHearing() {
        final JsonEnvelope publicResultingEvent = EnvelopeFactory
                .createEnvelope("public.resulting.case-adjourned-to-later-sjp-hearing",
                        Json.createObjectBuilder()
                                .add("caseId", CASE_ID.toString())
                                .add("adjournedTo", ADJOURNED_TO.toString())
                                .build());

        caseAdjournmentProcessor.caseAdjournedToLaterSjpHearing(publicResultingEvent);

        verify(sender).send(argThat(
                jsonEnvelope(metadata().withName("sjp.command.record-case-adjourned-to-later-sjp-hearing"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", is(CASE_ID.toString())),
                                withJsonPath("$.adjournedTo", is(ADJOURNED_TO.toString())))))
        ));
    }

    @Test
    public void shouldInvokeActivityFlowWhenCaseAdjournmentRecorder() {
        final JsonEnvelope caseAdjournmentRecordedEvent = EnvelopeFactory
                .createEnvelope("sjp.events.case-adjourned-to-later-sjp-hearing-recorded",
                        Json.createObjectBuilder()
                                .add("caseId", CASE_ID.toString())
                                .add("adjournedTo", ADJOURNED_TO.toString())
                                .build());

        caseAdjournmentProcessor.caseAdjournedForLaterSjpHearingRecorded(caseAdjournmentRecordedEvent);

        verify(caseStateService).caseAdjournedForLaterHearing(CASE_ID, ADJOURNED_TO.atStartOfDay(), caseAdjournmentRecordedEvent.metadata());
    }
}
