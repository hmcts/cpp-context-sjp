package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;
import uk.gov.moj.cpp.sjp.event.CaseApplicationRecorded;
import uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification.EnforcementNotificationService;

import java.util.UUID;
import java.util.function.Consumer;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseApplicationRecordedProcessorTest {
    static final String CASE_APPLICATION_RECORDED_PUBLIC_EVENT = "public.sjp.case-application-recorded";
    private static final UUID APPLICATION_ID = randomUUID();

    @InjectMocks
    private CaseApplicationRecordedProcessor caseApplicationRecordedProcessor;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> captor;

    @Spy
    private Enveloper envelopers = createEnveloper();

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private EnforcementNotificationService enforcementNotificationService;


    @Test
    public void shouldHandleCaseStatDecRecordedEventMessage() {
        verifyEventMessage(CASE_APPLICATION_RECORDED_PUBLIC_EVENT, caseApplicationRecordedProcessor::handleCaseApplicationRecorded);
    }

    private void verifyEventMessage(final String eventName, Consumer<JsonEnvelope> consumer) {
        verifyCaseApplicationRecordedEventMessage(eventName, consumer);
    }

    private void verifyCaseApplicationRecordedEventMessage(final String eventName, final Consumer<JsonEnvelope> consumer) {
        final UUID caseId = UUID.randomUUID();
        final CaseApplicationRecorded caseApplicationRecorded = CaseApplicationRecorded.caseApplicationRecorded()
                .withCourtApplication(CourtApplication.courtApplication().withId(APPLICATION_ID).withApplicationReference("ABC123").build())
                .withCaseId(caseId)
                .build();
        final JsonObject jsonObject = mock(JsonObject.class);

        final JsonEnvelope privateEvent = EnvelopeFactory.createEnvelope(eventName, jsonObject);
        setField(jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());

        when(jsonObjectToObjectConverter.convert(jsonObject, CaseApplicationRecorded.class)).thenReturn(caseApplicationRecorded);
        consumer.accept(privateEvent);

        verify(sender).send(captor.capture());
        final JsonEnvelope publicEvent = captor.getValue();
        assertThat(publicEvent, jsonEnvelope(
                metadata().withName(eventName),
                payloadIsJson(allOf(
                        withJsonPath("$.applicationId", equalTo(APPLICATION_ID.toString())),
                        withJsonPath("$.applicationReference", equalTo("ABC123"))))
        ));
        verify(enforcementNotificationService).checkIfEnforcementToBeNotified(caseId,privateEvent);
    }
}