package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator.SystemDocGeneratorResponseStrategy;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class SysDocGeneratorProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SysDocGeneratorProcessor.class);

    private static final String DOCUMENT_AVAILABLE_EVENT_NAME = "public.systemdocgenerator.events.document-available";
    private static final String DOCUMENT_GENERATION_FAILED_EVENT_NAME = "public.systemdocgenerator.events.generation-failed";
    private static final String SJP_SOURCE = "sjp";

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Instance<SystemDocGeneratorResponseStrategy> responseHandlerStrategies;

    @Handles(DOCUMENT_AVAILABLE_EVENT_NAME)
    public void handleDocumentAvailableEvent(final JsonEnvelope envelope) {
        final SystemDocGeneratorResponseStrategy processor = resolveHandler(envelope);
        processor.process(envelope);
    }

    @Handles(DOCUMENT_GENERATION_FAILED_EVENT_NAME)
    public void handleDocumentGenerationFailedEvent(final JsonEnvelope envelope) {
        final SystemDocGeneratorResponseStrategy processor = resolveHandler(envelope);
        processor.process(envelope);
    }

    private SystemDocGeneratorResponseStrategy resolveHandler(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        if (isSjpSource(payload)) {

            for (SystemDocGeneratorResponseStrategy responseStrategy : responseHandlerStrategies) {
                if (responseStrategy.canProcess(envelope)) {
                    return responseStrategy;
                }
            }

            final String templateIdentifier = payload.getString("templateIdentifier");
            LOGGER.info("unrecognized template {}", templateIdentifier);

        } else {
            LOGGER.debug("document generated for another context, ignoring");
        }
        return doNothingHandler();
    }

    private boolean isSjpSource(final JsonObject payload) {
        final String source = payload.getString("originatingSource", "");
        return SJP_SOURCE.equalsIgnoreCase(source);
    }


    private SystemDocGeneratorResponseStrategy doNothingHandler() {
        return new SystemDocGeneratorResponseStrategy() {
            @SuppressWarnings({"squid:S1172"}) // suppress sonar empty method issue.
            @Override
            public void process(final JsonEnvelope envelope) {
                // ignore
            }

            @Override
            public boolean canProcess(final JsonEnvelope envelope) {
                return false;
            }
        };
    }
}