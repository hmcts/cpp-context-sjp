package uk.gov.moj.cpp.sjp.event.processor.service;


import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.DEFENDANT_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.SESSION_ID;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.query.DefendantsOnlinePlea;
import uk.gov.justice.json.schemas.domains.sjp.query.EmployerDetails;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

public class SjpService {

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    public CaseDetails getCaseDetails(final UUID caseId, final JsonEnvelope envelope) {
        final JsonObject payload = Json.createObjectBuilder().add(CASE_ID, caseId.toString()).build();
        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata()).withName( "sjp.query.case"), payload);
        final Envelope<CaseDetails> caseDetailsEnvelope = requester.request(request, CaseDetails.class);

        return caseDetailsEnvelope.payload();
    }

    public JsonObject getSessionDetails(final UUID sessionId, final JsonEnvelope envelope) {
        final JsonObject payload = Json.createObjectBuilder().add(SESSION_ID, sessionId.toString()).build();
        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata()).withName( "sjp.query.session"), payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);
        return response.payloadAsJsonObject();
    }

    public DefendantsOnlinePlea getDefendantPleaDetails(final UUID caseId, final UUID defendantId, final JsonEnvelope envelope) {
        final JsonObject payload = Json.createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .add(DEFENDANT_ID, defendantId.toString())
                .build();
        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata()).withName("sjp.query.defendants-online-plea"), payload);
        final Envelope<DefendantsOnlinePlea> defendantsOnlinePleaEnvelope = requester.request(request, DefendantsOnlinePlea.class);

        return defendantsOnlinePleaEnvelope.payload();
    }

    public EmployerDetails getEmployerDetails(final UUID defendantId, final JsonEnvelope envelope) {
        final JsonObject payload = Json.createObjectBuilder().add(DEFENDANT_ID, defendantId.toString()).build();
        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata()).withName("sjp.query.employer"), payload);
        final Envelope<EmployerDetails> defendantsOnlinePleaEnvelope = requester.request(request, EmployerDetails.class);

        return defendantsOnlinePleaEnvelope.payload();
    }

    public List<JsonObject> getPendingCases(final JsonEnvelope envelope, final ExportType exportType) {
        return requester.request(
                envelopeFrom(
                        metadataFrom(envelope.metadata())
                                .withName("sjp.query.pending-cases").build(),
                        createObjectBuilder()
                                .add("export", exportType.name().toLowerCase())
                                .build()
                ))
                .payloadAsJsonObject()
                .getJsonArray("pendingCases")
                .getValuesAs(JsonObject.class);
    }
}
