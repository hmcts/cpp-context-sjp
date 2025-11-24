package uk.gov.moj.sjp.it.util;

import static java.util.stream.Collectors.groupingBy;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EventUtil {
    public static Map<String, List<JsonEnvelope>> eventsByName(final JsonEnvelope... events) {
        return Arrays.stream(events).collect(groupingBy(e -> e.metadata().name()));
    }
}
