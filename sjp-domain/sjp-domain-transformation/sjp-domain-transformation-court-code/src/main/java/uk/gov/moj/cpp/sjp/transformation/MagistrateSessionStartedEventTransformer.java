package uk.gov.moj.cpp.sjp.transformation;

import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;
import uk.gov.moj.cpp.sjp.transformation.data.CourtHouseDataSource;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transforms sjp.events.magistrate-session-started events, enriching them with court house code.
 */
@Transformation
public class MagistrateSessionStartedEventTransformer extends BaseCourtCodeEnrichmentTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MagistrateSessionStartedEventTransformer.class);

    public MagistrateSessionStartedEventTransformer() {
        super(CourtHouseDataSource.inMemory());
    }

    @VisibleForTesting
    MagistrateSessionStartedEventTransformer(CourtHouseDataSource courtHouseDataSource) {
        super(courtHouseDataSource);
    }

    @Override
    String getEventName() {
        return "sjp.events.magistrate-session-started";
    }

    @Override
    String getEventSchemaFileName() {
        return "magistrate-session-started.json";
    }

    @Override
    Logger getLogger() {
        return LOGGER;
    }
}
