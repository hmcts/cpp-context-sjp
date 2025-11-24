package uk.gov.moj.cpp.sjp.query.service;


import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.json.JsonObject;

import org.apache.commons.collections.MapUtils;


public class OffenceFineLevels {
    private final ReferenceDataService referenceDataService;
    private Map<Integer, BigDecimal> maxValuesByLevels;
    private final JsonEnvelope envelope;

    private static final String FINE_LEVEL = "fineLevel";
    private static final String MAX_VALUE = "maxValue";

    public OffenceFineLevels(final ReferenceDataService referenceDataService, final JsonEnvelope envelope) {
        this.referenceDataService = referenceDataService;
        this.envelope = envelope;
    }

    public Optional<BigDecimal> getOffenceMaxFineValue(final Integer fineLevel) {
        if (MapUtils.isEmpty(maxValuesByLevels)) {
            final List<JsonObject> offenceFineLevels = referenceDataService.getOffenceFineLevels(envelope);
            maxValuesByLevels = offenceFineLevels.stream()
                    .collect(toMap(fine -> fine.getInt(FINE_LEVEL), fine -> fine.getJsonNumber(MAX_VALUE).bigDecimalValue()));
        }
        return ofNullable(maxValuesByLevels.get(fineLevel));
    }
}
