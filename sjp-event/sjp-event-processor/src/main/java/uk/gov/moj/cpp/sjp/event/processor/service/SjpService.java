package uk.gov.moj.cpp.sjp.event.processor.service;


import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.DEFENDANT_ID;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.SESSION_ID;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.query.DefendantsOnlinePlea;
import uk.gov.justice.json.schemas.domains.sjp.query.EmployerDetails;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

public class SjpService {

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    public CaseDetails getCaseDetails(final UUID caseId, final JsonEnvelope envelope) {
        final JsonObject payload = Json.createObjectBuilder().add(CASE_ID, caseId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(envelope, "sjp.query.case").apply(payload);
        final Envelope<CaseDetails> caseDetailsEnvelope = requester.request(request, CaseDetails.class);

        return caseDetailsEnvelope.payload();
    }

    public JsonObject getSessionDetails(final UUID sessionId, final JsonEnvelope envelope) {
        final JsonObject payload = Json.createObjectBuilder().add(SESSION_ID, sessionId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(envelope, "sjp.query.session").apply(payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);
        return response.payloadAsJsonObject();
    }

    public DefendantsOnlinePlea getDefendantPleaDetails(final UUID caseId, final UUID defendantId, final JsonEnvelope envelope) {
        final JsonObject payload = Json.createObjectBuilder()
                .add(CASE_ID, caseId.toString())
                .add(DEFENDANT_ID, defendantId.toString())
                .build();
        final JsonEnvelope request = enveloper.withMetadataFrom(envelope, "sjp.query.defendants-online-plea").apply(payload);
        final Envelope<DefendantsOnlinePlea> defendantsOnlinePleaEnvelope = requester.request(request, DefendantsOnlinePlea.class);

        return defendantsOnlinePleaEnvelope.payload();
    }

    public EmployerDetails getEmployerDetails(final UUID defendantId, final JsonEnvelope envelope) {
        final JsonObject payload = Json.createObjectBuilder().add(DEFENDANT_ID, defendantId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(envelope, "sjp.query.employer").apply(payload);
        final Envelope<EmployerDetails> defendantsOnlinePleaEnvelope = requester.request(request, EmployerDetails.class);

        return defendantsOnlinePleaEnvelope.payload();
    }
}
