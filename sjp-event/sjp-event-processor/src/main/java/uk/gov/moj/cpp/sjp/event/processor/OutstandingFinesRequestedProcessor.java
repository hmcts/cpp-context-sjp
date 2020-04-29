package uk.gov.moj.cpp.sjp.event.processor;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.DefendantOutstandingFineRequestsQueryResult;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class OutstandingFinesRequestedProcessor {

    public static final int DEFAULT_OUTSTANDING_FINES_BATCH_SIZE = 100;
    private static final Logger LOGGER = LoggerFactory.getLogger(OutstandingFinesRequestedProcessor.class);
    // can be configurable
    private int outstandingFinesBatchSize = DEFAULT_OUTSTANDING_FINES_BATCH_SIZE;

    @Inject
    private Sender sender;

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;


    @Handles("sjp.events.outstanding-fines-requested")
    public void outstandingFinesRequested(final JsonEnvelope event) {

        LOGGER.info("sjp.events.outstanding-fines-requested received ");

        final Envelope<JsonObject> envelope = envelop(createObjectBuilder().build())
                .withName("sjp.query.outstanding-fine-requests").withMetadataFrom(event);

        final JsonEnvelope hearingAccountQueryInformation = requester.request(envelopeFrom(envelope.metadata(), envelope.payload()));

        final DefendantOutstandingFineRequestsQueryResult defendantInfoQueryResult = this.jsonObjectToObjectConverter.convert(hearingAccountQueryInformation.payloadAsJsonObject(),
                DefendantOutstandingFineRequestsQueryResult.class);

        if (defendantInfoQueryResult == null || defendantInfoQueryResult.getDefendantDetails() == null || defendantInfoQueryResult.getDefendantDetails().isEmpty()) {
            LOGGER.info("sjp.query.outstanding-fine-requests Query Result is empty");

        } else {
            LOGGER.info("sjp.query.outstanding-fine-requests Query Results Size {}",
                    defendantInfoQueryResult.getDefendantDetails() != null ? defendantInfoQueryResult.getDefendantDetails().size() : 0);

            Lists.partition(defendantInfoQueryResult.getDefendantDetails(), outstandingFinesBatchSize).forEach(
                    defendantRequestProfiles -> {
                        final JsonArrayBuilder fineRequests = createArrayBuilder();
                        defendantRequestProfiles.forEach(
                                defendantOutstandingFineRequest -> fineRequests.add(objectToJsonObjectConverter.convert(defendantOutstandingFineRequest)));
                        this.sender.send(envelopeFrom(
                                metadataFrom(event.metadata()).withName("stagingenforcement.request-outstanding-fine"),
                                createObjectBuilder().add("fineRequests", fineRequests)
                        ));
                    }
            );

        }

    }


}
