package uk.gov.moj.cpp.sjp.event.processor;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.CourtDocumentTransformer;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.CourtDocumentsDataSourcingService;

import java.time.LocalDate;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.slf4j.Logger;

@ServiceComponent(EVENT_PROCESSOR)
public class SjpProsecutionCaseCreatedProcessor {

    private static final Logger LOGGER = getLogger(SjpProsecutionCaseCreatedProcessor.class);
    public static final String PROSECUTION_CASE = "prosecutionCase";

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private CourtDocumentsDataSourcingService courtDocumentsDataSourcingService;

    @Inject
    private SjpService sjpService;

    @Inject
    private CourtDocumentTransformer courtDocumentTransformer;

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @SuppressWarnings("squid:CallToDeprecatedMethod")
    @Handles("public.progression.sjp-prosecution-case-created")
    public void handleApplicationDecisionSaved(final JsonEnvelope envelope) {

        final JsonEnvelope emptyEnvelope = envelopeFrom(metadataFrom(envelope.metadata()), NULL);

        final JsonObject prosecutionCase = envelope.payloadAsJsonObject().getJsonObject(PROSECUTION_CASE);
        final CaseDetails caseDetails = sjpService.getCaseDetails(UUID.fromString(prosecutionCase.getString("id")), emptyEnvelope);

        final JsonArray documentsArray = courtDocumentsDataSourcingService.createCourtDocumentViews(
                LocalDate.now(),
                caseDetails,
                emptyEnvelope)
                .stream()
                .map(e -> courtDocumentTransformer.transform(e, envelope))
                .map(e -> objectToJsonObjectConverter.convert(e))
                .reduce(createArrayBuilder(), JsonArrayBuilder::add, JsonArrayBuilder::add)
                .build();

        final JsonObject payload = createObjectBuilder().add("courtDocuments", documentsArray).build();
        LOGGER.info("CreateCourtDocumentsPayload {}",payload);

        sender.send(enveloper
                .withMetadataFrom(envelope,
                        "progression.create-court-documents")
                .apply(payload));
    }

}

