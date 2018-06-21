package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataOf;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;
import uk.gov.moj.cpp.sjp.event.EnterpriseIdAssociated;

import java.util.UUID;

import javax.json.JsonObjectBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AssociateEnterpriseIdHandlerTest {

    private static final String CASE_ID_PROPERTY = "caseId";
    private static final String ENTERPRISE_ID_PROPERTY = "enterpriseId";

    @Mock
    private EventStream eventStream;

    @Mock
    private EventSource eventSource;

    @InjectMocks
    private AssociateEnterpriseIdHandler associateEnterpriseIdHandler;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            EnterpriseIdAssociated.class
    );

    @Test
    public void shouldProcessAssociateEnterpriseIdCommandAndGenerateAppendExpectedEvent() throws EventStreamException {
        final UUID caseId = randomUUID();
        final String enterpriseId = "2K2SLYFC743H";
        final JsonEnvelope command = createAssociateEnterpriseIdHandlerCommand(caseId, enterpriseId);

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);

        associateEnterpriseIdHandler.associateEnterpriseId(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.enterprise-id-associated"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                                        withJsonPath("$.enterpriseId", equalTo(enterpriseId))
                                )))
                                .thatMatchesSchema()
                )));
    }

    private JsonEnvelope createAssociateEnterpriseIdHandlerCommand(final UUID caseId, final String enterpriseId) {
        final JsonObjectBuilder payload = createObjectBuilder()
                .add(CASE_ID_PROPERTY, caseId.toString())
                .add(ENTERPRISE_ID_PROPERTY, enterpriseId);

        return JsonEnvelopeBuilder.envelopeFrom(
                metadataOf(randomUUID(), "sjp.command.associate-enterprise-id"),
                payload.build());
    }

}