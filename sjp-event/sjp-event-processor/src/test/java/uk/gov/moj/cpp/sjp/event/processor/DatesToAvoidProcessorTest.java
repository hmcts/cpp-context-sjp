package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
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

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DatesToAvoidProcessorTest {

    @InjectMocks
    private DatesToAvoidProcessor processor;

    @Mock
    private Sender sender;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private CaseStateService caseStateService;

    @Test
    public void raisesDatesToAvoidAddedPublicEvent() {
        final UUID caseId = UUID.randomUUID();
        final String datesToAvoid = "Mon - Wed";
        final JsonEnvelope envelope = EnvelopeFactory.createEnvelope("sjp.events.dates-to-avoid-added",
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("datesToAvoid", datesToAvoid)
                        .build()
        );

        processor.publishDatesToAvoidAdded(envelope);

        verify(caseStateService).datesToAvoidAdded(caseId, datesToAvoid, envelope.metadata());
    }

    @Test
    public void raisesDatesToAvoidUpdatedPublicEvent() {
        final UUID caseId = UUID.randomUUID();
        final String datesToAvoid = "Tue - Wed";
        final JsonEnvelope envelope = EnvelopeFactory.createEnvelope("sjp.events.dates-to-avoid-updated",
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("datesToAvoid", datesToAvoid)
                        .build()
        );

        processor.publishDatesToAvoidUpdated(envelope);

        verify(sender).send(argThat(jsonEnvelope(
                metadata()
                        .envelopedWith(envelope.metadata())
                        .withName("public.sjp.dates-to-avoid-updated"),
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.datesToAvoid", equalTo(datesToAvoid))
                )))));
    }

}