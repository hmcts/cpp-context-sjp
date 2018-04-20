package uk.gov.moj.sjp.it.helper;

import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static uk.gov.moj.sjp.it.EventSelector.PRIVATE_ACTIVE_MQ_TOPIC;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_ACTIVE_MQ_TOPIC;

import uk.gov.justice.services.messaging.DefaultJsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.moj.sjp.it.util.HttpClientUtil;

import java.util.UUID;

import javax.json.Json;

public class AssignmentHelper implements AutoCloseable {

    private MessageConsumerClient caseNotAssignedPublicEventConsumer;
    private MessageConsumerClient caseAssignedPublicEventConsumer;
    private MessageConsumerClient caseAssignedPrivateEventConsumer;
    private MessageConsumerClient caseAssignmentRejectedPublicEventConsumer;

    public AssignmentHelper() {
        caseAssignedPrivateEventConsumer = new MessageConsumerClient();
        caseAssignedPublicEventConsumer = new MessageConsumerClient();
        caseNotAssignedPublicEventConsumer = new MessageConsumerClient();
        caseAssignmentRejectedPublicEventConsumer = new MessageConsumerClient();

        caseAssignedPrivateEventConsumer.startConsumer("sjp.events.case-assigned", PRIVATE_ACTIVE_MQ_TOPIC);
        caseAssignedPublicEventConsumer.startConsumer("public.sjp.case-assigned", PUBLIC_ACTIVE_MQ_TOPIC);
        caseNotAssignedPublicEventConsumer.startConsumer("public.sjp.case-not-assigned", PUBLIC_ACTIVE_MQ_TOPIC);
        caseAssignmentRejectedPublicEventConsumer.startConsumer("public.sjp.case-assignment-rejected", PUBLIC_ACTIVE_MQ_TOPIC);
    }

    public static void requestCaseAssignment(final UUID sessionId, final UUID userId) {
        final String contentType = "application/vnd.sjp.assign-case+json";
        final String url = String.format("/sessions/%s", sessionId);
        HttpClientUtil.makePostCall(userId, url, contentType, Json.createObjectBuilder().build().toString(), ACCEPTED);
    }

    public JsonEnvelope getCaseNotAssignedEvent() {
        final String message = caseNotAssignedPublicEventConsumer.retrieveMessage().get();
        return new DefaultJsonObjectEnvelopeConverter().asEnvelope(message);
    }

    public JsonEnvelope getCaseAssignedPrivateEvent() {
        final String message = caseAssignedPrivateEventConsumer.retrieveMessage().get();
        return new DefaultJsonObjectEnvelopeConverter().asEnvelope(message);
    }

    public JsonEnvelope getCaseAssignedPublicEvent() {
        final String message = caseAssignedPublicEventConsumer.retrieveMessage().get();
        return new DefaultJsonObjectEnvelopeConverter().asEnvelope(message);
    }

    public JsonEnvelope getCaseAssignmentRejectedPublicEvent() {
        final String message = caseAssignmentRejectedPublicEventConsumer.retrieveMessage().get();
        return new DefaultJsonObjectEnvelopeConverter().asEnvelope(message);
    }

    @Override
    public void close() {
        caseAssignedPrivateEventConsumer.close();
        caseAssignedPublicEventConsumer.close();
        caseNotAssignedPublicEventConsumer.close();
        caseAssignmentRejectedPublicEventConsumer.close();
    }
}
