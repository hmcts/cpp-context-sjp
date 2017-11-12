package uk.gov.moj.cpp.sjp.event.processor.utils;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;

import org.activiti.engine.delegate.DelegateExecution;

public class ProcessIdHelper {

    private final static String SJP_PREFIX = "sjp:";

    public static String encodeProcessId(final DelegateExecution delegateExecution) {
        return SJP_PREFIX + delegateExecution.getId();
    }

    public static Optional<String> decodeProcessId(final JsonEnvelope envelope) {
        return envelope.metadata().clientCorrelationId()
                .filter(id -> id.startsWith(SJP_PREFIX))
                .flatMap(y -> Optional.of(y.substring(SJP_PREFIX.length())));
    }

}
