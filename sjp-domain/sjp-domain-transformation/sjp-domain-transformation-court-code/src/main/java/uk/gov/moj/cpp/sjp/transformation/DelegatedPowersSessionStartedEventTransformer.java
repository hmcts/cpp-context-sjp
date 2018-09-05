package uk.gov.moj.cpp.sjp.transformation;

import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;
import uk.gov.moj.cpp.sjp.transformation.data.CourtHouseDataSource;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Transformation
public class DelegatedPowersSessionStartedEventTransformer extends BaseCourtCodeEnrichmentTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelegatedPowersSessionStartedEventTransformer.class);

    public DelegatedPowersSessionStartedEventTransformer() {
        super(CourtHouseDataSource.inMemory());
    }

    @VisibleForTesting
    DelegatedPowersSessionStartedEventTransformer(CourtHouseDataSource courtHouseDataSource) {
        super(courtHouseDataSource);
    }

    @Override
    String getEventName() {
        return "sjp.events.delegated-powers-session-started";
    }

    @Override
    String getEventSchemaFileName() {
        return "delegated-powers-session-started.json";
    }

    @Override
    Logger getLogger() {
        return LOGGER;
    }
}
