package uk.gov.moj.sjp.it.producer;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_ACTIVE_MQ_TOPIC;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SELECTOR_CASE_ADJOURNED_TO_LATER_HEARING_IN_RESULTING;

import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;

import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;

public class CaseAdjournmentProducer {

    private final UUID caseId;
    private final UUID sessionId;
    private final LocalDate adjournedTo;

    public CaseAdjournmentProducer(final UUID caseId, final UUID sessionId, LocalDate adjournedTo) {
        this.caseId = caseId;
        this.sessionId = sessionId;
        this.adjournedTo = adjournedTo;
    }

   public void adjournCase() {
       final JsonObject payload = createObjectBuilder()
               .add("caseId", caseId.toString())
               .add("sjpSessionId", sessionId.toString())
               .add("adjournedTo", adjournedTo.toString())
               .build();

       try (final MessageProducerClient messageProducer = new MessageProducerClient()) {
           messageProducer.startProducer(PUBLIC_ACTIVE_MQ_TOPIC);
           messageProducer.sendMessage(PUBLIC_EVENT_SELECTOR_CASE_ADJOURNED_TO_LATER_HEARING_IN_RESULTING, payload);
       }
   }
}
