package uk.gov.moj.cpp.sjp.event.processor.utils.fake;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator.DocumentGenerationRequest;
import uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator.SystemDocGenerator;

import java.util.HashMap;
import java.util.Map;

public class FakeSystemDocGenerator extends SystemDocGenerator {

    private Map<JsonEnvelope, DocumentGenerationRequest> requestStorage = new HashMap<>();

    @Override
    public void generateDocument(final DocumentGenerationRequest request, final JsonEnvelope envelope) {
        this.requestStorage.put(envelope, request);
    }

    public DocumentGenerationRequest getDocumentGenerationRequest(final JsonEnvelope envelope) {
        return this.requestStorage.get(envelope);
    }
}
