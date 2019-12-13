package uk.gov.moj.cpp.sjp.query.api.service;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class CourtExtractDataService {

    private static final String TEMPLATE_IDENTIFIER = "CourtExtract";

    @Inject
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;

    @Inject
    private SystemUserProvider systemUserProvider;

    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;

    public Optional<JsonObject> getCourtExtractData(final JsonEnvelope initialQueryEnvelope) {
        return ofNullable(requester.request(initialQueryEnvelope)).
                map(JsonEnvelope::payloadAsJsonObject);
    }

    public byte[] generatePdfDocument(final JsonObject courtExtractData) throws IOException {
        return documentGeneratorClientProducer.documentGeneratorClient().
                generatePdfDocument(courtExtractData, TEMPLATE_IDENTIFIER, getSystemUser());
    }

    private UUID getSystemUser() {
        return systemUserProvider.getContextSystemUserId()
                .orElseThrow(() -> new RuntimeException("systemUserProvider.getContextSystemUserId() not available"));
    }
}
