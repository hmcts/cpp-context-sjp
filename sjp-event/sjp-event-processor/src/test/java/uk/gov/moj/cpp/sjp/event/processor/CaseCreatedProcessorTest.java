package uk.gov.moj.cpp.sjp.event.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseCreatedProcessorTest {

    private static final String CASE_ID = UUID.randomUUID().toString();
    private static final String POSTING_DATE = LocalDate.now().toString();

    @InjectMocks
    private CaseCreatedProcessor caseCreatedProcessor;

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope event;

    @Mock
    private JsonObject jsonObject;

    @Mock
    private Enveloper enveloper;

    @Mock
    private JsonEnvelope messageToPublish;

    @Before
    public void before() {
        when(jsonObject.getString("caseId")).thenReturn(CASE_ID);
        when(event.payloadAsJsonObject()).thenReturn(jsonObject);
        when(enveloper.withMetadataFrom(eq(event), any())).thenReturn((t) -> {
            when(messageToPublish.payload()).thenReturn((JsonValue) t);
            return messageToPublish;
        });
    }

    @After
    public void after() {
        verify(sender).send(messageToPublish);
        final JsonObject requestPayload = (JsonObject) messageToPublish.payload();
        assertNotNull(requestPayload);
    }

    @Test
    public void publishSjpCaseCreatedPublicEvent() throws Exception {

        when(jsonObject.getString("postingDate")).thenReturn(POSTING_DATE);

        caseCreatedProcessor.publishSjpCaseCreatedPublicEvent(event);

        final JsonObject requestPayload = (JsonObject) messageToPublish.payload();
        assertEquals(requestPayload.getString("id"), CASE_ID);
        assertEquals(requestPayload.getString("postingDate"), POSTING_DATE);

    }

}